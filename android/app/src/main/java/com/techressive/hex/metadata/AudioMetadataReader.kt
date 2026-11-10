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

        try {
            if (uri.scheme == "content") {
                val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                if (pfd != null) {
                    pfd.use {
                        retriever.setDataSource(it.fileDescriptor)
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.let { t -> if (t.isNotBlank()) title = t }
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.let { a -> if (a.isNotBlank()) artist = a }
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.let { al -> if (al.isNotBlank()) album = al }
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)?.let { g -> if (g.isNotBlank()) genre = g }
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.let { d -> durationMs = d }
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)?.toIntOrNull()?.let { n -> trackNumber = n }
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)?.toIntOrNull()?.let { y -> year = y }
                        embeddedPicBytes = retriever.embeddedPicture
                    }
                } else {
                    retriever.setDataSource(context, uri)
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.let { t -> if (t.isNotBlank()) title = t }
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.let { a -> if (a.isNotBlank()) artist = a }
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.let { al -> if (al.isNotBlank()) album = al }
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)?.let { g -> if (g.isNotBlank()) genre = g }
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.let { d -> durationMs = d }
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)?.toIntOrNull()?.let { n -> trackNumber = n }
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)?.toIntOrNull()?.let { y -> year = y }
                    embeddedPicBytes = retriever.embeddedPicture
                }
            } else {
                retriever.setDataSource(context, uri)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.let { t -> if (t.isNotBlank()) title = t }
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.let { a -> if (a.isNotBlank()) artist = a }
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.let { al -> if (al.isNotBlank()) album = al }
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)?.let { g -> if (g.isNotBlank()) genre = g }
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.let { d -> durationMs = d }
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)?.toIntOrNull()?.let { n -> trackNumber = n }
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)?.toIntOrNull()?.let { y -> year = y }
                embeddedPicBytes = retriever.embeddedPicture
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
            sampleRate = 96000.0,
            bitDepth = 24,
            codec = codec,
            artworkUri = artworkUri
        )
    }
}
