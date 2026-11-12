package com.techressive.hex.state

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.documentfile.provider.DocumentFile
import com.techressive.hex.audio.HexAudioPlayer
import com.techressive.hex.audio.ParametricEQEngine
import com.techressive.hex.audio.SpectrumAnalyzer
import com.techressive.hex.metadata.ArtistBioService
import com.techressive.hex.metadata.ArtistDetails
import com.techressive.hex.metadata.ArtworkFetchService
import com.techressive.hex.metadata.AudioMetadataReader
import com.techressive.hex.metadata.LyricLine
import com.techressive.hex.metadata.LyricsService
import com.techressive.hex.metadata.Track
import com.techressive.hex.ui.theme.HexColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

sealed class Screen {
    object Home : Screen()
    object Library : Screen()
    object NowPlaying : Screen()
    object FullScreenLyrics : Screen()
    data class AlbumDetail(val albumName: String, val artist: String) : Screen()
    data class ArtistBio(val artistName: String) : Screen()
    object Settings : Screen()
}

enum class TrackSortOrder(val displayName: String) {
    TITLE_ASC("Title (A-Z)"),
    TITLE_DESC("Title (Z-A)"),
    ARTIST_ASC("Artist (A-Z)"),
    ALBUM_ASC("Album (A-Z)"),
    DURATION_DESC("Duration (Longest)"),
    YEAR_DESC("Year (Newest)")
}

enum class ThemeMode(val displayName: String) {
    DARK("Dark (OLED)"),
    LIGHT("Light")
}

enum class HexSkin(
    val displayName: String,
    val bgOled: Color,
    val surfaceColor: Color,
    val cardBorderColor: Color
) {
    OLED_BLACK("OLED Black", Color(0xFF000000), Color(0xFF101216), Color(0xFF22252D)),
    MIDNIGHT_BLUE("Midnight Blue", Color(0xFF040814), Color(0xFF0D172A), Color(0xFF1E2E4A)),
    OBSIDIAN("Obsidian", Color(0xFF08070B), Color(0xFF14121A), Color(0xFF242030)),
    TITANIUM("Titanium", Color(0xFF0D0F12), Color(0xFF181C22), Color(0xFF2A313C))
}

class AppState(
    private val context: Context,
    val scope: CoroutineScope
) {
    val spectrumAnalyzer = SpectrumAnalyzer
    val eqEngine = ParametricEQEngine.getInstance(context)
    val audioPlayer = HexAudioPlayer(context, spectrumAnalyzer, scope)

    // Navigation Stack
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val navHistory = mutableListOf<Screen>()
    private val _canGoBack = MutableStateFlow(false)
    val canGoBack: StateFlow<Boolean> = _canGoBack.asStateFlow()

    // Artist Separation Settings
    private val _isArtistSeparationEnabled = MutableStateFlow(true)
    val isArtistSeparationEnabled: StateFlow<Boolean> = _isArtistSeparationEnabled.asStateFlow()

    private val _artistSeparators = MutableStateFlow(setOf(";", "/", "feat.", "featuring", "ft.", "&", " - ", ","))
    val artistSeparators: StateFlow<Set<String>> = _artistSeparators.asStateFlow()

    // Equalizer Sheet Visibility
    private val _isEqSheetVisible = MutableStateFlow(false)
    val isEqSheetVisible: StateFlow<Boolean> = _isEqSheetVisible.asStateFlow()

    // Navigation Sidebar Drawer State
    private val _isDrawerOpen = MutableStateFlow(false)
    val isDrawerOpen: StateFlow<Boolean> = _isDrawerOpen.asStateFlow()

    fun openDrawer() { _isDrawerOpen.value = true }
    fun closeDrawer() { _isDrawerOpen.value = false }
    fun toggleDrawer() { _isDrawerOpen.value = !_isDrawerOpen.value }

    // Library State
    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    // Track Sorting
    private val _trackSortOrder = MutableStateFlow(TrackSortOrder.TITLE_ASC)
    val trackSortOrder: StateFlow<TrackSortOrder> = _trackSortOrder.asStateFlow()

    // Library Tab Memory
    private val _selectedLibraryTabIndex = MutableStateFlow(0)
    val selectedLibraryTabIndex: StateFlow<Int> = _selectedLibraryTabIndex.asStateFlow()

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private val _recentTracks = MutableStateFlow<List<Track>>(emptyList())
    val recentTracks: StateFlow<List<Track>> = _recentTracks.asStateFlow()

    private val _playCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val playCounts: StateFlow<Map<String, Int>> = _playCounts.asStateFlow()

    private val _playlists = MutableStateFlow<Map<String, List<Track>>>(emptyMap())
    val playlists: StateFlow<Map<String, List<Track>>> = _playlists.asStateFlow()

    // Lyrics & Artist Info
    private val _lyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val lyrics: StateFlow<List<LyricLine>> = _lyrics.asStateFlow()

    private val _artistDetails = MutableStateFlow<ArtistDetails?>(null)
    val artistDetails: StateFlow<ArtistDetails?> = _artistDetails.asStateFlow()

    // Appearance & Dynamic Accents
    private val _themeMode = MutableStateFlow(ThemeMode.DARK)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _activeSkin = MutableStateFlow(HexSkin.OLED_BLACK)
    val activeSkin: StateFlow<HexSkin> = _activeSkin.asStateFlow()

    private val _accentColorPrimary = MutableStateFlow(HexColors.CobaltBlue)
    val accentColorPrimary: StateFlow<Color> = _accentColorPrimary.asStateFlow()

    private val _accentColorSecondary = MutableStateFlow(HexColors.Lavender)
    val accentColorSecondary: StateFlow<Color> = _accentColorSecondary.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val prefs = context.getSharedPreferences("hex_app_state", Context.MODE_PRIVATE)

    init {
        audioPlayer.attachEqEngine(eqEngine)
        loadPersistedState()
    }

    fun navigateTo(screen: Screen) {
        if (_currentScreen.value != screen) {
            navHistory.add(_currentScreen.value)
            _currentScreen.value = screen
            _canGoBack.value = navHistory.isNotEmpty()
            if (screen is Screen.ArtistBio) {
                _artistDetails.value = null // IMMEDIATELY clear previous details!
                loadArtistDetails(screen.artistName)
            }
        }
    }

    fun goBack() {
        if (navHistory.isNotEmpty()) {
            val prev = navHistory.removeAt(navHistory.size - 1)
            _currentScreen.value = prev
            _canGoBack.value = navHistory.isNotEmpty()
            if (prev is Screen.ArtistBio) {
                _artistDetails.value = null
                loadArtistDetails(prev.artistName)
            }
        } else {
            if (_currentScreen.value !is Screen.Home) {
                _currentScreen.value = Screen.Home
                _canGoBack.value = false
            }
        }
    }

    fun setTrackSortOrder(order: TrackSortOrder) {
        _trackSortOrder.value = order
        prefs.edit().putString("track_sort_order", order.name).apply()
    }

    fun setSelectedLibraryTabIndex(index: Int) {
        _selectedLibraryTabIndex.value = index
    }

    fun getSortedTracks(sourceTracks: List<Track> = _tracks.value): List<Track> {
        return when (_trackSortOrder.value) {
            TrackSortOrder.TITLE_ASC -> sourceTracks.sortedBy { it.title.lowercase() }
            TrackSortOrder.TITLE_DESC -> sourceTracks.sortedByDescending { it.title.lowercase() }
            TrackSortOrder.ARTIST_ASC -> sourceTracks.sortedBy { it.artist.lowercase() }
            TrackSortOrder.ALBUM_ASC -> sourceTracks.sortedBy { it.album.lowercase() }
            TrackSortOrder.DURATION_DESC -> sourceTracks.sortedByDescending { it.durationMs }
            TrackSortOrder.YEAR_DESC -> sourceTracks.sortedByDescending { it.year ?: 0 }
        }
    }

    fun autoFetchAllArtistDetails() {
        scope.launch(Dispatchers.IO) {
            val artists = getSeparatedArtists().keys
            for (artist in artists) {
                if (artist.isNotBlank() && !artist.equals("Unknown Artist", ignoreCase = true)) {
                    ArtistBioService.fetchArtistDetails(context, artist)
                }
            }
        }
    }

    fun showEqualizer(show: Boolean) {
        _isEqSheetVisible.value = show
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    fun setSkin(skin: HexSkin) {
        _activeSkin.value = skin
        prefs.edit().putString("active_skin", skin.name).apply()
    }

    fun playTrack(track: Track) {
        _currentTrack.value = track
        _lyrics.value = emptyList() // Immediately clear previous lyrics!
        audioPlayer.playTrack(track)
        addToRecent(track)
        incrementPlayCount(track)
        loadLyricsForTrack(track)
        extractDynamicColors(track)
        navigateTo(Screen.NowPlaying)
    }

    private fun incrementPlayCount(track: Track) {
        val current = _playCounts.value.toMutableMap()
        val count = (current[track.id] ?: 0) + 1
        current[track.id] = count
        _playCounts.value = current
        savePlayCounts()
    }

    fun skipNext() {
        val list = _tracks.value
        if (list.isEmpty()) return
        val current = _currentTrack.value
        val currentIndex = list.indexOfFirst { it.id == current?.id }
        val nextIndex = if (currentIndex in 0 until list.size - 1) currentIndex + 1 else 0
        playTrack(list[nextIndex])
    }

    fun skipPrevious() {
        val list = _tracks.value
        if (list.isEmpty()) return
        val current = _currentTrack.value
        val currentIndex = list.indexOfFirst { it.id == current?.id }
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else list.size - 1
        playTrack(list[prevIndex])
    }

    fun toggleArtistSeparation(enabled: Boolean) {
        _isArtistSeparationEnabled.value = enabled
        prefs.edit().putBoolean("artist_separation_enabled", enabled).apply()
    }

    fun setArtistSeparators(separators: Set<String>) {
        _artistSeparators.value = separators
        prefs.edit().putStringSet("artist_separators", separators).apply()
    }

    fun splitArtists(rawArtist: String): List<String> {
        if (!_isArtistSeparationEnabled.value || rawArtist.isBlank()) {
            return if (rawArtist.isNotBlank()) listOf(rawArtist.trim()) else listOf("Unknown Artist")
        }

        val activeSeps = _artistSeparators.value
        val regexParts = mutableListOf<String>()

        activeSeps.forEach { sep ->
            when (sep.lowercase().trim()) {
                ";" -> regexParts.add(";")
                "/" -> regexParts.add("/")
                "," -> regexParts.add(",")
                "&" -> regexParts.add("&")
                "-" -> regexParts.add("\\s+-\\s+")
                "feat." -> regexParts.add("(?i)\\bfeat\\.?\\s+")
                "ft." -> regexParts.add("(?i)\\bft\\.?\\s+")
                "featuring" -> regexParts.add("(?i)\\bfeaturing\\s+")
                else -> regexParts.add(java.util.regex.Pattern.quote(sep))
            }
        }

        if (regexParts.isEmpty()) {
            return listOf(rawArtist.trim())
        }

        val pattern = regexParts.joinToString("|")
        return rawArtist.split(Regex(pattern))
            .map { it.trim().trim('(', ')', '[', ']', ',', ';', '-') }
            .filter { it.isNotBlank() }
            .distinct()
            .ifEmpty { listOf(rawArtist.trim()) }
    }

    fun getSeparatedArtists(): Map<String, List<Track>> {
        val result = mutableMapOf<String, MutableList<Track>>()
        val currentTracks = _tracks.value

        if (!_isArtistSeparationEnabled.value) {
            return currentTracks.groupBy { if (it.artist.isNotBlank()) it.artist else "Unknown Artist" }
        }

        currentTracks.forEach { track ->
            val artists = splitArtists(track.artist)
            artists.forEach { artistName ->
                result.getOrPut(artistName) { mutableListOf() }.add(track)
            }
        }
        return result
    }

    fun toggleFavorite(track: Track) {
        val currentFavs = _favorites.value.toMutableSet()
        if (currentFavs.contains(track.id)) {
            currentFavs.remove(track.id)
        } else {
            currentFavs.add(track.id)
        }
        _favorites.value = currentFavs
        prefs.edit().putStringSet("favorite_ids", currentFavs).apply()
    }

    fun isFavorite(track: Track): Boolean = _favorites.value.contains(track.id)

    private fun addToRecent(track: Track) {
        val updated = listOf(track) + _recentTracks.value.filter { it.id != track.id }
        _recentTracks.value = updated.take(50)
    }

    fun fetchArtworkForTrack(track: Track) {
        scope.launch {
            val url = ArtworkFetchService.fetchArtwork(context, track)
            if (url != null) {
                val updated = track.copy(artworkUri = url)
                updateTrackInLibrary(updated)
                if (_currentTrack.value?.id == track.id) {
                    _currentTrack.value = updated
                    extractDynamicColors(updated)
                }
            }
        }
    }

    private fun updateTrackInLibrary(updated: Track) {
        _tracks.value = _tracks.value.map { if (it.id == updated.id) updated else it }
        saveCachedLibrary()
    }

    private fun loadLyricsForTrack(track: Track) {
        scope.launch {
            val parsedLyrics = LyricsService.getLyrics(context, track)
            _lyrics.value = parsedLyrics
        }
    }

    private fun loadArtistDetails(artistName: String) {
        scope.launch {
            val details = ArtistBioService.fetchArtistDetails(context, artistName)
            _artistDetails.value = details
        }
    }

    private fun extractDynamicColors(track: Track) {
        scope.launch(Dispatchers.IO) {
            val artUri = track.artworkUri
            var pColor = HexColors.CobaltBlue
            var sColor = HexColors.Lavender
            if (artUri != null) {
                try {
                    val uriString = artUri.toString()
                    val bitmap = when {
                        uriString.startsWith("content://") || uriString.startsWith("file://") -> {
                            context.contentResolver.openInputStream(artUri)?.use {
                                BitmapFactory.decodeStream(it)
                            }
                        }
                        uriString.startsWith("/") -> {
                            BitmapFactory.decodeFile(uriString)
                        }
                        else -> null
                    }
                    if (bitmap != null) {
                        val samplePixels = IntArray(100)
                        val scaled = Bitmap.createScaledBitmap(bitmap, 10, 10, false)
                        scaled.getPixels(samplePixels, 0, 10, 0, 0, 10, 10)
                        var rSum = 0L; var gSum = 0L; var bSum = 0L
                        samplePixels.forEach { px ->
                            rSum += (px shr 16) and 0xFF
                            gSum += (px shr 8) and 0xFF
                            bSum += px and 0xFF
                        }
                        val count = samplePixels.size.toFloat()
                        val r = (rSum / count) / 255f
                        val g = (gSum / count) / 255f
                        val b = (bSum / count) / 255f
                        pColor = Color(r.coerceIn(0.2f, 0.9f), g.coerceIn(0.2f, 0.9f), b.coerceIn(0.2f, 0.9f), 1f)
                        sColor = Color(
                            ((r + 0.3f) % 1f).coerceIn(0.4f, 0.95f),
                            ((g + 0.2f) % 1f).coerceIn(0.4f, 0.95f),
                            ((b + 0.4f) % 1f).coerceIn(0.4f, 0.95f),
                            1f
                        )
                    }
                } catch (_: Exception) {}
            }
            withContext(Dispatchers.Main) {
                _accentColorPrimary.value = pColor
                _accentColorSecondary.value = sColor
            }
        }
    }

    // Import Directory via SAF
    fun importDirectoryUri(treeUri: Uri) {
        scope.launch(Dispatchers.IO) {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(treeUri, takeFlags)
                prefs.edit().putString("persisted_tree_uri", treeUri.toString()).apply()
            } catch (e: Exception) {
                Log.e("AppState", "Failed to take persistable URI permission for treeUri: $treeUri", e)
            }

            _isScanning.value = true
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@launch
            val scannedTracks = mutableListOf<Track>()
            val existing = _tracks.value.toMutableList()
            val existingIds = existing.map { it.id }.toMutableSet()

            scanDocumentFileRecursive(root, scannedTracks) { batch ->
                // Progressive batch update for ultra-smooth UI responsiveness
                val newTracks = batch.filter { !existingIds.contains(it.id) }
                if (newTracks.isNotEmpty()) {
                    existing.addAll(newTracks)
                    existingIds.addAll(newTracks.map { it.id })
                    withContext(Dispatchers.Main) {
                        _tracks.value = existing.toList()
                        if (_currentTrack.value == null && existing.isNotEmpty()) {
                            _currentTrack.value = existing.first()
                        }
                    }
                }
            }

            withContext(Dispatchers.Main) {
                saveCachedLibrary()
                _isScanning.value = false
                autoFetchAllArtistDetails()
            }
        }
    }

    private fun scanDocumentFileRecursive(
        dir: DocumentFile,
        outList: MutableList<Track>,
        onBatchEmitted: suspend (List<Track>) -> Unit
    ) {
        val files = dir.listFiles()
        val currentBatch = mutableListOf<Track>()

        // Index companion .lrc files in the folder
        val lrcMap = files
            .filter { it.isFile && (it.name?.endsWith(".lrc", ignoreCase = true) == true) }
            .associateBy { it.name?.substringBeforeLast('.')?.lowercase() ?: "" }

        for (f in files) {
            if (f.isDirectory) {
                scanDocumentFileRecursive(f, outList, onBatchEmitted)
            } else if (f.isFile) {
                val name = f.name?.lowercase() ?: ""
                if (name.endsWith(".flac") || name.endsWith(".wav") || name.endsWith(".alac") ||
                    name.endsWith(".m4a") || name.endsWith(".mp3") || name.endsWith(".aac") ||
                    name.endsWith(".dsf") || name.endsWith(".dff") || name.endsWith(".aiff")) {
                    val track = AudioMetadataReader.readTrack(context, f.uri)
                    if (track != null) {
                        val baseName = f.name?.substringBeforeLast('.')?.lowercase() ?: ""
                        val lrcDoc = lrcMap[baseName]
                        if (lrcDoc != null) {
                            try {
                                context.contentResolver.openInputStream(lrcDoc.uri)?.use { stream ->
                                    val text = stream.bufferedReader(Charsets.UTF_8).readText()
                                    val parsed = LyricsService.parseLrc(text)
                                    if (parsed.isNotEmpty()) {
                                        track.syncedLyrics = parsed
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                        outList.add(track)
                        currentBatch.add(track)
                        if (currentBatch.size >= 15) {
                            kotlinx.coroutines.runBlocking {
                                onBatchEmitted(currentBatch.toList())
                            }
                            currentBatch.clear()
                        }
                    }
                }
            }
        }
        if (currentBatch.isNotEmpty()) {
            kotlinx.coroutines.runBlocking {
                onBatchEmitted(currentBatch.toList())
            }
        }
    }

    private fun savePlayCounts() {
        val obj = JSONObject()
        _playCounts.value.forEach { (k, v) -> obj.put(k, v) }
        prefs.edit().putString("play_counts_json", obj.toString()).apply()
    }

    private fun saveCachedLibrary() {
        val list = _tracks.value
        val arr = JSONArray()
        list.forEach { t ->
            val obj = JSONObject().apply {
                put("id", t.id)
                put("title", t.title)
                put("artist", t.artist)
                put("album", t.album)
                put("genre", t.genre)
                put("duration", t.duration)
                put("durationMs", t.durationMs)
                put("path", t.path)
                put("uri", t.uri.toString())
                put("artworkUri", t.artworkUri?.toString() ?: "")
                put("sampleRate", t.sampleRate)
                put("bitDepth", t.bitDepth)
                put("bitrate", t.bitrate)
                put("codec", t.codec.name)
                put("year", t.year ?: 0)
                put("isHiRes", t.isHiRes)
            }
            arr.put(obj)
        }
        prefs.edit().putString("cached_library_json", arr.toString()).apply()
    }

    private fun loadPersistedState() {
        val favs = prefs.getStringSet("favorite_ids", emptySet()) ?: emptySet()
        _favorites.value = favs

        val savedTheme = prefs.getString("theme_mode", ThemeMode.DARK.name)
        try {
            _themeMode.value = ThemeMode.valueOf(savedTheme ?: ThemeMode.DARK.name)
        } catch (_: Exception) {}

        val savedSkin = prefs.getString("active_skin", HexSkin.OLED_BLACK.name)
        try {
            _activeSkin.value = HexSkin.valueOf(savedSkin ?: HexSkin.OLED_BLACK.name)
        } catch (_: Exception) {}

        val isSepEnabled = prefs.getBoolean("artist_separation_enabled", true)
        _isArtistSeparationEnabled.value = isSepEnabled

        val savedSeps = prefs.getStringSet("artist_separators", null)
        if (savedSeps != null) {
            _artistSeparators.value = savedSeps
        }

        val pcJson = prefs.getString("play_counts_json", null)
        if (!pcJson.isNullOrBlank()) {
            try {
                val obj = JSONObject(pcJson)
                val map = mutableMapOf<String, Int>()
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    map[k] = obj.getInt(k)
                }
                _playCounts.value = map
            } catch (_: Exception) {}
        }

        val cachedJson = prefs.getString("cached_library_json", null)
        if (!cachedJson.isNullOrBlank()) {
            try {
                val arr = JSONArray(cachedJson)
                val list = mutableListOf<Track>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val pathStr = obj.optString("path", "")
                    val uriStr = obj.optString("uri", pathStr)
                    val parsedUri = if (uriStr.isNotBlank()) Uri.parse(uriStr) else Uri.EMPTY
                    val artStr = obj.optString("artworkUri", "").takeIf { it.isNotBlank() }
                    val artUri = if (artStr != null) Uri.parse(artStr) else null
                    val durMs = obj.optLong("durationMs", (obj.optDouble("duration", 0.0) * 1000).toLong())
                    list.add(
                        Track(
                            id = obj.getString("id"),
                            uri = parsedUri,
                            title = obj.getString("title"),
                            artist = obj.getString("artist"),
                            album = obj.getString("album"),
                            genre = obj.optString("genre", "Unknown"),
                            year = obj.optInt("year", 0).takeIf { it > 0 },
                            durationMs = durMs,
                            sampleRate = obj.optDouble("sampleRate", 44100.0),
                            bitDepth = obj.optInt("bitDepth", 16),
                            bitRate = obj.optInt("bitRate", obj.optInt("bitrate", 0)),
                            codec = try { com.techressive.hex.metadata.AudioCodec.valueOf(obj.getString("codec")) } catch (_: Exception) { com.techressive.hex.metadata.AudioCodec.UNKNOWN },
                            artworkUri = artUri
                        )
                    )
                }
                _tracks.value = list
                if (list.isNotEmpty()) {
                    _currentTrack.value = list.first()
                }
            } catch (_: Exception) {}
        }
    }
}
