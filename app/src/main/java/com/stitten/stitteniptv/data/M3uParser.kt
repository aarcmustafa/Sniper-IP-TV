package com.stitten.stitteniptv.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object M3uParser {

    suspend fun loadFromUrl(url: String): List<Channel> = withContext(Dispatchers.IO) {
        try {
            val result = HttpClientProvider.fetchText(url)
            if (result.isFailure) return@withContext emptyList()
            parse(result.getOrNull() ?: "")
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun loadFromContent(content: String): List<Channel> =
        withContext(Dispatchers.IO) { parse(content) }

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
