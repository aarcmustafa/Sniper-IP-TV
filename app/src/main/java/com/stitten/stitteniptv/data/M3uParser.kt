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
