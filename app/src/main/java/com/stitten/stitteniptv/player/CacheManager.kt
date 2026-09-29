package com.stitten.stitteniptv.player

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

object CacheManager {
    private var cache: SimpleCache? = null

    @Synchronized
    fun get(context: Context): SimpleCache {
        cache?.let { return it }
        val cacheDir = File(context.cacheDir, "media_cache")
        val evictor = LeastRecentlyUsedCacheEvictor(200L * 1024 * 1024)
        val db = StandaloneDatabaseProvider(context)
        val newCache = SimpleCache(cacheDir, evictor, db)
        cache = newCache
        return newCache
    }
}
