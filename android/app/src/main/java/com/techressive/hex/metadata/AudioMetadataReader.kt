package com.techressive.hex.metadata

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.File

object AudioMetadataReader {

    fun readTrack(context: Context, uri: Uri): Track? = readMetadata(context, uri)

    fun readMetadata(context: Context, uri: Uri): Track {
        val retriever = MediaMetadataRetriever()
        var title = uri.lastPathSegment ?: "Unknown Track"
        var artist = "Unknown Artist"
        var album = "Unknown Album"
        var genre = "Audiophile Master"
        var durationMs = 0L
        var trackNumber: Int? = 1
        var year: Int? = null
        var embeddedPicBytes: ByteArray? = null

        val ext = (uri.path ?: "").substringAfterLast('.', "").lowercase()
        val codec = when (ext) {
            "flac" -> AudioCodec.FLAC
            "wav" -> AudioCodec.WAV
            "alac", "m4a" -> AudioCodec.ALAC
            "aiff", "aif" -> AudioCodec.AIFF
            "dsf", "dff", "dsd" -> AudioCodec.DSD
            "mp3" -> AudioCodec.MP3
            "ogg" -> AudioCodec.OGG
            "opus" -> AudioCodec.OPUS
            else -> AudioCodec.UNKNOWN
        }

        var sampleRate = 44100.0
        var bitDepth = 16
        var bitrate = 320000
        var embeddedLyrics: String? = null

        try {
            val extractMetadataFun: (MediaMetadataRetriever) -> Unit = { r ->
                r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.let { t -> if (t.isNotBlank()) title = t }
                r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.let { a -> if (a.isNotBlank()) artist = a }
                r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.let { al -> if (al.isNotBlank()) album = al }
                r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)?.let { g -> if (g.isNotBlank()) genre = g }
                r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.let { d -> durationMs = d }
                r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)?.toIntOrNull()?.let { n -> trackNumber = n }
                r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)?.toIntOrNull()?.let { y -> year = y }
                r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()?.let { b -> bitrate = b }
                // Sample rate detection (API 29+)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toDoubleOrNull()?.let { sr ->
                        if (sr > 0) sampleRate = sr
                    }
                    r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITS_PER_SAMPLE)?.toIntOrNull()?.let { bd ->
                        if (bd > 0) bitDepth = bd
                    }
                }
                // Check if bitrate suggests hi-res/lossless
                if (bitDepth == 16 && (ext == "flac" || ext == "alac" || ext == "wav")) {
                    if (bitrate > 2000000) {
                        bitDepth = 24
                    }
                }
                if (sampleRate == 44100.0 && (ext == "dsf" || ext == "dff")) {
                    sampleRate = 2822400.0
                    bitDepth = 1
                }
                embeddedPicBytes = r.embeddedPicture
            }

            if (uri.scheme == "content") {
                val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                if (pfd != null) {
                    pfd.use {
                        retriever.setDataSource(it.fileDescriptor)
                        extractMetadataFun(retriever)
                    }
                } else {
                    retriever.setDataSource(context, uri)
                    extractMetadataFun(retriever)
                }
            } else {
                retriever.setDataSource(context, uri)
                extractMetadataFun(retriever)
            }
        } catch (_: Exception) {
            // Fallback or ignore
        } finally {
            try { retriever.release() } catch (_: Throwable) {}
        }

        // Clean up title if it's still a raw filename
        if (title.contains(".") && (title.endsWith(".$ext", ignoreCase = true) || title.contains("/"))) {
            title = title.substringAfterLast('/').substringBeforeLast('.')
        }

        var artworkUri: Uri? = null

        // 1. If embedded picture exists in audio file, cache it locally for Coil
        if (embeddedPicBytes != null && embeddedPicBytes!!.isNotEmpty()) {
            try {
                val artCacheDir = File(context.cacheDir, "HEX/artworks").apply { mkdirs() }
                // Use hash of album + artist or URI for stable caching
                val artKey = "${artist.trim()}_${album.trim()}".hashCode().toString()
                val artFile = File(artCacheDir, "art_$artKey.jpg")
                if (!artFile.exists() || artFile.length() == 0L) {
                    artFile.writeBytes(embeddedPicBytes!!)
                }
                artworkUri = Uri.fromFile(artFile)
            } catch (_: Exception) {}
        }

        // 2. Check for companion cover.jpg in the same folder if accessible as File
        if (artworkUri == null && uri.scheme == "file") {
            try {
                val file = File(uri.path ?: "")
                val folder = file.parentFile
                if (folder != null && folder.exists()) {
                    val candidates = listOf("cover.jpg", "folder.jpg", "cover.png", "Front.jpg", "front.png", "artwork.jpg")
                    for (name in candidates) {
                        val cover = File(folder, name)
                        if (cover.exists() && cover.length() > 0) {
                            artworkUri = Uri.fromFile(cover)
                            break
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        return Track(
            uri = uri,
            title = title,
            artist = artist,
            album = album,
            genre = genre,
            year = year,
            durationMs = durationMs,
            trackNumber = trackNumber,
            sampleRate = sampleRate,
            bitDepth = bitDepth,
            bitRate = bitrate,
            codec = codec,
            artworkUri = artworkUri
        )
    }
}
