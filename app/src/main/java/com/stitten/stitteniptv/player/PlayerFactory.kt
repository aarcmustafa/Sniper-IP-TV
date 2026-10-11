package com.stitten.stitteniptv.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

enum class StreamKind { LIVE, MOVIE, EPISODE }

@UnstableApi
object PlayerFactory {

    fun create(
        context: Context,
        candidates: List<String>,
        kind: StreamKind,
        startPositionMs: Long = 0L,
    ): ExoPlayer {

        // ✅ RetryHttpDataSource لم يعد يستقبل context
        val dataSource = RetryHttpDataSource(candidates)
        val mediaSourceFactory = DefaultMediaSourceFactory { dataSource }

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                if (kind == StreamKind.LIVE) 8_000 else 30_000,
                if (kind == StreamKind.LIVE) 20_000 else 90_000,
                1_500,
                3_000
            )
            .build()

        val player = ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setHandleAudioBecomingNoisy(true)
            .build()

        player.setMediaItem(MediaItem.fromUri(candidates.first()))
        player.prepare()
        if (startPositionMs > 0) player.seekTo(startPositionMs)
        player.playWhenReady = true

        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                val pos = player.currentPosition
                player.prepare()
                player.seekTo(pos)
                player.playWhenReady = true
            }
        })

        return player
    }
}
