package com.stitten.stitteniptv.data.network

data class XtreamCredentials(
    val baseUrl: String,
    val username: String,
    val password: String,
)

class StreamUrlBuilder(private val creds: XtreamCredentials) {

    private fun normalizeBase(url: String): String =
        url.trim().removeSuffix("/")

    fun liveUrl(streamId: Int, ext: String = "ts"): String =
        "${normalizeBase(creds.baseUrl)}/live/${creds.username}/${creds.password}/$streamId.$ext"

    fun movieUrl(streamId: Int, ext: String): String =
        "${normalizeBase(creds.baseUrl)}/movie/${creds.username}/${creds.password}/$streamId.$ext"

    fun episodeUrl(episodeId: Int, ext: String): String =
        "${normalizeBase(creds.baseUrl)}/series/${creds.username}/${creds.password}/$episodeId.$ext"

    fun movieCandidates(streamId: Int, originalExt: String): List<String> {
        val exts = (listOf(originalExt) + listOf("mp4", "mkv", "avi", "ts"))
            .distinct()
            .filter { it.isNotBlank() }
        return exts.map { movieUrl(streamId, it) }
    }

    fun episodeCandidates(episodeId: Int, originalExt: String): List<String> {
        val exts = (listOf(originalExt) + listOf("mp4", "mkv", "avi"))
            .distinct()
            .filter { it.isNotBlank() }
        return exts.map { episodeUrl(episodeId, it) }
    }

    fun liveCandidates(streamId: Int): List<String> =
        listOf("ts", "m3u8").map { liveUrl(streamId, it) }
}
