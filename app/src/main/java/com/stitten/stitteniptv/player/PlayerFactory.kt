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

    fun create(
        context: Context,
        candidates: List<String>,
        kind: StreamKind,
        startPositionMs: Long = 0L,
    ): ExoPlayer {

        val dataSource = RetryHttpDataSource(candidates)
        val mediaSourceFactory = DefaultMediaSourceFactory { dataSource }

        // ═══════════════════════════════════════════════════════════
        //  LoadControl — يعوّض VLC caching options:
        //    --network-caching=1500
        //    --live-caching=1500
        //    --file-caching=1000
        //  نستخدم قيمًا أعلى للبث المباشر لأن IPTV أكثر عرضة للانقطاع
        // ═══════════════════════════════════════════════════════════
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ if (kind == StreamKind.LIVE) 5_000 else 15_000,
                /* maxBufferMs = */ if (kind == StreamKind.LIVE) 60_000 else 120_000,
                /* bufferForPlaybackMs = */ 1_500,
                /* bufferForPlaybackAfterRebufferMs = */ 3_000,
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .setBackBuffer(30_000, true)   // VLC-like back buffer
            .setTargetBufferBytes(C.LENGTH_UNSET)
            .build()

        // ═══════════════════════════════════════════════════════════
        //  RenderersFactory — يعوّض:
        //    --avcodec-hw=any
        //  مع fallback تلقائي إلى SW إذا فشل HW (مثل VLC)
        // ═══════════════════════════════════════════════════════════
        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)                             // ← أساسي
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            .setEnableAudioTrackPlaybackParams(true)

        // ═══════════════════════════════════════════════════════════
        //  TrackSelector — تسامح مع صيغ متنوعة (مثل VLC)
        //  بدون هذه الخيارات، بعض القنوات قد ترفض التشغيل
        // ═══════════════════════════════════════════════════════════
        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setAllowVideoMixedMimeTypeAdaptiveness(true)
                    .setAllowAudioMixedMimeTypeAdaptiveness(true)
                    .setAllowVideoNonSeamlessAdaptiveness(true)     // ← يمنع التوقف على القنوات المتقطعة
                    .setAllowAudioNonSeamlessAdaptiveness(true)
                    .setForceHighestSupportedBitrate(false)
                    .setExceedVideoConstraintsIfNecessary(true)     // ← يتجاوز قيود الفيديو
                    .setExceedAudioConstraintsIfNecessary(true)     // ← يتجاوز قيود الصوت
            )
        }

        // ═══════════════════════════════════════════════════════════
        //  ExoPlayer builder — يضيف:
        //    setWakeMode          (يمنع نوم الشاشة أثناء البث)
        //    AudioAttributes      (USB DAC/Bluetooth)
        //    handleAudioBecomingNoisy (يقاطع عند فصل السماعات)
        // ═══════════════════════════════════════════════════════════
        val player = ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setTrackSelector(trackSelector)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)                           // ← يمنع نوم الشاشة
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setUseLazyPreparation(true)
            .build()

        val mediaItem = MediaItem.fromUri(candidates.first())

        // ═══════════════════════════════════════════════════════════
        //  LiveConfiguration — للقنوات المباشرة
        //  يعوّض VLC's live caching + speed adjustment
        // ═══════════════════════════════════════════════════════════
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

        // ═══════════════════════════════════════════════════════════
        //  Error handling — إعادة محاولة ذكية (مثل VLC)
        //  حتى 3 محاولات خلال 30 ثانية
        // ═══════════════════════════════════════════════════════════
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
