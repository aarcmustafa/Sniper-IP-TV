package com.stitten.stitteniptv.ui.vod

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.media3.common.util.UnstableApi
import com.stitten.stitteniptv.data.network.StreamUrlBuilder
import com.stitten.stitteniptv.player.StreamKind
import com.stitten.stitteniptv.ui.player.PlayerScreen

@UnstableApi
@Composable
fun MoviePlayerRoute(
    builder: StreamUrlBuilder,
    streamId: Int,
    containerExt: String,
    title: String,
    startPositionMs: Long = 0L,
    onClose: () -> Unit,
) {
    val candidates = remember(streamId, containerExt) {
        builder.movieCandidates(streamId, containerExt)
    }
    PlayerScreen(
        candidates = candidates,
        kind = StreamKind.MOVIE,
        title = title,
        startPositionMs = startPositionMs,
        onClose = onClose,
    )
}
