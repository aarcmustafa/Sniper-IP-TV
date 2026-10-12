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

private fun detectKind(url: String): StreamKind {
    val lower = url.lowercase()
    return when {
        lower.contains("/live/") -> StreamKind.LIVE
        lower.contains("/series/") -> StreamKind.EPISODE
        else -> StreamKind.MOVIE
    }
}

// ═══════════════════════════════════════════════════════════════
//  بناء المرشحين — مرشحان فقط (per PCAP analysis)
// ═══════════════════════════════════════════════════════════════

private fun buildCandidates(url: String, kind: StreamKind): List<String> {
    if (url.isBlank()) return listOf(url)

    val cleanUrl = url.substringBefore("?")
    val lower = cleanUrl.lowercase()

    // ─── HLS / M3U8 — لا نبدّل ───
    if (lower.endsWith(".m3u8") || lower.endsWith(".m3u")) {
        return listOf(url)
    }

    // ─── القنوات: مرشحان فقط (ts ↔ m3u8) ───
    if (kind == StreamKind.LIVE) {
        val hasExt = cleanUrl.contains(".")
        val base = if (hasExt) cleanUrl.substringBeforeLast(".") else cleanUrl
        val ext = if (hasExt) cleanUrl.substringAfterLast(".").lowercase() else ""

        val alt = if (ext == "ts") "m3u8" else "ts"

        return listOf(url, "$base.$alt")
    }

    // ─── الأفلام: مرشحان فقط ───
    if (!cleanUrl.contains(".")) return listOf(url)

    val base = url.substringBeforeLast(".")
    val query = url.substringAfterLast(".").substringAfter("?", "")

    val alt = if (query.isBlank()) "$base.mkv" else "$base.mkv?$query"

    return listOf(url, alt)
}
