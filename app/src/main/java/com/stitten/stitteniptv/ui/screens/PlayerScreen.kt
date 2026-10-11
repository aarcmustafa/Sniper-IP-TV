package com.stitten.stitteniptv.ui.screens

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.navigation.NavHostController
import com.stitten.stitteniptv.player.PlayerFactory
import com.stitten.stitteniptv.player.StreamKind
import com.stitten.stitteniptv.data.PrefsManager

@UnstableApi
@Composable
fun PlayerScreen(
    navController: NavHostController,
    url: String,
    title: String,
) {
    val context = LocalContext.current

    // ── بناء قائمة المرشحين من الرابط الواحد ──
    val candidates = remember(url) { buildCandidates(url) }

    // ── تحديد نوع البث من الرابط ──
    val kind = remember(url) { detectKind(url) }

    // ── إنشاء المشغل ──
    val exoPlayer = remember(candidates, kind) {
        PlayerFactory.create(context, candidates, kind)
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  أدوات مساعدة
// ═══════════════════════════════════════════════════════════════

/** يبني قائمة الروابط بامتدادات مختلفة لتجربتها تباعًا */
private fun buildCandidates(url: String): List<String> {
    if (url.isBlank()) return listOf(url)

    // لا نبدّل الامتداد لروابط HLS أو M3U
    val lower = url.substringBefore("?").lowercase()
    if (lower.endsWith(".m3u8") || lower.endsWith(".m3u")) {
        return listOf(url)
    }

    val alternatives = listOf("mp4", "mkv", "avi", "ts", "m3u8")
    val base = url.substringBeforeLast(".")

    val candidates = mutableListOf(url)
    alternatives.forEach { ext ->
        val candidate = "$base.$ext"
        if (candidate != url && candidate !in candidates) {
            candidates.add(candidate)
        }
    }
    return candidates
}

/** يحدّد نوع البث من الرابط */
private fun detectKind(url: String): StreamKind {
    val lower = url.lowercase()
    return when {
        lower.contains("/live/") -> StreamKind.LIVE
        lower.contains("/series/") -> StreamKind.EPISODE
        lower.contains("/movie/") -> StreamKind.MOVIE
        else -> StreamKind.MOVIE
    }
}
