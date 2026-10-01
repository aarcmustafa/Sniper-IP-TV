package com.stitten.stitteniptv.data

import okhttp3.Dns
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap

object SmartDns : Dns {

    private val cache = ConcurrentHashMap<String, List<InetAddress>>()
    private val cacheTime = ConcurrentHashMap<String, Long>()
    private const val CACHE_TTL_MS = 10 * 60 * 1000L

    override fun lookup(hostname: String): List<InetAddress> {
        val cached = cache[hostname]
        val cachedTime = cacheTime[hostname] ?: 0L
        if (cached != null && (System.currentTimeMillis() - cachedTime) < CACHE_TTL_MS) {
            return cached
        }

        try {
            val result = InetAddress.getAllByName(hostname).toList()
            cache[hostname] = result
            cacheTime[hostname] = System.currentTimeMillis()
            return result
        } catch (e: UnknownHostException) {
            if (cached != null) return cached
            throw e
        }
    }

    fun clearCache() {
        cache.clear()
        cacheTime.clear()
    }
}
