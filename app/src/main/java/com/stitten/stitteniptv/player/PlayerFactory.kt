package com.stitten.stitteniptv.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector

enum class StreamKind { LIVE, MOVIE, EPISODE }

@UnstableApi
object PlayerFactory {

    fun create(
        context: Context,
        candidates: List<String>,
        kind: StreamKind,
        startPositionMs: Long = 0L,
    ): ExoPlayer {

        val dataSource = RetryHttpDataSource(candidates)
        val mediaSourceFactory = DefaultMediaSourceFactory { dataSource }

        // LoadControl محسّن
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                30_000,
                120_000,
                3_000,
                6_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        // TrackSelector
        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setAllowVideoMixedMimeTypeAdaptiveness(true)
                    .setAllowAudioMixedMimeTypeAdaptiveness(true)
                    .setForceHighestSupportedBitrate(false)
            )
        }

        val player = ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setTrackSelector(trackSelector)
            .setHandleAudioBecomingNoisy(true)
            .build()

        val mediaItem = MediaItem.fromUri(candidates.first())

        // LiveConfiguration تُضاف إلى MediaItem نفسه
        if (kind == StreamKind.LIVE) {
            val liveConfig = MediaItem.LiveConfiguration.Builder()
                .setTargetOffsetMs(3_000)
                .setMinPlaybackSpeed(0.95f)
                .setMaxPlaybackSpeed(1.05f)
                .build()

            player.setMediaItem(
                mediaItem.buildUpon()
                    .setLiveConfiguration(liveConfig)
                    .build()
            )
        } else {
            player.setMediaItem(mediaItem, startPositionMs)
        }

        player.prepare()
        player.playWhenReady = true

        // إدارة أخطاء ذكية - حد 3 محاولات
        var errorCount = 0
        var lastErrorTime = 0L

        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                val now = System.currentTimeMillis()
                if (now - lastErrorTime > 30_000) errorCount = 0
                errorCount++
                lastErrorTime = now

                if (errorCount > 3) return

                val pos = player.currentPosition
                player.prepare()
                if (pos > 0) player.seekTo(pos)
                player.playWhenReady = true
            }
        })

        return player
    }
}
