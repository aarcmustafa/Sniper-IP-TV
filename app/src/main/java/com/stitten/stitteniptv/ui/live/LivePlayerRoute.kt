package com.stitten.stitteniptv.ui.live

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.media3.common.util.UnstableApi
import com.stitten.stitteniptv.data.network.StreamUrlBuilder
import com.stitten.stitteniptv.player.StreamKind
import com.stitten.stitteniptv.ui.player.PlayerScreen

@UnstableApi
@Composable
fun LivePlayerRoute(
    builder: StreamUrlBuilder,
    streamId: Int,
    title: String,
    onClose: () -> Unit,
) {
    val candidates = remember(streamId) { builder.liveCandidates(streamId) }
    PlayerScreen(
        candidates = candidates,
        kind = StreamKind.LIVE,
        title = title,
        onClose = onClose,
    )
}
