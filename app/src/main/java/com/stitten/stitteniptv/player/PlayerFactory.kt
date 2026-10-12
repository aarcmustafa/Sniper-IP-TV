package com.stitten.stitteniptv.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector

enum class StreamKind { LIVE, MOVIE, EPISODE }

@UnstableApi
object PlayerFactory {

    private const val MAX_RETRIES = 8

    fun create(
        context: Context,
        candidates: List<String>,
        kind: StreamKind,
        startPositionMs: Long = 0L,
    ): ExoPlayer {

        // ─── 1) DataSource ───
        val dataSource = RetryHttpDataSource(candidates)
        val mediaSourceFactory = DefaultMediaSourceFactory { dataSource }

        // ─── 2) RenderersFactory ───
        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(
                DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
            )
            .setEnableAudioTrackPlaybackParams(true)

        // ─── 3) LoadControl ───
        val isLive = kind == StreamKind.LIVE
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                if (isLive) 5_000 else 20_000,
                if (isLive) 60_000 else 120_000,
                1_500,
                3_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .setBackBuffer(30_000, true)
            .setTargetBufferBytes(C.LENGTH_UNSET)
            .build()

        // ─── 4) TrackSelector ───
        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setAllowVideoMixedMimeTypeAdaptiveness(true)
                    .setAllowAudioMixedMimeTypeAdaptiveness(true)
                    .setAllowVideoNonSeamlessAdaptiveness(true)
                    .setAllowAudioNonSeamlessAdaptiveness(true)
                    .setForceHighestSupportedBitrate(false)
                    .setExceedVideoConstraintsIfNecessary(true)
                    .setExceedAudioConstraintsIfNecessary(true)
            )
        }

        // ─── 5) ExoPlayer builder ───
        val player = ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setTrackSelector(trackSelector)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true
            )
            .setUseLazyPreparation(true)
            .build()

        // ─── 6) MediaItem + LiveConfiguration ───
        val mediaItem = MediaItem.fromUri(candidates.first())

        if (isLive) {
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

        // ─── 7) Error listener ───
        var errorCount = 0
        var lastErrorTime = 0L

        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                val now = System.currentTimeMillis()
                if (now - lastErrorTime > 30_000) errorCount = 0
                errorCount++
                lastErrorTime = now

                if (errorCount > MAX_RETRIES) return

                val pos = player.currentPosition
                player.prepare()
                if (pos > 0) player.seekTo(pos)
                player.playWhenReady = true
            }
        })

        return player
    }
}
