package com.techressive.hex

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import java.io.File

class HexApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        pruneCacheIfNeeded()
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil_cache"))
                    .maxSizeBytes(25L * 1024L * 1024L) // Strictly cap Coil disk cache to 25 MB max
                    .build()
            }
            .respectCacheHeaders(false)
            .build()
    }

    private fun pruneCacheIfNeeded() {
        try {
            val hexDir = File(cacheDir, "HEX")
            if (hexDir.exists()) {
                val files = hexDir.walkTopDown().filter { it.isFile }.sortedBy { it.lastModified() }.toList()
                var totalSize = files.sumOf { it.length() }
                val maxAllowed = 30L * 1024L * 1024L // 30 MB
                if (totalSize > maxAllowed) {
                    for (file in files) {
                        val len = file.length()
                        if (file.delete()) {
                            totalSize -= len
                            if (totalSize <= maxAllowed) break
                        }
                    }
                }
            }
        } catch (_: Exception) {}
    }
}
