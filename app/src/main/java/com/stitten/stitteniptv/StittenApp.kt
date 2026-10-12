package com.stitten.stitteniptv

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class StittenApp : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            // ✅ Memory Cache: 25% من ذاكرة التطبيق
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            // ✅ Disk Cache: 100 MB
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100L * 1024 * 1024)
                    .build()
            }
            // ✅ سياسات Cache
            .respectCacheHeaders(false)  // مهم للـ IPTV (بعض السيرفرات لا تُرسل headers)
            .crossfade(true)
            .build()
    }
}
