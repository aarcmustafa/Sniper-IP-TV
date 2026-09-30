package com.stitten.stitteniptv.player

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

object CacheManager {
    private var cache: SimpleCache? = null
    private var currentLiteMode: Boolean? = null

    @Synchronized
    fun get(context: Context, liteMode: Boolean = false): SimpleCache {
        if (cache != null && currentLiteMode != liteMode) {
            cache?.release()
            cache = null
        }

        cache?.let { return it }

        val cacheDir = File(context.cacheDir, "media_cache")
        val maxSize = if (liteMode) {
            30L * 1024 * 1024
        } else {
            200L * 1024 * 1024
        }

        val evictor = LeastRecentlyUsedCacheEvictor(maxSize)
        val db = StandaloneDatabaseProvider(context)
        val newCache = SimpleCache(cacheDir, evictor, db)

        cache = newCache
        currentLiteMode = liteMode
        return newCache
    }

    @Synchronized
    fun clear() {
        cache?.release()
        cache = null
        currentLiteMode = null
    }
}
