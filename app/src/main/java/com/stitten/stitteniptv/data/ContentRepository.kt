package com.stitten.stitteniptv.data

object ContentRepository {
    var channels: List<Channel> = emptyList()
    var movies: List<Movie> = emptyList()
    var series: List<Series> = emptyList()
    var episodes: Map<String, List<Episode>> = emptyMap()

    /**
     * ذاكرة مؤقتة للروابط المباشرة
     * المفتاح: الرابط الأصلي (مع 302)
     * القيمة: الرابط المباشر (بعد 302)
     */
    private val directUrlCache = mutableMapOf<String, String>()

    fun getDirectUrl(originalUrl: String): String? {
        return directUrlCache[originalUrl]
    }

    fun saveDirectUrl(originalUrl: String, directUrl: String) {
        if (directUrl.isNotBlank() && directUrl != originalUrl) {
            directUrlCache[originalUrl] = directUrl
        }
    }

    fun clearDirectUrlCache() {
        directUrlCache.clear()
    }

    fun clear() {
        channels = emptyList()
        movies = emptyList()
        series = emptyList()
        episodes = emptyMap()
        directUrlCache.clear()
    }
}
