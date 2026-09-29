package com.stitten.stitteniptv.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object XtreamApi {

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
                json.optJSONObject("user_info")?.optString("auth") == "1"
            } catch (e: Exception) {
                false
            }
        }

    suspend fun loadLiveStreams(server: String, user: String, pass: String): List<Channel> =
        withContext(Dispatchers.IO) {
            val channels = mutableListOf<Channel>()
            try {
                val s = normalizeServer(server)
                val url = "$s/player_api.php?username=$user&password=$pass&action=get_live_streams"
                val body = HttpClientProvider.fetchText(url).getOrNull() ?: return@withContext emptyList()
                if (body.isBlank() || body.trimStart().startsWith("<")) return@withContext emptyList()
                val arr = JSONArray(body)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val streamId = obj.optString("stream_id")
                    val name = obj.optString("name")
                    val icon = obj.optString("stream_icon")
                    val categoryId = obj.optString("category_id")
                    val streamUrl = "$s/live/$user/$pass/$streamId.ts"
                    channels.add(
                        Channel(
                            id = streamId,
                            name = name,
                            logo = icon,
                            url = streamUrl,
                            group = categoryId
                        )
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
                    movies.add(
                        Movie(
                            id = streamId,
                            name = name,
                            poster = poster,
                            url = url2,
                            category = obj.optString("category_id")
                        )
                    )
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
                            id = obj.optString("series_id"),
                            name = obj.optString("name"),
                            poster = obj.optString("cover"),
                            category = obj.optString("category_id")
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
            s = "http://$s"
        }
        return s
    }
}
