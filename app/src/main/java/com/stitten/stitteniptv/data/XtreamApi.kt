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

    private data class RawChannel(
        val id: String,
        val name: String,
        val icon: String,
        val categoryId: String
    )

    private data class RawMovie(
        val id: String,
        val name: String,
        val poster: String,
        val categoryId: String,
        val ext: String
    )

    private data class RawSeries(
        val id: String,
        val name: String,
        val cover: String,
        val categoryId: String
    )

    suspend fun validate(server: String, user: String, pass: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val s = normalizeServer(server)
                val url = "$s/player_api.php?username=$user&password=$pass"
                val result = HttpClientProvider.fetchText(url)
                if (result.isFailure) return@withContext false
                val body = result.getOrNull() ?: return@withContext false
                if (body.isBlank() || body.trimStart().startsWith("<")) return@withContext false
                val json = JSONObject(body)
                val auth = json.optJSONObject("user_info")?.opt("auth")
                val authString = when (auth) {
                    is Number -> auth.toInt().toString()
                    is String -> auth
                    else -> ""
                }
                authString == "1"
            } catch (e: Exception) { false }
        }

    // ============== التصنيفات (suspend) ==============
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

    // 🔧 الدالة أصبحت suspend
    private suspend fun loadCategories(
        server: String, user: String, pass: String, action: String
    ): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            val s = normalizeServer(server)
            val url = "$s/player_api.php?username=$user&password=$pass&action=$action"
            val body = HttpClientProvider.fetchText(url).getOrNull() ?: return emptyMap()
            if (body.isBlank() || body.trimStart().startsWith("<")) return emptyMap()
            val arr = JSONArray(body)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.optString("category_id")
                val rawName = obj.optString("category_name", "").trim()
                val name = if (rawName.isBlank() || rawName == "null") {
                    "باقة $id"
                } else {
                    rawName
                }
                if (id.isNotBlank()) {
                    map[id] = name
                }
            }
        } catch (e: Exception) { }
        return map
    }

    // ============== الكشف الذكي ==============
    private fun detectSmartCategories(
        rawChannels: List<RawChannel>,
        categoriesMap: Map<String, String>
    ): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val groupedByCategory = rawChannels.groupBy { it.categoryId }

        groupedByCategory.forEach { (categoryId, channels) ->
            val originalName = categoriesMap[categoryId] ?: "باقة $categoryId"

            if (!originalName.startsWith("باقة ")) {
                result[categoryId] = originalName
                return@forEach
            }

            val channelNames = channels.map { it.name }
            val detectedType = CategoriesManager.detectTypeFromChannelNames(channelNames)

            val smartName = if (detectedType.isNotEmpty()) {
                "$originalName - $detectedType"
            } else {
                originalName
            }

            result[categoryId] = smartName
        }

        return result
    }

    private fun detectSmartCategoriesForMovies(
        rawMovies: List<RawMovie>,
        categoriesMap: Map<String, String>
    ): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val groupedByCategory = rawMovies.groupBy { it.categoryId }

        groupedByCategory.forEach { (categoryId, movies) ->
            val originalName = categoriesMap[categoryId] ?: "باقة $categoryId"

            if (!originalName.startsWith("باقة ")) {
                result[categoryId] = originalName
                return@forEach
            }

            val names = movies.map { it.name }
            val detectedType = CategoriesManager.detectTypeFromChannelNames(names)

            val smartName = if (detectedType.isNotEmpty()) {
                "$originalName - $detectedType"
            } else {
                originalName
            }

            result[categoryId] = smartName
        }

        return result
    }

    private fun detectSmartCategoriesForSeries(
        rawSeries: List<RawSeries>,
        categoriesMap: Map<String, String>
    ): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val groupedByCategory = rawSeries.groupBy { it.categoryId }

        groupedByCategory.forEach { (categoryId, seriesList) ->
            val originalName = categoriesMap[categoryId] ?: "باقة $categoryId"

            if (!originalName.startsWith("باقة ")) {
                result[categoryId] = originalName
                return@forEach
            }

            val names = seriesList.map { it.name }
            val detectedType = CategoriesManager.detectTypeFromChannelNames(names)

            val smartName = if (detectedType.isNotEmpty()) {
                "$originalName - $detectedType"
            } else {
                originalName
            }

            result[categoryId] = smartName
        }

        return result
    }

    // ============== القنوات المباشرة ==============
    suspend fun loadLiveStreams(
        server: String, user: String, pass: String
    ): List<Channel> = withContext(Dispatchers.IO) {
        val channels = mutableListOf<Channel>()
        try {
            val s = normalizeServer(server)
            val categoriesMap = loadLiveCategories(server, user, pass)

            val url = "$s/player_api.php?username=$user&password=$pass&action=get_live_streams"
            val body = HttpClientProvider.fetchText(url).getOrNull() ?: return@withContext emptyList()
            if (body.isBlank() || body.trimStart().startsWith("<")) return@withContext emptyList()
            val arr = JSONArray(body)

            val rawChannels = mutableListOf<RawChannel>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                rawChannels.add(
                    RawChannel(
                        id = obj.optString("stream_id"),
                        name = obj.optString("name"),
                        icon = obj.optString("stream_icon"),
                        categoryId = obj.optString("category_id")
                    )
                )
            }

            val smartCategories = detectSmartCategories(rawChannels, categoriesMap)

            val ext = if (preferredFormat == "m3u8") "m3u8" else "ts"
            rawChannels.forEach { raw ->
                val groupName = smartCategories[raw.categoryId] ?: "باقة ${raw.categoryId}"
                val streamUrl = "$s/live/$user/$pass/${raw.id}.$ext"
                channels.add(
                    Channel(
                        id = raw.id,
                        name = raw.name,
                        logo = raw.icon,
                        url = streamUrl,
                        group = groupName
                    )
                )
            }
        } catch (e: Exception) { }
        channels
    }
    
    // ============== الأفلام ==============
    suspend fun loadVodStreams(
        server: String, user: String, pass: String
    ): List<Movie> = withContext(Dispatchers.IO) {
        val movies = mutableListOf<Movie>()
        try {
            val s = normalizeServer(server)
            val categoriesMap = loadVodCategories(server, user, pass)

            val url = "$s/player_api.php?username=$user&password=$pass&action=get_vod_streams"
            val body = HttpClientProvider.fetchText(url).getOrNull() ?: return@withContext emptyList()
            if (body.isBlank() || body.trimStart().startsWith("<")) return@withContext emptyList()
            val arr = JSONArray(body)

            val rawMovies = mutableListOf<RawMovie>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                rawMovies.add(
                    RawMovie(
                        id = obj.optString("stream_id"),
                        name = obj.optString("name"),
                        poster = obj.optString("stream_icon"),
                        categoryId = obj.optString("category_id"),
                        ext = obj.optString("container_extension", "mp4")
                    )
                )
            }

            val smartCategories = detectSmartCategoriesForMovies(rawMovies, categoriesMap)

            rawMovies.forEach { raw ->
                val categoryName = smartCategories[raw.categoryId] ?: "باقة ${raw.categoryId}"
                val url2 = "$s/movie/$user/$pass/${raw.id}.${raw.ext}"
                movies.add(
                    Movie(
                        id = raw.id,
                        name = raw.name,
                        poster = raw.poster,
                        url = url2,
                        category = categoryName
                    )
                )
            }
        } catch (e: Exception) { }
        movies
    }

    // ============== المسلسلات ==============
    suspend fun loadSeries(
        server: String, user: String, pass: String
    ): List<Series> = withContext(Dispatchers.IO) {
        val series = mutableListOf<Series>()
        try {
            val s = normalizeServer(server)
            val categoriesMap = loadSeriesCategories(server, user, pass)

            val url = "$s/player_api.php?username=$user&password=$pass&action=get_series"
            val body = HttpClientProvider.fetchText(url).getOrNull() ?: return@withContext emptyList()
            if (body.isBlank() || body.trimStart().startsWith("<")) return@withContext emptyList()
            val arr = JSONArray(body)

            val rawSeries = mutableListOf<RawSeries>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                rawSeries.add(
                    RawSeries(
                        id = obj.optString("series_id"),
                        name = obj.optString("name"),
                        cover = obj.optString("cover"),
                        categoryId = obj.optString("category_id")
                    )
                )
            }

            val smartCategories = detectSmartCategoriesForSeries(rawSeries, categoriesMap)

            rawSeries.forEach { raw ->
                val categoryName = smartCategories[raw.categoryId] ?: "باقة ${raw.categoryId}"
                series.add(
                    Series(
                        id = raw.id,
                        name = raw.name,
                        poster = raw.cover,
                        category = categoryName
                    )
                )
            }
        } catch (e: Exception) { }
        series
    }

    suspend fun loadEpisodes(
        server: String, user: String, pass: String, seriesId: String
    ): List<Episode> = withContext(Dispatchers.IO) {
        val episodes = mutableListOf<Episode>()
        try {
            val s = normalizeServer(server)
            val url = "$s/player_api.php?username=$user&password=$pass&action=get_series_info&series_id=$seriesId"
            val body = HttpClientProvider.fetchText(url).getOrNull() ?: return@withContext emptyList()
            if (body.isBlank() || body.trimStart().startsWith("<")) return@withContext emptyList()
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
                    val epUrl = "$s/series/$user/$pass/$epId.$ext"
                    episodes.add(
                        Episode(
                            id = epId,
                            title = title,
                            season = season.toIntOrNull() ?: 1,
                            episode = epNum,
                            url = epUrl,
                            poster = ""
                        )
                    )
                }
            }
        } catch (e: Exception) { }
        episodes
    }

    private fun normalizeServer(server: String): String {
        var s = server.trim().trimEnd('/')
        if (!s.startsWith("http://") && !s.startsWith("https://")) {
            s = "$preferredProtocol://$s"
        }
        return s
    }
}
