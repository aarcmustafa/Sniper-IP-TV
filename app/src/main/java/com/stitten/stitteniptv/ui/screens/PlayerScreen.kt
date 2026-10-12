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
//  بناء المرشحين — محدود بـ 2 (per PCAP analysis)
// ═══════════════════════════════════════════════════════════════

private fun buildCandidates(url: String, kind: StreamKind): List<String> {
    if (url.isBlank()) return listOf(url)

    val lower = url.lowercase()

    // ─── HLS / M3U8 — لا نبدّل شيئًا ───
    if (lower.substringBefore("?").endsWith(".m3u8") ||
        lower.substringBefore("?").endsWith(".m3u")
    ) {
        return listOf(url)
    }

    val cleanUrl = url.substringBefore("?")
    val query = url.substringAfter("?", "").let {
        if (it.isNotEmpty() && it != url) "?$it" else ""
    }

    val hasExt = cleanUrl.contains(".")
    val base = if (hasExt) cleanUrl.substringBeforeLast(".") else cleanUrl
    val currentExt = if (hasExt) cleanUrl.substringAfterLast(".").lowercase() else ""

    val candidates = mutableListOf(url)

    // ─── القنوات المباشرة: الأصلي + بديل واحد ───
    if (kind == StreamKind.LIVE) {
        val alternative = when (currentExt) {
            "ts" -> "m3u8"
            "m3u8" -> "ts"
            else -> "m3u8"
        }
        candidates.add("$base.$alternative$query")
        return candidates
    }

    // ─── الفيلم/المسلسل: الأصلي + بديل واحد ───
    if (!hasExt) return listOf(url)

    val alternative = when (currentExt) {
        "mp4" -> "mkv"
        "mkv" -> "mp4"
        "ts" -> "m3u8"
        "m3u8" -> "ts"
        else -> "mp4"
    }
    candidates.add("$base.$alternative$query")
    return candidates
}
