package com.techressive.hex.metadata

import android.net.Uri
import java.util.UUID

enum class AudioCodec(val displayName: String, val isLossless: Boolean) {
    FLAC("FLAC", true),
    WAV("WAV", true),
    ALAC("ALAC", true),
    AIFF("AIFF", true),
    DSD("DSD", true),
    MP3("MP3", false),
    AAC("AAC", false),
    OGG("OGG", false),
    OPUS("OPUS", false),
    UNKNOWN("AUDIO", false)
}

data class LyricLine(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Double = 0.0,
    val text: String = "",
    val timeMs: Long = (timestamp * 1000).toLong()
) {
    constructor(timestamp: Double, text: String) : this(
        id = UUID.randomUUID().toString(),
        timestamp = timestamp,
        text = text,
        timeMs = (timestamp * 1000).toLong()
    )
}

data class Track(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String = "Audiophile Master",
    val year: Int? = null,
    val durationMs: Long = 0L,
    val trackNumber: Int? = 1,
    val sampleRate: Double = 48000.0,
    val bitDepth: Int = 24,
    val channels: Int = 2,
    val bitRate: Int = 2304000,
    val codec: AudioCodec = AudioCodec.FLAC,
    var artworkUri: Uri? = null,
    var syncedLyrics: List<LyricLine> = emptyList(),
    var plainLyrics: String? = null
) {
    val path: String
        get() = uri.path ?: uri.toString()

    val bitrate: Int
        get() = bitRate

    val duration: Double
        get() = durationMs / 1000.0

    val formattedSampleRate: String
        get() = when {
            sampleRate >= 1_000_000 -> String.format("%.1f MHz", sampleRate / 1_000_000.0)
            sampleRate >= 1000 -> {
                val khz = sampleRate / 1000.0
                if (khz % 1.0 == 0.0) "${khz.toInt()} kHz" else String.format("%.1f kHz", khz)
            }
            else -> "${sampleRate.toInt()} Hz"
        }

    val formattedBitDepth: String
        get() = if (codec == AudioCodec.DSD) "1-bit Direct Stream" else "$bitDepth-bit"

    val audiophileBadgeText: String
        get() = "${codec.displayName} • $formattedBitDepth / $formattedSampleRate"

    val isHiRes: Boolean
        get() = sampleRate >= 88200 || bitDepth >= 24 || codec == AudioCodec.DSD

    fun formatString(): String = "$formattedBitDepth / $formattedSampleRate"
}
