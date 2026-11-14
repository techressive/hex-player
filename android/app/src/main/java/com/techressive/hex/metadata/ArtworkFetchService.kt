package com.techressive.hex.metadata

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class ArtworkFetchService private constructor(private val context: Context) {
    companion object {
        @Volatile
        private var instance: ArtworkFetchService? = null

        fun getInstance(context: Context): ArtworkFetchService {
            return instance ?: synchronized(this) {
                instance ?: ArtworkFetchService(context.applicationContext).also { instance = it }
            }
        }

        suspend fun fetchArtwork(context: Context, track: Track): Uri? {
            return getInstance(context).fetchAndSaveArtwork(track)
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val cacheDir = File(context.cacheDir, "HEX/Artwork").apply { mkdirs() }

    private val _fetchingTrackIds = MutableStateFlow<Set<String>>(emptySet())
    val fetchingTrackIds: StateFlow<Set<String>> = _fetchingTrackIds.asStateFlow()

    fun isFetching(trackId: String): Boolean = _fetchingTrackIds.value.contains(trackId)

    suspend fun fetchAndSaveArtwork(track: Track): Uri? = withContext(Dispatchers.IO) {
        _fetchingTrackIds.value = _fetchingTrackIds.value + track.id
        try {
            val onlineUrl = findArtworkUrl(track.title, track.artist, track.album) ?: return@withContext null
            val req = Request.Builder().url(onlineUrl).build()
            val resp = httpClient.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext null

            val bytes = resp.body?.bytes() ?: return@withContext null

            // 1. Save companion cover.jpg in track directory if file scheme
            if (track.uri.scheme == "file") {
                try {
                    val trackFile = File(track.uri.path ?: "")
                    val companionCover = File(trackFile.parentFile, "cover.jpg")
                    if (!companionCover.exists()) {
                        FileOutputStream(companionCover).use { it.write(bytes) }
                    }
                } catch (ignored: Exception) {}
            }

            // 2. Save to persistent cache deduplicated by album and compressed
            val artKey = "art_${track.artist.trim()}_${track.album.trim()}".hashCode().toString()
            val cachedFile = File(cacheDir, "art_$artKey.jpg")
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bitmap != null) {
                val maxDim = 600
                val scaled = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                    val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                    val (w, h) = if (ratio > 1f) maxDim to (maxDim / ratio).toInt() else (maxDim * ratio).toInt() to maxDim
                    Bitmap.createScaledBitmap(bitmap, w, h, true)
                } else {
                    bitmap
                }
                FileOutputStream(cachedFile).use { fos ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, 80, fos)
                }
                if (scaled != bitmap) {
                    bitmap.recycle()
                }
                scaled.recycle()
            } else {
                FileOutputStream(cachedFile).use { it.write(bytes) }
            }

            val uri = Uri.fromFile(cachedFile)
            track.artworkUri = uri
            return@withContext uri
        } catch (e: Exception) {
            return@withContext null
        } finally {
            _fetchingTrackIds.value = _fetchingTrackIds.value - track.id
        }
    }

    private fun findArtworkUrl(title: String, artist: String, album: String): String? {
        val queries = listOf("$artist $title", "$artist $album", title)
        for (q in queries) {
            val itunes = searchItunes(q)
            if (itunes != null) return itunes
        }
        for (q in queries) {
            val deezer = searchDeezer(q)
            if (deezer != null) return deezer
        }
        return null
    }

    private fun searchItunes(query: String): String? {
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://itunes.apple.com/search?term=$encoded&media=music&entity=song&limit=1"
            val req = Request.Builder().url(url).build()
            val resp = httpClient.newCall(req).execute()
            val body = resp.body?.string() ?: return null
            val json = JSONObject(body)
            val results = json.optJSONArray("results") ?: return null
            if (results.length() == 0) return null
            val rawUrl = results.getJSONObject(0).optString("artworkUrl100")
            if (rawUrl.isEmpty()) return null
            rawUrl.replace("100x100bb.jpg", "1200x1200bb.jpg")
                .replace("100x100bb", "1200x1200bb")
        } catch (e: Exception) {
            null
        }
    }

    private fun searchDeezer(query: String): String? {
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://api.deezer.com/search?q=$encoded&limit=1"
            val req = Request.Builder().url(url).build()
            val resp = httpClient.newCall(req).execute()
            val body = resp.body?.string() ?: return null
            val json = JSONObject(body)
            val data = json.optJSONArray("data") ?: return null
            if (data.length() == 0) return null
            val albumObj = data.getJSONObject(0).optJSONObject("album") ?: return null
            albumObj.optString("cover_xl").ifEmpty {
                albumObj.optString("cover_big").ifEmpty {
                    albumObj.optString("cover_medium")
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
