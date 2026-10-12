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

private fun buildCandidates(url: String, kind: StreamKind): List<String> {
    if (url.isBlank()) return listOf(url)

    val cleanUrl = url.substringBefore("?")
    val lower = cleanUrl.lowercase()

    if (lower.endsWith(".m3u8") || lower.endsWith(".m3u")) {
        return listOf(url)
    }
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
