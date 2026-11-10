package com.techressive.hex.metadata

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object LyricsService {

    private val lyricsCache = mutableMapOf<String, List<LyricLine>>()

    suspend fun getLyrics(context: Context, track: Track): List<LyricLine> = withContext(Dispatchers.IO) {
        val cacheKey = "${track.title}_${track.artist}".lowercase()
        lyricsCache[cacheKey]?.let { return@withContext it }

        // 1. Check local companion .lrc file in track's parent directory (or SAF tree)
        try {
            if (track.path.isNotBlank()) {
                val audioFile = File(track.path)
                val candidates = listOf(
                    File(audioFile.parentFile, "${audioFile.nameWithoutExtension}.lrc"),
                    File(audioFile.parentFile, "${audioFile.nameWithoutExtension}.LRC"),
                    File(audioFile.parentFile, "${track.title}.lrc")
                )
                for (cand in candidates) {
                    if (cand.exists() && cand.canRead()) {
                        val bytes = cand.readBytes()
                        val text = decodeWithFallbackCharsets(bytes)
                        val parsed = parseLrc(text)
                        if (parsed.isNotEmpty()) {
                            lyricsCache[cacheKey] = parsed
                            return@withContext parsed
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // 2. Check disk cache
        val diskCacheDir = File(context.cacheDir, "lyrics")
        if (!diskCacheDir.exists()) diskCacheDir.mkdirs()
        val cacheFile = File(diskCacheDir, "${cacheKey.hashCode()}.lrc")
        if (cacheFile.exists()) {
            val bytes = cacheFile.readBytes()
            val text = decodeWithFallbackCharsets(bytes)
            val parsed = parseLrc(text)
            if (parsed.isNotEmpty()) {
                lyricsCache[cacheKey] = parsed
                return@withContext parsed
            }
        }

        // 3. Query LRCLIB API with full UTF-8 encoding (supporting Persian, Arabic, East Asian, and European scripts)
        try {
            val tName = URLEncoder.encode(track.title, "UTF-8")
            val aName = URLEncoder.encode(track.artist, "UTF-8")
            val albName = URLEncoder.encode(track.album, "UTF-8")
            val dur = track.duration.toInt()

            val apiUrl = "https://lrclib.net/api/get?track_name=$tName&artist_name=$aName&album_name=$albName&duration=$dur"
            val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", "HEX-Audiophile-Android/1.0 (lyrics@hex.app)")
                setRequestProperty("Accept-Charset", "UTF-8")
            }

            if (conn.responseCode == 200) {
                val bytes = conn.inputStream.use { it.readBytes() }
                val response = decodeWithFallbackCharsets(bytes)
                val json = JSONObject(response)
                val syncedLyrics = json.optString("syncedLyrics", "")
                if (syncedLyrics.isNotBlank()) {
                    val parsed = parseLrc(syncedLyrics)
                    if (parsed.isNotEmpty()) {
                        lyricsCache[cacheKey] = parsed
                        cacheFile.writeText(syncedLyrics, Charsets.UTF_8)
                        conn.disconnect()
                        return@withContext parsed
                    }
                }

                val plainLyrics = json.optString("plainLyrics", "")
                if (plainLyrics.isNotBlank()) {
                    val fallback = parsePlainLyrics(plainLyrics, track.duration)
                    lyricsCache[cacheKey] = fallback
                    conn.disconnect()
                    return@withContext fallback
                }
            }
            conn.disconnect()
        } catch (_: Exception) {}

        // 4. Return procedural or empty lines
        emptyList()
    }

    fun parseLrc(content: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        val timeRegex = Regex("""\[(\d{1,2}):(\d{2})(?:\.(\d{2,3}))?\]""")

        content.lines().forEach { rawLine ->
            val match = timeRegex.find(rawLine)
            if (match != null) {
                val minutes = match.groupValues[1].toDoubleOrNull() ?: 0.0
                val seconds = match.groupValues[2].toDoubleOrNull() ?: 0.0
                val msString = match.groupValues[3]
                val fraction = when {
                    msString.length == 2 -> (msString.toDoubleOrNull() ?: 0.0) / 100.0
                    msString.length == 3 -> (msString.toDoubleOrNull() ?: 0.0) / 1000.0
                    else -> 0.0
                }
                val timestamp = minutes * 60.0 + seconds + fraction
                val text = rawLine.replace(timeRegex, "").trim()
                if (text.isNotBlank()) {
                    lines.add(LyricLine(timestamp, text))
                }
            }
        }
        return lines.sortedBy { it.timestamp }
    }

    private fun parsePlainLyrics(plain: String, totalDuration: Double): List<LyricLine> {
        val lines = plain.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()
        val step = if (totalDuration > 0) totalDuration / lines.size else 4.0
        return lines.mapIndexed { index, text ->
            LyricLine(timestamp = index * step, text = text)
        }
    }

    private fun decodeWithFallbackCharsets(bytes: ByteArray): String {
        return try {
            String(bytes, java.nio.charset.StandardCharsets.UTF_8)
        } catch (_: Exception) {
            try {
                // Windows-1256 (Arabic & Persian)
                String(bytes, java.nio.charset.Charset.forName("windows-1256"))
            } catch (_: Exception) {
                String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1)
            }
        }
    }
}
