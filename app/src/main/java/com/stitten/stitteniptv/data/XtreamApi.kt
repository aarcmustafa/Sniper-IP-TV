package com.stitten.stitteniptv.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object XtreamApi {

    @Volatile
    private var preferredProtocol: String = "http"

    @Volatile
    private var preferredFormat: String = "ts"

    fun setPreferredProtocol(useHttps: Boolean) {
        preferredProtocol = if (useHttps) "https" else "http"
    }

    fun setPreferredFormat(format: String) {
        preferredFormat = if (format == "m3u8" || format == "hls") "m3u8" else "ts"
    }

    private data class RawChannel(val id: String, val name: String, val icon: String, val categoryId: String)
    private data class RawMovie(val id: String, val name: String, val poster: String, val categoryId: String, val ext: String)
    private data class RawSeries(val id: String, val name: String, val cover: String, val categoryId: String)

    // ============== التحقق ==============
    suspend fun validate(server: String, user: String, pass: String): Boolean =
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            try {
                XtreamLogger.log("VALIDATE", "بدء التحقق من السيرفر: $server")
                val s = normalizeServer(server)
                val url = "$s/player_api.php?username=$user&password=$pass"

                val result = HttpClientProvider.fetchText(url)

                if (result.isFailure) {
                    val elapsed = System.currentTimeMillis() - startTime
                    XtreamLogger.log("VALIDATE", "❌ فشل الاتصال بعد ${elapsed}ms: ${result.exceptionOrNull()?.message}")
                    return@withContext false
                }

                val body = result.getOrNull() ?: ""
                if (body.isBlank() || body.trimStart().startsWith("<")) {
                    XtreamLogger.log("VALIDATE", "❌ السيرفر أرجع HTML بدلاً من JSON — لا يدعم Xtream API")
                    return@withContext false
                }

                val json = JSONObject(body)
                val userInfo = json.optJSONObject("user_info")
                if (userInfo == null) {
                    XtreamLogger.log("VALIDATE", "❌ لا يوجد user_info في الاستجابة")
                    return@withContext false
                }

                val auth = userInfo.opt("auth")
                val authString = when (auth) {
                    is Number -> auth.toInt().toString()
                    is String -> auth
                    else -> ""
                }

                val isValid = authString == "1"
                val elapsed = System.currentTimeMillis() - startTime
                XtreamLogger.log("VALIDATE",
                    if (isValid) "✅ نجح التحقق في ${elapsed}ms"
                    else "❌ فشل التحقق (auth=$authString) في ${elapsed}ms"
                )
                isValid
            } catch (e: Exception) {
                val elapsed = System.currentTimeMillis() - startTime
                XtreamLogger.logException("VALIDATE", "التحقق", e)
                XtreamLogger.log("VALIDATE", "❌ خطأ بعد ${elapsed}ms: ${e.message}")
                false
            }
        }

    // ============== التصنيفات ==============
    suspend fun loadLiveCategories(
        server: String, user: String, pass: String
    ): Map<String, String> = withContext(Dispatchers.IO) {
        loadCategories(server, user, pass, "get_live_categories")
    }

    suspend fun loadVodCategories(
        server: String, user: String, pass: String
    ): Map<String, String> = withContext(Dispatchers.IO) {
        loadCategories(server, user, pass, "get_vod_categories")
    }

    suspend fun loadSeriesCategories(
        server: String, user: String, pass: String
    ): Map<String, String> = withContext(Dispatchers.IO) {
        loadCategories(server, user, pass, "get_series_categories")
    }

    private suspend fun loadCategories(
        server: String, user: String, pass: String, action: String
    ): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            XtreamLogger.log("API", "📂 جاري تحميل التصنيفات: $action")
            val s = normalizeServer(server)
            val url = "$s/player_api.php?username=$user&password=$pass&action=$action"
            val body = HttpClientProvider.fetchText(url).getOrNull() ?: return emptyMap()
            if (body.isBlank() || body.trimStart().startsWith("<")) return emptyMap()
            val arr = JSONArray(body)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.optString("category_id")
                val rawName = obj.optString("category_name", "").trim()
                val name = if (rawName.isBlank() || rawName == "null") "باقة $id" else rawName
                if (id.isNotBlank()) map[id] = name
            }
            XtreamLogger.log("API", "✅ تم تحميل ${map.size} تصنيف")
        } catch (e: Exception) {
            XtreamLogger.logException("API", "تحميل التصنيفات", e)
        }
        return map
    }

    // ============== كشف ذكي للتصنيفات ==============
    private fun detectSmartCategories(
        rawChannels: List<RawChannel>, categoriesMap: Map<String, String>
    ): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val grouped = rawChannels.groupBy { it.categoryId }
        grouped.forEach { (catId, channels) ->
            val originalName = categoriesMap[catId] ?: "باقة $catId"
            if (!originalName.startsWith("باقة ")) {
                result[catId] = originalName
                return@forEach
            }
            val detected = CategoriesManager.detectTypeFromChannelNames(channels.map { it.name })
            result[catId] = if (detected.isNotEmpty()) "$originalName - $detected" else originalName
        }
        return result
    }

    private fun detectSmartCategoriesForMovies(
        raw: List<RawMovie>, categoriesMap: Map<String, String>
    ): Map<String, String> {
        val result = mutableMapOf<String, String>()
        raw.groupBy { it.categoryId }.forEach { (catId, list) ->
            val originalName = categoriesMap[catId] ?: "باقة $catId"
            if (!originalName.startsWith("باقة ")) {
                result[catId] = originalName
                return@forEach
            }
            val detected = CategoriesManager.detectTypeFromChannelNames(list.map { it.name })
            result[catId] = if (detected.isNotEmpty()) "$originalName - $detected" else originalName
        }
        return result
    }

    private fun detectSmartCategoriesForSeries(
        raw: List<RawSeries>, categoriesMap: Map<String, String>
    ): Map<String, String> {
        val result = mutableMapOf<String, String>()
        raw.groupBy { it.categoryId }.forEach { (catId, list) ->
            val originalName = categoriesMap[catId] ?: "باقة $catId"
            if (!originalName.startsWith("باقة ")) {
                result[catId] = originalName
                return@forEach
            }
            val detected = CategoriesManager.detectTypeFromChannelNames(list.map { it.name })
            result[catId] = if (detected.isNotEmpty()) "$originalName - $detected" else originalName
        }
        return result
    }

    // ============== القنوات المباشرة ==============
    suspend fun loadLiveStreams(
        server: String, user: String, pass: String,
        onProgress: (loaded: Int) -> Unit = {}
    ): List<Channel> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        XtreamLogger.log("LIVE", "📺 بدء تحميل القنوات المباشرة...")
        val channels = mutableListOf<Channel>()
        try {
            val s = normalizeServer(server)
            val categoriesMap = loadLiveCategories(server, user, pass)

            val url = "$s/player_api.php?username=$user&password=$pass&action=get_live_streams"
            val body = HttpClientProvider.fetchText(url).getOrNull()
                ?: run {
                    XtreamLogger.log("LIVE", "❌ لم يتم استلام استجابة من السيرفر")
                    return@withContext emptyList()
                }

            if (body.isBlank() || body.trimStart().startsWith("<")) {
                XtreamLogger.log("LIVE", "❌ استجابة غير صالحة (HTML أو فارغة)")
                return@withContext emptyList()
            }

            val arr = JSONArray(body)
            XtreamLogger.log("LIVE", "📥 تم استلام ${arr.length()} عنصر خام")

            val rawChannels = mutableListOf<RawChannel>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                rawChannels.add(RawChannel(
                    id = obj.optString("stream_id"),
                    name = obj.optString("name"),
                    icon = obj.optString("stream_icon"),
                    categoryId = obj.optString("category_id")
                ))
                if (i % 500 == 0 && i > 0) {
                    onProgress(i)
                    XtreamLogger.log("LIVE", "⏳ تم تحليل $i قناة...")
                }
            }
            onProgress(rawChannels.size)

            val smartCategories = detectSmartCategories(rawChannels, categoriesMap)
            XtreamLogger.log("LIVE", "🏷️ تم كشف ${smartCategories.size} تصنيف ذكي")

            val ext = if (preferredFormat == "m3u8") "m3u8" else "ts"
            rawChannels.forEach { raw ->
                val groupName = smartCategories[raw.categoryId] ?: "باقة ${raw.categoryId}"
                channels.add(Channel(
                    id = raw.id,
                    name = raw.name,
                    logo = raw.icon,
                    url = "$s/live/$user/$pass/${raw.id}.$ext",
                    group = groupName
                ))
            }

            val elapsed = System.currentTimeMillis() - startTime
            XtreamLogger.log("LIVE", "✅ اكتمل تحميل ${channels.size} قناة في ${elapsed}ms")
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            XtreamLogger.logException("LIVE", "تحميل القنوات", e)
            XtreamLogger.log("LIVE", "❌ فشل بعد ${elapsed}ms: ${e.message}")
        }
        channels
    }
    
    // ============== الأفلام ==============
    suspend fun loadVodStreams(
        server: String, user: String, pass: String,
        onProgress: (loaded: Int) -> Unit = {}
    ): List<Movie> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        XtreamLogger.log("VOD", "🎬 بدء تحميل الأفلام...")
        val movies = mutableListOf<Movie>()
        try {
            val s = normalizeServer(server)
            val categoriesMap = loadVodCategories(server, user, pass)

            val url = "$s/player_api.php?username=$user&password=$pass&action=get_vod_streams"
            val body = HttpClientProvider.fetchText(url).getOrNull()
                ?: run {
                    XtreamLogger.log("VOD", "❌ لم يتم استلام استجابة")
                    return@withContext emptyList()
                }

            if (body.isBlank() || body.trimStart().startsWith("<")) {
                XtreamLogger.log("VOD", "❌ استجابة غير صالحة")
                return@withContext emptyList()
            }

            val arr = JSONArray(body)
            XtreamLogger.log("VOD", "📥 تم استلام ${arr.length()} فيلم خام")

            val rawMovies = mutableListOf<RawMovie>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                rawMovies.add(RawMovie(
                    id = obj.optString("stream_id"),
                    name = obj.optString("name"),
                    poster = obj.optString("stream_icon"),
                    categoryId = obj.optString("category_id"),
                    ext = obj.optString("container_extension", "mp4")
                ))
                if (i % 500 == 0 && i > 0) {
                    onProgress(i)
                    XtreamLogger.log("VOD", "⏳ تم تحليل $i فيلم...")
                }
            }
            onProgress(rawMovies.size)

            val smartCategories = detectSmartCategoriesForMovies(rawMovies, categoriesMap)

            rawMovies.forEach { raw ->
                val categoryName = smartCategories[raw.categoryId] ?: "باقة ${raw.categoryId}"
                movies.add(Movie(
                    id = raw.id,
                    name = raw.name,
                    poster = raw.poster,
                    url = "$s/movie/$user/$pass/${raw.id}.${raw.ext}",
                    category = categoryName
                ))
            }

            val elapsed = System.currentTimeMillis() - startTime
            XtreamLogger.log("VOD", "✅ اكتمل تحميل ${movies.size} فيلم في ${elapsed}ms")
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            XtreamLogger.logException("VOD", "تحميل الأفلام", e)
            XtreamLogger.log("VOD", "❌ فشل بعد ${elapsed}ms: ${e.message}")
        }
        movies
    }

    // ============== المسلسلات ==============
    suspend fun loadSeries(
        server: String, user: String, pass: String,
        onProgress: (loaded: Int) -> Unit = {}
    ): List<Series> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        XtreamLogger.log("SERIES", "📼 بدء تحميل المسلسلات...")
        val series = mutableListOf<Series>()
        try {
            val s = normalizeServer(server)
            val categoriesMap = loadSeriesCategories(server, user, pass)

            val url = "$s/player_api.php?username=$user&password=$pass&action=get_series"
            val body = HttpClientProvider.fetchText(url).getOrNull()
                ?: run {
                    XtreamLogger.log("SERIES", "❌ لم يتم استلام استجابة")
                    return@withContext emptyList()
                }

            if (body.isBlank() || body.trimStart().startsWith("<")) {
                XtreamLogger.log("SERIES", "❌ استجابة غير صالحة")
                return@withContext emptyList()
            }

            val arr = JSONArray(body)
            XtreamLogger.log("SERIES", "📥 تم استلام ${arr.length()} مسلسل خام")

            val rawSeries = mutableListOf<RawSeries>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                rawSeries.add(RawSeries(
                    id = obj.optString("series_id"),
                    name = obj.optString("name"),
                    cover = obj.optString("cover"),
                    categoryId = obj.optString("category_id")
                ))
                if (i % 500 == 0 && i > 0) {
                    onProgress(i)
                    XtreamLogger.log("SERIES", "⏳ تم تحليل $i مسلسل...")
                }
            }
            onProgress(rawSeries.size)

            val smartCategories = detectSmartCategoriesForSeries(rawSeries, categoriesMap)

            rawSeries.forEach { raw ->
                val categoryName = smartCategories[raw.categoryId] ?: "باقة ${raw.categoryId}"
                series.add(Series(
                    id = raw.id,
                    name = raw.name,
                    poster = raw.cover,
                    category = categoryName
                ))
            }

            val elapsed = System.currentTimeMillis() - startTime
            XtreamLogger.log("SERIES", "✅ اكتمل تحميل ${series.size} مسلسل في ${elapsed}ms")
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            XtreamLogger.logException("SERIES", "تحميل المسلسلات", e)
            XtreamLogger.log("SERIES", "❌ فشل بعد ${elapsed}ms: ${e.message}")
        }
        series
    }

    // ============== الحلقات ==============
    suspend fun loadEpisodes(
        server: String, user: String, pass: String, seriesId: String
    ): List<Episode> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        XtreamLogger.log("EPISODES", "🎞️ جاري تحميل حلقات المسلسل #$seriesId")
        val episodes = mutableListOf<Episode>()
        try {
            val s = normalizeServer(server)
            val url = "$s/player_api.php?username=$user&password=$pass&action=get_series_info&series_id=$seriesId"
            val body = HttpClientProvider.fetchText(url).getOrNull()
                ?: return@withContext emptyList()

            if (body.isBlank() || body.trimStart().startsWith("<")) {
                XtreamLogger.log("EPISODES", "❌ استجابة غير صالحة")
                return@withContext emptyList()
            }

            val json = JSONObject(body)
            val epsObj = json.optJSONObject("episodes") ?: return@withContext episodes
            val seasonKeys = epsObj.keys()
            while (seasonKeys.hasNext()) {
                val season = seasonKeys.next()
                val arr = epsObj.getJSONArray(season)
                for (i in 0 until arr.length()) {
                    val ep = arr.getJSONObject(i)
                    val epId = ep.optString("id")
                    val epNum = ep.optInt("episode_num", i + 1)
                    val title = ep.optString("title", "الحلقة $epNum")
                    val ext = ep.optString("container_extension", "mp4")
                    episodes.add(Episode(
                        id = epId,
                        title = title,
                        season = season.toIntOrNull() ?: 1,
                        episode = epNum,
                        url = "$s/series/$user/$pass/$epId.$ext",
                        poster = ""
                    ))
                }
            }

            val elapsed = System.currentTimeMillis() - startTime
            XtreamLogger.log("EPISODES", "✅ اكتمل تحميل ${episodes.size} حلقة في ${elapsed}ms")
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            XtreamLogger.logException("EPISODES", "تحميل الحلقات", e)
            XtreamLogger.log("EPISODES", "❌ فشل بعد ${elapsed}ms: ${e.message}")
        }
        episodes
    }

    // ============== أدوات ==============
    private fun normalizeServer(server: String): String {
        var s = server.trim().trimEnd('/')
        if (!s.startsWith("http://") && !s.startsWith("https://")) {
            s = "$preferredProtocol://$s"
        }
        return s
    }
}
