package com.techressive.hex.audio

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionResult
import com.techressive.hex.MainActivity
import com.techressive.hex.metadata.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

enum class AudioOutputMethod(val displayName: String, val description: String) {
    AAUDIO("AAudio (Low-Latency)", "High-performance native Android low-latency audio stream"),
    AUDIOTRACK("Java AudioTrack", "Standard Android Java audio pipeline with broad compatibility"),
    OPENSL_ES("OpenSL ES", "Legacy native C/C++ audio sink for low jitter playback"),
    HI_RES_DIRECT("Hi-Res Direct (32-bit Float)", "Bit-perfect uncompressed float PCM directly to audio hardware")
}

enum class AudioSampleRate(val displayName: String, val valueHz: Int) {
    AUTO("Follow Source", 0),
    RATE_44100("44.1 kHz (CD Audio)", 44100),
    RATE_48000("48.0 kHz (Standard)", 48000),
    RATE_96000("96.0 kHz (Hi-Res Studio)", 96000),
    RATE_192000("192.0 kHz (Audiophile Master)", 192000),
    RATE_384000("384.0 kHz (Ultra Master)", 384000)
}

enum class AudioSampleFormat(val displayName: String, val bits: Int) {
    AUTO("Follow Source", 0),
    PCM_16BIT("16-bit Integer", 16),
    PCM_24BIT("24-bit Integer", 24),
    FLOAT_32BIT("32-bit IEEE Float", 32)
}

class HexAudioPlayer(
    private val context: Context,
    private val spectrumAnalyzer: SpectrumAnalyzer,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "HexAudioPlayer"
        private const val PREFS_NAME = "HEX_AUDIO_CONFIG"
        private const val KEY_OUTPUT_METHOD = "output_method"
        private const val KEY_SAMPLE_RATE = "sample_rate"
        private const val KEY_SAMPLE_FORMAT = "sample_format"
        var activeMediaSession: MediaSession? = null
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _outputMethod = MutableStateFlow(
        try {
            AudioOutputMethod.valueOf(prefs.getString(KEY_OUTPUT_METHOD, AudioOutputMethod.AAUDIO.name) ?: AudioOutputMethod.AAUDIO.name)
        } catch (_: Exception) { AudioOutputMethod.AAUDIO }
    )
    val outputMethod: StateFlow<AudioOutputMethod> = _outputMethod.asStateFlow()

    private val _sampleRate = MutableStateFlow(
        try {
            AudioSampleRate.valueOf(prefs.getString(KEY_SAMPLE_RATE, AudioSampleRate.AUTO.name) ?: AudioSampleRate.AUTO.name)
        } catch (_: Exception) { AudioSampleRate.AUTO }
    )
    val sampleRate: StateFlow<AudioSampleRate> = _sampleRate.asStateFlow()

    private val _sampleFormat = MutableStateFlow(
        try {
            AudioSampleFormat.valueOf(prefs.getString(KEY_SAMPLE_FORMAT, AudioSampleFormat.FLOAT_32BIT.name) ?: AudioSampleFormat.FLOAT_32BIT.name)
        } catch (_: Exception) { AudioSampleFormat.FLOAT_32BIT }
    )
    val sampleFormat: StateFlow<AudioSampleFormat> = _sampleFormat.asStateFlow()

    private var exoPlayer: ExoPlayer? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0.0)
    val currentPosition: StateFlow<Double> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0.0)
    val duration: StateFlow<Double> = _duration.asStateFlow()

    private val _volume = MutableStateFlow(1.0f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _isShuffled = MutableStateFlow(false)
    val isShuffled: StateFlow<Boolean> = _isShuffled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private var currentTrack: Track? = null
    private var progressJob: Job? = null
    private var eqEngine: ParametricEQEngine? = null

    init {
        initPlayer()
    }

    fun attachEqEngine(engine: ParametricEQEngine) {
        this.eqEngine = engine
        bindEqSession()
    }

    private fun bindEqSession() {
        val player = exoPlayer ?: return
        val sessionId = player.audioSessionId
        if (sessionId > 0 && sessionId != C.AUDIO_SESSION_ID_UNSET) {
            eqEngine?.attachAudioSession(sessionId)
        } else {
            eqEngine?.attachAudioSession(0)
        }
    }

    fun setAudioOutputConfig(
        method: AudioOutputMethod,
        rate: AudioSampleRate,
        format: AudioSampleFormat
    ) {
        _outputMethod.value = method
        _sampleRate.value = rate
        _sampleFormat.value = format

        prefs.edit()
            .putString(KEY_OUTPUT_METHOD, method.name)
            .putString(KEY_SAMPLE_RATE, rate.name)
            .putString(KEY_SAMPLE_FORMAT, format.name)
            .apply()

        // Reconfigure player preserving position
        val wasPlaying = _isPlaying.value
        val pos = _currentPosition.value
        val track = currentTrack

        reinitPlayer()

        if (track != null) {
            playTrack(track)
            if (pos > 0.0) {
                seekTo(pos)
            }
            if (!wasPlaying) {
                pause()
            }
        }
    }

    private fun reinitPlayer() {
        try {
            exoPlayer?.stop()
            exoPlayer?.release()
        } catch (_: Exception) {}
        initPlayer()
    }

    val player: ExoPlayer? get() = exoPlayer
    private var mediaSession: MediaSession? = null
    private var hasAttemptedStandardFallback = false

    var onSkipNextListener: (() -> Unit)? = null
    var onSkipPreviousListener: (() -> Unit)? = null

    fun getMediaSession(): MediaSession? = mediaSession

    private fun bindMediaSession() {
        val p = exoPlayer ?: return
        try {
            mediaSession?.release()

            val sessionIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                sessionIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            mediaSession = MediaSession.Builder(context, p)
                .setId("HexAudioPlayerSession")
                .setSessionActivity(pendingIntent)
                .setCallback(object : MediaSession.Callback {
                    override fun onPlayerCommandRequest(
                        session: MediaSession,
                        controller: MediaSession.ControllerInfo,
                        playerCommand: Int
                    ): Int {
                        if (playerCommand == Player.COMMAND_SEEK_TO_NEXT ||
                            playerCommand == Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM) {
                            scope.launch(Dispatchers.Main) { skipNext() }
                            return SessionResult.RESULT_SUCCESS
                        }
                        if (playerCommand == Player.COMMAND_SEEK_TO_PREVIOUS ||
                            playerCommand == Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM) {
                            scope.launch(Dispatchers.Main) { skipPrevious() }
                            return SessionResult.RESULT_SUCCESS
                        }
                        return super.onPlayerCommandRequest(session, controller, playerCommand)
                    }
                })
                .build()
            activeMediaSession = mediaSession

            HexPlaybackService.instance?.let { service ->
                mediaSession?.let { s ->
                    try {
                        if (!service.sessions.contains(s)) {
                            service.addSession(s)
                        }
                    } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build MediaSession", e)
        }
    }

    private fun createPlayerListener(): Player.Listener {
        return object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
                if (playing) {
                    spectrumAnalyzer.startSimulation()
                    startProgressTracking()
                } else {
                    spectrumAnalyzer.stopSimulation()
                    stopProgressTracking()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    bindEqSession()
                    val durMs = exoPlayer?.duration ?: 0L
                    if (durMs > 0) {
                        _duration.value = durMs / 1000.0
                    }
                } else if (playbackState == Player.STATE_ENDED) {
                    val wasPlaying = _isPlaying.value
                    _isPlaying.value = false
                    spectrumAnalyzer.stopSimulation()
                    if (wasPlaying) {
                        skipNext()
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "ExoPlayer error occurred: ${error.errorCodeName} (${error.errorCode})", error)
                if ((error.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED ||
                     error.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED ||
                     error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED) &&
                    !hasAttemptedStandardFallback) {
                    hasAttemptedStandardFallback = true
                    Log.w(TAG, "Audio hardware sink rejected float format. Gracefully falling back to standard 16-bit PCM AudioSink.")
                    fallbackToStandardPlayer()
                    return
                }
                _isPlaying.value = false
                _lastError.value = error.localizedMessage ?: "Playback error: ${error.errorCodeName}"
                spectrumAnalyzer.stopSimulation()
            }
        }
    }

    private fun fallbackToStandardPlayer() {
        val wasPlaying = _isPlaying.value
        val pos = _currentPosition.value
        val track = currentTrack
        try {
            exoPlayer?.stop()
            exoPlayer?.release()
        } catch (_: Exception) {}

        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()

            exoPlayer = ExoPlayer.Builder(context)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(C.WAKE_MODE_LOCAL)
                .build().apply {
                    volume = this@HexAudioPlayer._volume.value
                    addListener(createPlayerListener())
                }
            bindEqSession()
            bindMediaSession()

            if (track != null) {
                val mediaItem = buildMediaItem(track)
                exoPlayer?.setMediaItem(mediaItem)
                exoPlayer?.prepare()
                if (pos > 0.0) {
                    seekTo(pos)
                }
                if (wasPlaying) {
                    play()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Standard player fallback failed", e)
            _isPlaying.value = false
            _lastError.value = e.localizedMessage
        }
    }

    private fun initPlayer() {
        try {
            val method = _outputMethod.value
            val format = _sampleFormat.value

            val enableFloat = format == AudioSampleFormat.FLOAT_32BIT ||
                    method == AudioOutputMethod.HI_RES_DIRECT ||
                    method == AudioOutputMethod.AAUDIO

            val renderersFactory = object : DefaultRenderersFactory(context) {
                override fun buildAudioSink(
                    context: Context,
                    enableFloatOutput: Boolean,
                    enableAudioTrackPlaybackParams: Boolean
                ): AudioSink? {
                    return DefaultAudioSink.Builder(context)
                        .setEnableFloatOutput(enableFloat)
                        .build()
                }
            }.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()

            exoPlayer = ExoPlayer.Builder(context, renderersFactory)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(C.WAKE_MODE_LOCAL)
                .build().apply {
                    volume = this@HexAudioPlayer._volume.value
                    addListener(createPlayerListener())
                }
            bindEqSession()
            bindMediaSession()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize custom audio sink, falling back to standard ExoPlayer", e)
            fallbackToStandardPlayer()
        }
    }

    fun playTrack(track: Track) {
        currentTrack = track
        _lastError.value = null
        hasAttemptedStandardFallback = false
        val player = exoPlayer ?: return
        try {
            val mediaItem = buildMediaItem(track)
            player.stop()
            player.clearMediaItems()
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()

            _duration.value = track.duration
            _currentPosition.value = 0.0
            _isPlaying.value = true
            startProgressTracking()
            startPlaybackService()
        } catch (e: Exception) {
            _lastError.value = e.localizedMessage
            Log.e(TAG, "Error playing track: ${track.title} (URI: ${track.uri})", e)
        }
    }

    private fun startPlaybackService() {
        try {
            val intent = Intent(context, HexPlaybackService::class.java)
            context.startService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start HexPlaybackService", e)
        }
    }

    private fun buildMediaItem(track: Track): MediaItem {
        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setIsPlayable(true)

        if (track.artworkUri != null) {
            metadataBuilder.setArtworkUri(track.artworkUri)
            // If local cached art file exists, read its bytes directly (instant, safe thumbnail)
            if (track.artworkUri?.scheme == "file") {
                track.artworkUri?.path?.let { p ->
                    val f = File(p)
                    if (f.exists() && f.length() in 1..400_000) {
                        try {
                            metadataBuilder.setArtworkData(f.readBytes(), MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                        } catch (_: Exception) {}
                    }
                }
            }
        }

        val mediaMetadata = metadataBuilder.build()

        val uriToPlay = if (track.uri != Uri.EMPTY) {
            track.uri
        } else {
            val p = track.path
            if (p.startsWith("content://") || p.startsWith("file://") || p.startsWith("http://") || p.startsWith("https://")) {
                Uri.parse(p)
            } else {
                Uri.fromFile(File(p))
            }
        }

        return MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(uriToPlay)
            .setMediaMetadata(mediaMetadata)
            .build()
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun play() {
        exoPlayer?.play()
        _isPlaying.value = true
        startProgressTracking()
        startPlaybackService()
    }

    fun pause() {
        exoPlayer?.pause()
        _isPlaying.value = false
        stopProgressTracking()
    }

    fun skipNext() {
        val listener = onSkipNextListener
        if (listener != null) {
            scope.launch(Dispatchers.Main) { listener.invoke() }
        } else {
            exoPlayer?.seekToNextMediaItem()
        }
    }

    fun skipPrevious() {
        val listener = onSkipPreviousListener
        if (listener != null) {
            scope.launch(Dispatchers.Main) { listener.invoke() }
        } else {
            exoPlayer?.seekToPreviousMediaItem()
        }
    }

    fun seekTo(seconds: Double) {
        val player = exoPlayer ?: return
        val posMs = (seconds * 1000.0).toLong().coerceAtLeast(0L)
        player.seekTo(posMs)
        _currentPosition.value = seconds
        if (_isPlaying.value) {
            startProgressTracking()
        }
    }

    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0.0f, 1.0f)
        _volume.value = clamped
        exoPlayer?.volume = clamped
    }

    fun toggleShuffle() {
        val next = !_isShuffled.value
        _isShuffled.value = next
        exoPlayer?.shuffleModeEnabled = next
    }

    fun toggleRepeat() {
        val nextMode = when (_repeatMode.value) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        _repeatMode.value = nextMode
        exoPlayer?.repeatMode = nextMode
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                exoPlayer?.let { player ->
                    val pos = player.currentPosition / 1000.0
                    _currentPosition.value = pos
                    val dur = player.duration / 1000.0
                    if (dur > 0) _duration.value = dur
                }
                delay(50)
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stopProgressTracking()
        spectrumAnalyzer.stopSimulation()
        try {
            mediaSession?.let { session ->
                HexPlaybackService.instance?.removeSession(session)
            }
            mediaSession?.release()
            mediaSession = null
            activeMediaSession = null
        } catch (_: Exception) {}
        exoPlayer?.release()
        exoPlayer = null
    }

    val activeEngineBadge: String
        get() = "${_outputMethod.value.displayName} • ${_sampleFormat.value.displayName} / ${_sampleRate.value.displayName}"
}
