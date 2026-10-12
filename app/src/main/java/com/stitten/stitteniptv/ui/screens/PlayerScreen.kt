package com.stitten.stitteniptv.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import com.stitten.stitteniptv.player.StreamKind
import com.stitten.stitteniptv.ui.player.CorePlayerScreen
import dagger.hilt.android.EntryPointAccessors

@UnstableApi
@Composable
fun PlayerScreen(
    navController: NavHostController,
    url: String,
    title: String,
) {
    val context = LocalContext.current

    val entryPoint = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            PlayerEntryPoint::class.java
        )
    }
    val historyMgr = remember { entryPoint.watchHistoryManager() }

    val kind = remember(url) { detectKind(url) }
    val candidates = remember(url, kind) { buildCandidates(url, kind) }

    var startPos by remember { mutableLongStateOf(0L) }
    var resolved by remember { mutableStateOf(false) }

    LaunchedEffect(url) {
        try {
            val h = historyMgr.getById(url)
            if (h != null && h.positionMs > 5_000) {
                startPos = h.positionMs
            }
        } catch (_: Exception) { }

        resolved = true
    }

    if (resolved) {
        CorePlayerScreen(
            candidates = candidates,
            kind = kind,
            title = title,
            startPositionMs = startPos,
            contentId = url,
            contentType = kind.name,
            posterUrl = "",
            onClose = { navController.popBackStack() },
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  تحديد نوع البث
// ═══════════════════════════════════════════════════════════════

private fun detectKind(url: String): StreamKind {
    val lower = url.lowercase()
    return when {
        lower.contains("/live/") -> StreamKind.LIVE
        lower.contains("/series/") -> StreamKind.EPISODE
        else -> StreamKind.MOVIE
    }
}

// ═══════════════════════════════════════════════════════════════
//  بناء المرشحين
// ═══════════════════════════════════════════════════════════════

private fun buildCandidates(url: String, kind: StreamKind): List<String> {
    if (url.isBlank()) return listOf(url)

    val lower = url.lowercase()

    // ─── HLS / M3U8 — نُبقي الرابط كما هو ───
    if (lower.substringBefore("?").endsWith(".m3u8") ||
        lower.substringBefore("?").endsWith(".m3u"))
    {
        return listOf(url)
    }

    // استخراج الأجزاء
    val cleanUrl = url.substringBefore("?")
    val query = url.substringAfter("?", "").let {
        if (it.isNotEmpty() && it != url) "?$it" else ""
    }

    val hasExt = cleanUrl.contains(".")
    val base = if (hasExt) cleanUrl.substringBeforeLast(".") else cleanUrl
    val currentExt = if (hasExt) cleanUrl.substringAfterLast(".").lowercase() else ""

    // ─── القنوات المباشرة ───
    if (kind == StreamKind.LIVE) {
        val candidates = mutableListOf<String>()

        // 1) الأصلي أولًا
        candidates.add(url)

        // 2) بدائل حسب الامتداد
        val alternatives = when (currentExt) {
            "ts" -> listOf("m3u8", "mp4", "")       // إذا فشل ts → m3u8, ثم بدون امتداد
            "m3u8" -> listOf("ts", "mp4", "")        // إذا فشل m3u8 → ts, ثم بدون امتداد
            "mp4" -> listOf("ts", "m3u8", "")        // ...
            else -> listOf("ts", "m3u8", "mp4")      // لو بدون امتداد → جرّب الكل
        }

        alternatives.forEach { ext ->
            val candidate = if (ext.isBlank()) {
                "$base$query"
            } else {
                "$base.$ext$query"
            }
            if (candidate != url && candidate !in candidates) {
                candidates.add(candidate)
            }
        }

        // 3) بديل: بدون /live/ (بعض السيرفرات لا تستخدمه)
        if (url.contains("/live/")) {
            val withoutLive = url.replace("/live/", "/")
            if (withoutLive !in candidates) candidates.add(withoutLive)
        }

        return candidates
    }

    // ─── الفيلم/المسلسل ───
    if (!hasExt) return listOf(url)

    val videoExts = listOf("mp4", "mkv", "avi", "ts", "m3u8")
    val candidates = mutableListOf(url)

    videoExts.forEach { ext ->
        val candidate = "$base.$ext$query"
        if (candidate != url && candidate !in candidates) {
            candidates.add(candidate)
        }
    }
    return candidates
}
