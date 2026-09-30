package com.stitten.stitteniptv.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class CategoryInfo(
    val id: String,
    val name: String,
    val type: String // "live", "vod", "series"
)

object XtreamApi {

    @Volatile
    private var preferredProtocol: String = "http"

    @Volatile
    private var preferredFormat: String = "ts"

    fun setPreferredProtocol(useHttps: Boolean) {
        preferredProtocol = if (useHttps) "https" else "http"
    }

    fun setPreferredFormat(format: String) {
        preferredFormat = if (format == "hls" || format == "m3u8") "m3u8" else "ts"
    }

    suspend fun validate(server: String, user: String, pass: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val cleanServer = normalizeServer(server)
                val url = "$cleanServer/player_api.php?username=$user&password=$pass"
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

    // ============== التصنيفات ==============
    suspend fun loadLiveCategories(server: String, user: String, pass: String): List<CategoryInfo> =
        withContext(Dispatchers.IO) {
            loadCategories(server, user, pass, "get_live_categories", "live")
        }

    suspend fun loadVodCategories(server: String, user: String, pass: String): List<CategoryInfo> =
        withContext(Dispatchers.IO) {
            loadCategories(server, user, pass, "get_vod_categories", "vod")
        }

    suspend fun loadSeriesCategories(server: String, user: String, pass: String): List<CategoryInfo> =
        withContext(Dispatchers.IO) {
            loadCategories(server, user, pass, "get_series_categories", "series")
        }

    private suspend fun loadCategories(
        server: String, user: String, pass: String,
        action: String, type: String
    ): List<CategoryInfo> = withContext(Dispatchers.IO) {
        val categories = mutableListOf<CategoryInfo>()
        try {
            val s = normalizeServer(server)
            val url = "$s/player_api.php?username=$user&password=$pass&action=$action"
            val body = HttpClientProvider.fetchText(url).getOrNull() ?: return@withContext emptyList()
            if (body.isBlank() || body.trimStart().startsWith("<")) return@withContext emptyList()
            val arr = JSONArray(body)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.optString("category_id")
                val name = obj.optString("category_name", "تصنيف $id")
                if (id.isNotBlank()) {
                    categories.add(CategoryInfo(id, name, type))
                }
            }
        } catch (e: Exception) { }
        categories
    }
    
// ============== قنوات تصنيف معين ==============
suspend fun loadLiveStreamsByCategory(
    server: String, user: String, pass: String, categoryId: String
): List<Channel> = withContext(Dispatchers.IO) {
    val channels = mutableListOf<Channel>()
    try {
        val s = normalizeServer(server)
        val url = "$s/player_api.php?username=$user&password=$pass&action=get_live_streams&category_id=$categoryId"
        val body = HttpClientProvider.fetchText(url).getOrNull() ?: return@withContext emptyList()
        if (body.isBlank() || body.trimStart().startsWith("<")) return@withContext emptyList()
        val arr = JSONArray(body)
        val ext = if (preferredFormat == "m3u8") "m3u8" else "ts"
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val streamId = obj.optString("stream_id")
            val name = obj.optString("name")
            val icon = obj.optString("stream_icon")
            val categoryIdFromApi = obj.optString("category_id", categoryId)
            val streamUrl = "$s/live/$user/$pass/$streamId.$ext"
            channels.add(
                Channel(
                    id = streamId,
                    name = name,
                    logo = icon,
                    url = streamUrl,
                    group = categoryIdFromApi
                )
            )
        }
    } catch (e: Exception) { }
    channels
}

suspend fun loadVodStreamsByCategory(
    server: String, user: String, pass: String, categoryId: String
): List<Movie> = withContext(Dispatchers.IO) {
    val movies = mutableListOf<Movie>()
    try {
        val s = normalizeServer(server)
        val url = "$s/player_api.php?username=$user&password=$pass&action=get_vod_streams&category_id=$categoryId"
        val body = HttpClientProvider.fetchText(url).getOrNull() ?: return@withContext emptyList()
        if (body.isBlank() || body.trimStart().startsWith("<")) return@withContext emptyList()
        val arr = JSONArray(body)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val streamId = obj.optString("stream_id")
            val name = obj.optString("name")
            val poster = obj.optString("stream_icon")
            val ext = obj.optString("container_extension", "mp4")
            val url2 = "$s/movie/$user/$pass/$streamId.$ext"
            movies.add(
                Movie(
                    id = streamId,
                    name = name,
                    poster = poster,
                    url = url2,
                    category = obj.optString("category_id", categoryId)
                )
            )
        }
    } catch (e: Exception) { }
    movies
}

suspend fun loadSeriesByCategory(
    server: String, user: String, pass: String, categoryId: String
): List<Series> = withContext(Dispatchers.IO) {
    val series = mutableListOf<Series>()
    try {
        val s = normalizeServer(server)
        val url = "$s/player_api.php?username=$user&password=$pass&action=get_series&category_id=$categoryId"
        val body = HttpClientProvider.fetchText(url).getOrNull() ?: return@withContext emptyList()
        if (body.isBlank() || body.trimStart().startsWith("<")) return@withContext emptyList()
        val arr = JSONArray(body)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            series.add(
                Series(
                    id = obj.optString("series_id"),
                    name = obj.optString("name"),
                    poster = obj.optString("cover"),
                    category = obj.optString("category_id", categoryId)
                )
            )
        }
    } catch (e: Exception) { }
    series
}

    // ============== التحميل الكامل (احتياطي) ==============
    suspend fun loadLiveStreams(server: String, user: String, pass: String): List<Channel> =
        withContext(Dispatchers.IO) {
            val channels = mutableListOf<Channel>()
            try {
                val s = normalizeServer(server)
                val url = "$s/player_api.php?username=$user&password=$pass&action=get_live_streams"
                val body = HttpClientProvider.fetchText(url).getOrNull() ?: return@withContext emptyList()
                if (body.isBlank() || body.trimStart().startsWith("<")) return@withContext emptyList()
                val arr = JSONArray(body)
                val ext = if (preferredFormat == "m3u8") "m3u8" else "ts"
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val streamId = obj.optString("stream_id")
                    val name = obj.optString("name")
                    val icon = obj.optString("stream_icon")
                    val categoryId = obj.optString("category_id")
                    val streamUrl = "$s/live/$user/$pass/$streamId.$ext"
                    channels.add(
                        Channel(streamId, name, icon, streamUrl, categoryId)
                    )
                }
            } catch (e: Exception) { }
            channels
        }

    suspend fun loadVodStreams(server: String, user: String, pass: String): List<Movie> =
        withContext(Dispatchers.IO) {
            val movies = mutableListOf<Movie>()
            try {
                val s = normalizeServer(server)
                val url = "$s/player_api.php?username=$user&password=$pass&action=get_vod_streams"
                val body = HttpClientProvider.fetchText(url).getOrNull() ?: return@withContext emptyList()
                if (body.isBlank() || body.trimStart().startsWith("<")) return@withContext emptyList()
                val arr = JSONArray(body)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val streamId = obj.optString("stream_id")
                    val name = obj.optString("name")
                    val poster = obj.optString("stream_icon")
                    val ext = obj.optString("container_extension", "mp4")
                    val url2 = "$s/movie/$user/$pass/$streamId.$ext"
                    movies.add(Movie(streamId, name, poster, url2, obj.optString("category_id")))
                }
            } catch (e: Exception) { }
            movies
        }

    suspend fun loadSeries(server: String, user: String, pass: String): List<Series> =
        withContext(Dispatchers.IO) {
            val series = mutableListOf<Series>()
            try {
                val s = normalizeServer(server)
                val url = "$s/player_api.php?username=$user&password=$pass&action=get_series"
                val body = HttpClientProvider.fetchText(url).getOrNull() ?: return@withContext emptyList()
                if (body.isBlank() || body.trimStart().startsWith("<")) return@withContext emptyList()
                val arr = JSONArray(body)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    series.add(
                        Series(
                            obj.optString("series_id"),
                            obj.optString("name"),
                            obj.optString("cover"),
                            obj.optString("category_id")
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
                            epId, title, season.toIntOrNull() ?: 1, epNum, epUrl, ""
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
