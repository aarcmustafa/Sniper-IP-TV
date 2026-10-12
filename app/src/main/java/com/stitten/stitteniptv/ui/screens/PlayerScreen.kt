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
//  ✅ بناء المرشحين — نُعيد الرابط الأصلي فقط
//
//  السبب: تمرير مرشح ثانٍ بصيغة مختلفة (.ts ↔ .m3u8) يؤدي إلى:
//    1. فتح قناة مختلفة تمامًا عند أول تقطيع شبكة
//    2. ظهور "قفزات" ومشاهد غريبة أثناء التشغيل
//    3. إعادة تحميل لا داعي لها
//
//  RetryHttpDataSource في PlayerFactory يتعامل مع إعادة المحاولة
//  تلقائيًا على نفس الرابط بدون أي تدخل خارجي.
// ═══════════════════════════════════════════════════════════════

private fun buildCandidates(url: String, kind: StreamKind): List<String> {
    if (url.isBlank()) return listOf(url)
    return listOf(url)
}
