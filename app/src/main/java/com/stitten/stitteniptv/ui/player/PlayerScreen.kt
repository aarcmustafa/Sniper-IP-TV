package com.stitten.stitteniptv.ui.player

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
import com.stitten.stitteniptv.database.entity.WatchHistoryEntity
import com.stitten.stitteniptv.player.PlayerFactory
import com.stitten.stitteniptv.player.StreamKind
import com.stitten.stitteniptv.ui.screens.PlayerEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@UnstableApi
@Composable
fun CorePlayerScreen(
    candidates: List<String>,
    kind: StreamKind,
    title: String,
    startPositionMs: Long = 0L,
    contentId: String = "",
    contentType: String = "",
    posterUrl: String = "",
    onClose: () -> Unit,
) {
    val context = LocalContext.current

    val entryPoint = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            PlayerEntryPoint::class.java
        )
    }
    val historyMgr = remember { entryPoint.watchHistoryManager() }

    // نطاق لا يُلغى عند الخروج (لإتمام الحفظ بعد إغلاق الشاشة)
    val saveScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.IO) }

    val exoPlayer = remember(candidates, kind) {
        PlayerFactory.create(context, candidates, kind, startPositionMs)
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                val position = exoPlayer.currentPosition
                val duration = exoPlayer.duration
                if (contentId.isNotBlank() && position > 5_000) {
                    saveScope.launch {
                        try {
                            historyMgr.upsert(
                                WatchHistoryEntity(
                                    contentId = contentId,
                                    contentType = contentType.ifBlank { kind.name },
                                    title = title,
                                    poster = posterUrl,
                                    url = candidates.firstOrNull() ?: "",
                                    positionMs = position,
                                    durationMs = if (duration > 0) duration else 0L,
                                    updatedAt = System.currentTimeMillis()
                                )
                            )
                        } catch (_: Exception) { }
                    }
                }
            } catch (_: Exception) { }
            exoPlayer.release()
        }
    }

    Box(
        Modifier.fillMaxSize().background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
