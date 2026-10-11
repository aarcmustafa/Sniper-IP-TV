package com.stitten.stitteniptv.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader

/**
 * نتيجة تحليل M3U مُصنّفة.
 */
data class M3uParseResult(
    val channels: List<Channel> = emptyList(),
    val movies: List<Movie> = emptyList(),
    val series: List<Series> = emptyList(),
    val seriesEpisodes: Map<String, List<Episode>> = emptyMap(),
)

object M3uParser {

    // ═══════════════════════════════════════════════════════════
    //  التحميل العادي (ملفات صغيرة < 20 MB)
    // ═══════════════════════════════════════════════════════════
    suspend fun loadFromUrl(url: String): List<Channel> = withContext(Dispatchers.IO) {
        try {
            val result = HttpClientProvider.fetchText(url)
            if (result.isFailure) return@withContext emptyList()
            val content = result.getOrNull() ?: return@withContext emptyList()
            if (!content.contains("#EXTM3U") && !content.contains("#EXTINF")) {
                return@withContext emptyList()
            }
            parse(content)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ✅ جديد: تحميل مُصنّف
    suspend fun loadFromUrlStructured(url: String): M3uParseResult =
        withContext(Dispatchers.IO) {
            try {
                val result = HttpClientProvider.fetchText(url)
                if (result.isFailure) return@withContext M3uParseResult()
                val content = result.getOrNull() ?: return@withContext M3uParseResult()
                if (!content.contains("#EXTM3U") && !content.contains("#EXTINF")) {
                    return@withContext M3uParseResult()
                }
                parseStructured(content)
            } catch (e: Exception) {
                M3uParseResult()
            }
        }

    // ═══════════════════════════════════════════════════════════
    //  التحميل الانسيابي (ملفات ضخمة > 20 MB)
    // ═══════════════════════════════════════════════════════════
    suspend fun loadFromUrlStreaming(
        ctx: Context,
        url: String,
        onProgress: (downloadedMb: Long, totalMb: Long) -> Unit = { _, _ -> }
    ): List<Channel> = withContext(Dispatchers.IO) {
        val tempFile = File(ctx.cacheDir, "temp_playlist.m3u")

        try {
            val downloaded = downloadToFile(url, tempFile, onProgress)
            if (!downloaded) return@withContext emptyList()

            val channels = parseFileStreaming(tempFile)
            tempFile.delete()
            channels
        } catch (e: Exception) {
            tempFile.delete()
            emptyList()
        }
    }

    // ✅ جديد: تحميل انسيابي مُصنّف
    suspend fun loadFromUrlStreamingStructured(
        ctx: Context,
        url: String,
        onProgress: (downloadedMb: Long, totalMb: Long) -> Unit = { _, _ -> }
    ): M3uParseResult = withContext(Dispatchers.IO) {
        val tempFile = File(ctx.cacheDir, "temp_playlist.m3u")

        try {
            val downloaded = downloadToFile(url, tempFile, onProgress)
            if (!downloaded) return@withContext M3uParseResult()

            val result = parseFileStreamingStructured(tempFile)
            tempFile.delete()
            result
        } catch (e: Exception) {
            tempFile.delete()
            M3uParseResult()
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  التحميل إلى ملف (Disk I/O)
    // ═══════════════════════════════════════════════════════════
    private fun downloadToFile(
        url: String,
        target: File,
        onProgress: (Long, Long) -> Unit
    ): Boolean {
        return try {
            val request = okhttp3.Request.Builder()
                .url(url)
                .header("User-Agent", HttpClientProvider.getUserAgent())
                .build()

            val response = HttpClientProvider.trustAllClient
                .newCall(request).execute()

            if (!response.isSuccessful) {
                response.close()
                return false
            }

            val body = response.body ?: run {
                response.close()
                return false
            }

            val contentLength = body.contentLength()
            val totalMb = if (contentLength > 0) contentLength / (1024 * 1024) else -1L

            body.byteStream().use { input ->
                FileOutputStream(target).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    var totalRead = 0L
                    var lastReported = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead

                        val currentMb = totalRead / (1024 * 1024)
                        if (currentMb > lastReported) {
                            lastReported = currentMb
                            onProgress(currentMb, totalMb)
                        }
                    }
                    output.flush()
                }
            }

            response.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  تحميل من محتوى نصي
    // ═══════════════════════════════════════════════════════════
    suspend fun loadFromContent(content: String): List<Channel> =
        withContext(Dispatchers.IO) { parse(content) }

    // ✅ جديد: محتوى مُصنّف
    suspend fun loadFromContentStructured(content: String): M3uParseResult =
        withContext(Dispatchers.IO) { parseStructured(content) }
        
    // ═══════════════════════════════════════════════════════════
    //  تحليل نص عادي (قنوات فقط)
    // ═══════════════════════════════════════════════════════════
    private fun parse(content: String): List<Channel> {
        if (content.isBlank()) return emptyList()
        val channels = mutableListOf<Channel>()
        val lines = content.lines()
        var pendingName = ""
        var pendingLogo = ""
        var pendingGroup = ""
        var index = 0

        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("#EXTINF") -> {
                    pendingName = trimmed.substringAfterLast(",", "").trim()
                    pendingLogo = extractAttribute(trimmed, "tvg-logo")
                    pendingGroup = extractAttribute(trimmed, "group-title")
                }
                trimmed.isNotEmpty() && !trimmed.startsWith("#") -> {
                    if (pendingName.isNotEmpty()) {
                        channels.add(
                            Channel(
                                id = "ch_${index++}",
                                name = pendingName.ifEmpty { "Channel $index" },
                                logo = pendingLogo,
                                url = trimmed,
                                group = pendingGroup.ifEmpty { "عام" }
                            )
                        )
                        pendingName = ""
                        pendingLogo = ""
                        pendingGroup = ""
                    }
                }
            }
        }
        return channels
    }

    // ═══════════════════════════════════════════════════════════
    //  ✅ تحليل نص مُصنّف (قنوات + أفلام + مسلسلات)
    // ═══════════════════════════════════════════════════════════
    private fun parseStructured(content: String): M3uParseResult {
        if (content.isBlank()) return M3uParseResult()
        val lines = content.lines()
        return buildStructuredFromLines(lines)
    }

    // ═══════════════════════════════════════════════════════════
    //  تحليل ملف سطراً بسطر (قنوات فقط)
    // ═══════════════════════════════════════════════════════════
    private fun parseFileStreaming(file: File): List<Channel> {
        if (!file.exists() || file.length() == 0L) return emptyList()

        val channels = mutableListOf<Channel>()
        var pendingName = ""
        var pendingLogo = ""
        var pendingGroup = ""
        var index = 0

        BufferedReader(InputStreamReader(file.inputStream(), "UTF-8"), 64 * 1024).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val trimmed = line?.trim() ?: continue

                when {
                    trimmed.startsWith("#EXTINF") -> {
                        pendingName = trimmed.substringAfterLast(",", "").trim()
                        pendingLogo = extractAttribute(trimmed, "tvg-logo")
                        pendingGroup = extractAttribute(trimmed, "group-title")
                    }
                    trimmed.isNotEmpty() && !trimmed.startsWith("#") -> {
                        if (pendingName.isNotEmpty()) {
                            channels.add(
                                Channel(
                                    id = "ch_${index++}",
                                    name = pendingName.ifEmpty { "Channel $index" },
                                    logo = pendingLogo,
                                    url = trimmed,
                                    group = pendingGroup.ifEmpty { "عام" }
                                )
                            )
                            pendingName = ""
                            pendingLogo = ""
                            pendingGroup = ""
                        }
                    }
                }
            }
        }
        return channels
    }

    // ✅ تحليل ملف مُصنّف (سطراً بسطر)
    private fun parseFileStreamingStructured(file: File): M3uParseResult {
        if (!file.exists() || file.length() == 0L) return M3uParseResult()

        val lines = BufferedReader(
            InputStreamReader(file.inputStream(), "UTF-8"),
            64 * 1024
        ).use { it.readLines() }

        return buildStructuredFromLines(lines)
    }

    // ═══════════════════════════════════════════════════════════
    //  ⭐ المنطق الأساسي للتصنيف
    // ═══════════════════════════════════════════════════════════
    private fun buildStructuredFromLines(lines: List<String>): M3uParseResult {
        val channels = mutableListOf<Channel>()
        val movies = mutableListOf<Movie>()
        val seriesMap = mutableMapOf<String, MutableList<Episode>>()
        val seriesInfoMap = mutableMapOf<String, Series>()

        var pendingName = ""
        var pendingLogo = ""
        var pendingGroup = ""
        var pendingId = ""

        var chIndex = 0
        var movieIndex = 0

        for (rawLine in lines) {
            val trimmed = rawLine.trim()

            when {
                trimmed.startsWith("#EXTINF") -> {
                    pendingName = trimmed.substringAfterLast(",", "").trim()
                    pendingLogo = extractAttribute(trimmed, "tvg-logo")
                    pendingGroup = extractAttribute(trimmed, "group-title")
                    pendingId = extractAttribute(trimmed, "tvg-id")
                }

                trimmed.isNotEmpty() && !trimmed.startsWith("#") -> {
                    if (pendingName.isNotEmpty()) {
                        val url = trimmed
                        val group = pendingGroup
                        val category = classify(group, pendingName, url)

                        when (category) {
                            M3uCategory.SERIES -> {
                                val baseName = extractSeriesBaseName(pendingName)
                                val season = extractSeason(pendingName)
                                val episode = extractEpisode(pendingName)
                                val key = baseName.lowercase().trim()

                                val ep = Episode(
                                    id = "m3u_ep_${seriesMap[key]?.size ?: 0}_${baseName}_${season}_${episode}",
                                    title = pendingName,
                                    season = season,
                                    episode = episode,
                                    url = url,
                                    poster = pendingLogo,
                                    containerExtension = extractExtension(url),
                                )

                                seriesMap.getOrPut(key) { mutableListOf() }.add(ep)

                                if (!seriesInfoMap.containsKey(key)) {
                                    seriesInfoMap[key] = Series(
                                        id = "m3u_series_$key",
                                        name = baseName,
                                        poster = pendingLogo,
                                        category = group.ifBlank { "مسلسلات" },
                                        addedAt = System.currentTimeMillis(),
                                        episodeCount = 0,
                                        seasonCount = 0,
                                    )
                                }
                            }

                            M3uCategory.MOVIE -> {
                                movies.add(
                                    Movie(
                                        id = "m3u_m_${movieIndex++}",
                                        name = pendingName,
                                        poster = pendingLogo,
                                        url = url,
                                        category = group.ifBlank { "أفلام" },
                                        containerExtension = extractExtension(url),
                                        addedAt = System.currentTimeMillis(),
                                    )
                                )
                            }

                            M3uCategory.CHANNEL -> {
                                channels.add(
                                    Channel(
                                        id = "ch_${chIndex++}",
                                        name = pendingName.ifEmpty { "Channel $chIndex" },
                                        logo = pendingLogo,
                                        url = url,
                                        group = group.ifEmpty { "عام" },
                                        epgChannelId = pendingId.takeIf { it.isNotBlank() },
                                    )
                                )
                            }
                        }

                        pendingName = ""
                        pendingLogo = ""
                        pendingGroup = ""
                        pendingId = ""
                    }
                }
            }
        }

        // ─── بناء المسلسلات النهائية من الحلقات ───
        val series = mutableListOf<Series>()
        val seriesEpisodes = mutableMapOf<String, List<Episode>>()

        seriesInfoMap.forEach { (key, info) ->
            val eps = seriesMap[key] ?: emptyList()
            val seasonCount = eps.map { it.season }.distinct().size
            series.add(
                info.copy(
                    episodeCount = eps.size,
                    seasonCount = seasonCount,
                )
            )
            seriesEpisodes[info.id] = eps.sortedWith(
                compareBy({ it.season }, { it.episode })
            )
        }

        return M3uParseResult(
            channels = channels,
            movies = movies,
            series = series,
            seriesEpisodes = seriesEpisodes,
        )
    }

    // ═══════════════════════════════════════════════════════════
    //  أدوات التصنيف
    // ═══════════════════════════════════════════════════════════
    private enum class M3uCategory { CHANNEL, MOVIE, SERIES }

    private fun classify(group: String, name: String, url: String): M3uCategory {
        val g = group.lowercase()
        val n = name.lowercase()

        // ─── مسلسل ───
        if (g.contains("series") || g.contains("show") ||
            g.contains("مسلسل") || g.contains("مسلسلات") ||
            g.contains("دراما") || g.contains("tv show")
        ) return M3uCategory.SERIES

        // نمط S01E01 أو 1x01
        if (Regex("""(?i)[sS]\d{1,2}\s*[eE]\d{1,3}""").containsMatchIn(name)) {
            return M3uCategory.SERIES
        }
        if (Regex("""\d{1,2}[xX]\d{1,3}""").containsMatchIn(name)) {
            return M3uCategory.SERIES
        }

        // ─── فيلم ───
        if (g.contains("movie") || g.contains("movies") ||
            g.contains("film") || g.contains("vod") ||
            g.contains("أفلام") || g.contains("فيلم") ||
            g.contains("سينما") || g.contains("cinema")
        ) return M3uCategory.MOVIE

        // امتداد فيديو في الرابط
        val ext = url.substringBefore("?").lowercase()
        if (ext.endsWith(".mp4") || ext.endsWith(".mkv") ||
            ext.endsWith(".avi") || ext.endsWith(".mov") ||
            ext.endsWith(".webm") || ext.endsWith(".flv")
        ) return M3uCategory.MOVIE

        // ─── قناة (افتراضي) ───
        return M3uCategory.CHANNEL
    }

    private fun extractSeriesBaseName(name: String): String {
        // إزالة S01E01 وما بعده
        var base = name
        base = base.replace(
            Regex("""(?i)\s*[sS]\d{1,2}\s*[eE]\d{1,3}.*$"""),
            ""
        )
        base = base.replace(
            Regex("""\s*\d{1,2}[xX]\d{1,3}.*$"""),
            ""
        )
        return base.trim().ifBlank { name }
    }

    private fun extractSeason(name: String): Int {
        val sMatch = Regex("""(?i)[sS](\d{1,2})""").find(name)
        if (sMatch != null) return sMatch.groupValues[1].toIntOrNull() ?: 1

        val xMatch = Regex("""(\d{1,2})[xX]\d{1,3}""").find(name)
        if (xMatch != null) return xMatch.groupValues[1].toIntOrNull() ?: 1

        return 1
    }

    private fun extractEpisode(name: String): Int {
        val eMatch = Regex("""(?i)[sS]\d{1,2}\s*[eE](\d{1,3})""").find(name)
        if (eMatch != null) return eMatch.groupValues[1].toIntOrNull() ?: 1

        val xMatch = Regex("""\d{1,2}[xX](\d{1,3})""").find(name)
        if (xMatch != null) return xMatch.groupValues[1].toIntOrNull() ?: 1

        return 1
    }

    private fun extractExtension(url: String): String? {
        val clean = url.substringBefore("?").lowercase()
        return when {
            clean.endsWith(".mp4") -> "mp4"
            clean.endsWith(".mkv") -> "mkv"
            clean.endsWith(".avi") -> "avi"
            clean.endsWith(".mov") -> "mov"
            clean.endsWith(".webm") -> "webm"
            clean.endsWith(".flv") -> "flv"
            clean.endsWith(".ts") -> "ts"
            else -> null
        }
    }

    private fun extractAttribute(line: String, attr: String): String {
        val regex = "$attr=\"([^\"]*)\"".toRegex()
        return regex.find(line)?.groupValues?.get(1) ?: ""
    }
}
