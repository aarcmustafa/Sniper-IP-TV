package com.stitten.stitteniptv.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader

object M3uParser {

    // ============== التحميل العادي (ملفات صغيرة < 20 MB) ==============
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

    // ============== التحميل الانسيابي (ملفات ضخمة > 20 MB) ==============
    suspend fun loadFromUrlStreaming(
        ctx: Context,
        url: String,
        onProgress: (downloadedMb: Long, totalMb: Long) -> Unit = { _, _ -> }
    ): List<Channel> = withContext(Dispatchers.IO) {
        val tempFile = File(ctx.cacheDir, "temp_playlist.m3u")

        try {
            // 1. تحميل الملف إلى القرص (بدون تحميله في الذاكرة)
            val downloaded = downloadToFile(url, tempFile, onProgress)
            if (!downloaded) return@withContext emptyList()

            // 2. تحليل الملف من القرص سطراً بسطر
            val channels = parseFileStreaming(tempFile)

            // 3. حذف الملف المؤقت
            tempFile.delete()

            channels
        } catch (e: Exception) {
            tempFile.delete()
            emptyList()
        }
    }

    // ============== التحميل إلى ملف (Disk I/O) ==============
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
                    val buffer = ByteArray(64 * 1024) // 64 KB buffer
                    var bytesRead: Int
                    var totalRead = 0L
                    var lastReported = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead

                        // الإبلاغ عن التقدم كل 1 MB
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

    // ============== تحليل الملف سطراً بسطر (Streaming) ==============
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
    
    // ============== تحميل من محتوى نصي ==============
    suspend fun loadFromContent(content: String): List<Channel> =
        withContext(Dispatchers.IO) { parse(content) }

    // ============== تحليل نص عادي ==============
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

    private fun extractAttribute(line: String, attr: String): String {
        val regex = "$attr=\"([^\"]*)\"".toRegex()
        return regex.find(line)?.groupValues?.get(1) ?: ""
    }
}
