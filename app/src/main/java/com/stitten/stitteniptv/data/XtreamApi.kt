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
        } catch (e: Exception) { }
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

    // ============== القنوات ==============
    suspend fun loadLiveStreams(
        server: String, user: String, pass: String,
        onProgress: (loaded: Int) -> Unit = {}
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
                rawChannels.add(RawChannel(
                    id = obj.optString("stream_id"),
                    name = obj.optString("name"),
                    icon = obj.optString("stream_icon"),
                    categoryId = obj.optString("category_id")
                ))
                if (i % 500 == 0) onProgress(i)
            }
            onProgress(rawChannels.size)

            val smartCategories = detectSmartCategories(rawChannels, categoriesMap)
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
        } catch (e: Exception) { }
        channels
    }
