package com.stitten.stitteniptv.data.network

data class XtreamCredentials(
    val baseUrl: String,
    val username: String,
    val password: String,
)

class StreamUrlBuilder(private val creds: XtreamCredentials) {

    // ═══════════════════════════════════════════════════════════
    //  قوائم الامتدادات حسب النوع
    // ═══════════════════════════════════════════════════════════

    /** امتدادات القنوات المباشرة (الأكثر شيوعًا أولًا) */
    private val liveExtensions = listOf(
        "ts",       // الأكثر شيوعًا في Xtream
        "m3u8",     // HLS
        "m3u",      // بديل نادر
    )

    /** امتدادات الأفلام (VOD) */
    private val vodExtensions = listOf(
        "mp4",      // الأكثر شيوعًا
        "mkv",
        "avi",
        "mov",
        "ts",
        "m3u8",     // بعض المزودين يقدمون VOD عبر HLS
        "m3u",
        "flv",
        "webm",
        "wmv",
    )

    /** امتدادات حلقات المسلسلات */
    private val episodeExtensions = listOf(
        "mp4",
        "mkv",
        "avi",
        "mov",
        "ts",
        "m3u8",
        "m3u",
        "flv",
        "webm",
        "wmv",
    )

    // ═══════════════════════════════════════════════════════════
    //  أدوات التنظيف
    // ═══════════════════════════════════════════════════════════

    private fun normalizeBase(url: String): String =
        url.trim().removeSuffix("/")

    /**
     * تنظيف الامتداد:
     *  - إزالة المسافات
     *  - إزالة النقطة في البداية إن وُجدت
     *  - تحويل للحروف الصغيرة
     *  - إزالة أي رمز غريب
     */
    private fun cleanExtension(ext: String?): String? {
        if (ext.isNullOrBlank()) return null
        val cleaned = ext
            .trim()
            .removePrefix(".")
            .lowercase()
            .replace(Regex("[^a-z0-9]"), "")
        return cleaned.ifBlank { null }
    }

    /**
     * بناء قائمة مرشحين نظيفة:
     *  - نبدأ بالامتداد الأصلي (إن وُجد وصالح)
     *  - نضيف الباقي بترتيب الأولوية
     *  - بدون تكرار
     *  - بدون امتدادات فارغة أو غريبة
     */
    private fun buildCandidates(
        originalExt: String?,
        priorityList: List<String>,
    ): List<String> {
        val cleaned = cleanExtension(originalExt)
        val all = buildList {
            if (cleaned != null) add(cleaned)
            addAll(priorityList)
        }
        return all.distinct().filter { it.isNotBlank() }
    }

    // ═══════════════════════════════════════════════════════════
    //  بناء الروابط
    // ═══════════════════════════════════════════════════════════

    fun liveUrl(streamId: Int, ext: String): String =
        "${normalizeBase(creds.baseUrl)}/live/${creds.username}/${creds.password}/$streamId.$ext"

    fun movieUrl(streamId: Int, ext: String): String =
        "${normalizeBase(creds.baseUrl)}/movie/${creds.username}/${creds.password}/$streamId.$ext"

    fun episodeUrl(episodeId: Int, ext: String): String =
        "${normalizeBase(creds.baseUrl)}/series/${creds.username}/${creds.password}/$episodeId.$ext"

    // ═══════════════════════════════════════════════════════════
    //  المرشحون (نظيفون)
    // ═══════════════════════════════════════════════════════════

    /** قنوات مباشرة: ts → m3u8 → m3u */
    fun liveCandidates(streamId: Int, originalExt: String? = null): List<String> =
        buildCandidates(originalExt, liveExtensions).map { liveUrl(streamId, it) }

    /** أفلام: نبدأ بالامتداد الأصلي ثم mp4 → mkv → ... */
    fun movieCandidates(streamId: Int, originalExt: String?): List<String> =
        buildCandidates(originalExt, vodExtensions).map { movieUrl(streamId, it) }

    /** حلقات المسلسل */
    fun episodeCandidates(episodeId: Int, originalExt: String?): List<String> =
        buildCandidates(originalExt, episodeExtensions).map { episodeUrl(episodeId, it) }

    // ═══════════════════════════════════════════════════════════
    //  كشف نوع الرابط
    // ═══════════════════════════════════════════════════════════

    companion object {
        /** هل الرابط قائمة تشغيل M3U/M3U8؟ */
        fun isPlaylist(url: String): Boolean {
            val lower = url.substringBefore("?").lowercase()
            return lower.endsWith(".m3u") || lower.endsWith(".m3u8")
        }

        /** هل الرابط بث مباشر؟ */
        fun isLiveUrl(url: String): Boolean =
            url.contains("/live/") || url.contains(".ts") || url.contains("m3u8")

        /** هل الرابط فيلم VOD؟ */
        fun isMovieUrl(url: String): Boolean = url.contains("/movie/")

        /** هل الرابط حلقة مسلسل؟ */
        fun isEpisodeUrl(url: String): Boolean = url.contains("/series/")
    }
}
