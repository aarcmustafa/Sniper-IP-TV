package com.stitten.stitteniptv

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.stitten.stitteniptv.data.DeviceCapabilityDetector
import com.stitten.stitteniptv.data.PrefsManager
import com.stitten.stitteniptv.data.WatchHistoryManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class StittenApp : Application(), ImageLoaderFactory {

    @Inject
    lateinit var watchHistoryManager: WatchHistoryManager

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        performAutoDetection()
        performStartupCleanup()
    }

    private fun performAutoDetection() {
        val prefs = PrefsManager(this)
        if (prefs.autoDetectionDone) return

        val capability = DeviceCapabilityDetector.detect(this)
        prefs.autoDetectionDone = true
        prefs.deviceIsLowEnd = capability.isLowEnd
        prefs.deviceCapabilityInfo = capability.reason

        if (capability.isLowEnd && !prefs.userDisabledLiteMode) {
            prefs.liteModeEnabled = true
        }
    }

    private fun performStartupCleanup() {
        appScope.launch {
            try {
                watchHistoryManager.performFullCleanup()
            } catch (e: Exception) { }
        }
    }

    override fun newImageLoader(): ImageLoader {
        val prefs = PrefsManager(this)
        val liteMode = prefs.liteModeEnabled

        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(if (liteMode) 0.10 else 0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(
                        if (liteMode) 30L * 1024 * 1024
                        else 250L * 1024 * 1024
                    )
                    .build()
            }
            .crossfade(!liteMode)
            .build()
    }
}
