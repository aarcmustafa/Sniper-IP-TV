package com.stitten.stitteniptv.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy

enum class StreamKind { LIVE, MOVIE, EPISODE }

/**
 * مصنع مشغل واحد فقط: ExoPlayer/Media3.
 * مُهيّأ ليعطي أداءً يقارب LibVLC على قنوات IPTV:
 *   - User-Agent متوافق مع VLC
 *   - HW + SW decoder fallback
 *   - إعادة محاولة متعددة
 *   - تسامح مع البث التالف/غير القياسي
 *   - Live latency مضبوط
 */
@UnstableApi
object PlayerFactory {

    // ═══════════════════════════════════════════════════════════
    //  User-Agent المتوافق مع VLC
    //  بعض السيرفرات تُرشّح الاتصال حسب UA وتقبل فقط:
    //    VLC/3.x.x LibVLC/3.x.x
    //  هذا هو أهم سبب لعمل قنوات معينة مع VLC وفشلها بدونه.
    // ═══════════════════════════════════════════════════════════
    const val VLC_USER_AGENT = "VLC/3.0.20 LibVLC/3.0.20"

    // عدد محاولات إعادة الاتصال
    private const val MAX_RETRIES = 8

    // زمن انتظار الشبكة
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 20_000

    fun create(
        context: Context,
        candidates: List<String>,
        kind: StreamKind,
        startPositionMs: Long = 0L,
        userAgent: String = VLC_USER_AGENT,
    ): ExoPlayer {

        // ─── 1) DataSource مع User-Agent + إعادة محاولة مدمجة ───
        val upstreamFactory: DataSource.Factory =
            DefaultHttpDataSource.Factory()
                .setUserAgent(userAgent)
                .setConnectTimeoutMs(CONNECT_TIMEOUT_MS)
                .setReadTimeoutMs(READ_TIMEOUT_MS)
                .setAllowCrossProtocolRedirects(true)
                .setKeepPostFor302Redirects(true)

        // نغلّفه بـ RetryHttpDataSource الموجود لديك (يدعم قائمة مرشحين)
        val retryFactory = RetryHttpDataSource(candidates, upstreamFactory)

        val mediaSourceFactory = DefaultMediaSourceFactory(retryFactory)

        // ─── 2) RenderersFactory — HW مع fallback تلقائي إلى SW ───
        // يعوّض: --avcodec-hw=any
        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(
                DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
            )
            .setEnableAudioTrackPlaybackParams(true)

        // ─── 3) LoadControl — Caching ذكي حسب نوع البث ───
        val isLive = kind == StreamKind.LIVE
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs  = */ if (isLive) 5_000 else 20_000,
                /* maxBufferMs  = */ if (isLive) 60_000 else 120_000,
                /* forPlayback  = */ 1_500,
                /* afterRebuffer= */ 3_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .setBackBuffer(30_000, true)
            .setTargetBufferBytes(C.LENGTH_UNSET)
            .build()

        // ─── 4) TrackSelector — تسامح مع الأنواع المختلطة ───
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

        // ─── 5) Load Error Handling — إعادة محاولة قوية ───
        val errorHandlingPolicy = DefaultLoadErrorHandlingPolicy(MAX_RETRIES)

        // ─── 6) ExoPlayer builder ───
        val player = ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setTrackSelector(trackSelector)
            .setLoadErrorHandlingPolicy(errorHandlingPolicy)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setUseLazyPreparation(true)
            .build()

        // ─── 7) MediaItem + LiveConfiguration ───
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

        // ─── 8) Error listener — يعيد المحاولة عند فشل جسيم ───
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
