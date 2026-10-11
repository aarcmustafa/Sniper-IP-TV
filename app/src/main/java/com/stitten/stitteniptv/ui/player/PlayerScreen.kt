package com.stitten.stitteniptv.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import com.stitten.stitteniptv.player.StreamKind
import com.stitten.stitteniptv.ui.player.PlayerScreen as CorePlayerScreen

/**
 * غلاف يربط NavGraph (الذي يمرّر url فقط) بالمشغل الحقيقي في ui/player/.
 * يبني قائمة المرشحين ويحدّد نوع البث تلقائيًا.
 */
@UnstableApi
@Composable
fun PlayerScreen(
    navController: NavHostController,
    url: String,
    title: String,
) {
    val candidates = remember(url) { buildCandidates(url) }
    val kind = remember(url) { detectKind(url) }

    CorePlayerScreen(
        candidates = candidates,
        kind = kind,
        title = title,
        startPositionMs = 0L,
        onClose = { navController.popBackStack() },
    )
}

// ═══════════════════════════════════════════════════════════════
//  أدوات مساعدة
// ═══════════════════════════════════════════════════════════════

/**
 * يبني قائمة روابط بامتدادات مختلفة ليجرّبها المشغل عند الفشل.
 * لا يبدّل امتداد روابط HLS/M3U لأنها تعمل كما هي.
 */
private fun buildCandidates(url: String): List<String> {
    if (url.isBlank()) return listOf(url)

    val cleanUrl = url.substringBefore("?")
    val lower = cleanUrl.lowercase()

    // روابط HLS/M3U → نتركها كما هي
    if (lower.endsWith(".m3u8") || lower.endsWith(".m3u")) {
        return listOf(url)
    }

    // لا نبدّل الامتداد إن لم يوجد امتداد
    if (!cleanUrl.contains(".")) return listOf(url)

    val base = url.substringBeforeLast(".")
    val query = url.substringAfterLast(".").substringAfter("?", "")

    val alternatives = listOf("mp4", "mkv", "avi", "ts", "m3u8")
    val candidates = mutableListOf(url)

    alternatives.forEach { ext ->
        val candidate = if (query.isBlank()) "$base.$ext" else "$base.$ext?$query"
        if (candidate != url && candidate !in candidates) {
            candidates.add(candidate)
        }
    }
    return candidates
}

/**
 * يحدّد نوع البث من الرابط لتعديل إعدادات Buffer في المشغل.
 */
private fun detectKind(url: String): StreamKind {
    val lower = url.lowercase()
    return when {
        lower.contains("/live/") -> StreamKind.LIVE
        lower.contains("/series/") -> StreamKind.EPISODE
        else -> StreamKind.MOVIE
    }
}
