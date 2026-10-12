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

        // ✅ RetryHttpDataSource كما هو (جيد)
        val dataSource = RetryHttpDataSource(candidates)
        val mediaSourceFactory = DefaultMediaSourceFactory { dataSource }

        // ═══════════════════════════════════════════════════════════
        //  ✅ LoadControl محسّن: مخزون أكبر بكثير للبث المباشر
        //  min=30s / max=120s / start=3s / afterRebuffer=6s
        // ═══════════════════════════════════════════════════════════
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                30_000,   // minBufferMs: أدنى مخزون قبل البدء
                120_000,  // maxBufferMs: أقصى مخزون
                3_000,    // bufferForPlaybackMs: لا تبدأ إلا بعد 3 ثوان
                6_000     // bufferForPlaybackAfterRebufferMs: بعد التقطيع
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        // ✅ TrackSelector لتحسين الاستقرار ومنع التبديل المزعج
        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setAllowVideoMixedMimeTypeAdaptiveness(true)
                    .setAllowAudioMixedMimeTypeAdaptiveness(true)
                    .setForceHighestSupportedBitrate(false) // يسمح بالتكيف
            )
        }

        val player = ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setTrackSelector(trackSelector)
            .setHandleAudioBecomingNoisy(true)
            .build()

        // ═══════════════════════════════════════════════════════════
        //  ✅ إعداد LiveConfiguration: تنعيم السرعة بدل القفزات
        // ═══════════════════════════════════════════════════════════
        val mediaItem = MediaItem.fromUri(candidates.first())

        if (kind == StreamKind.LIVE) {
            val liveConfig = MediaItem.LiveConfiguration.Builder()
                .setTargetOffsetMs(3_000)     // تأخير 3 ثوان عن البث الحقيقي
                .setMinPlaybackSpeed(0.95f)   // 95% حد أدنى
                .setMaxPlaybackSpeed(1.05f)   // 105% حد أقصى
                .build()

            player.setMediaItem(mediaItem, startPositionMs)
            player.prepare()
            player.setLiveConfiguration(liveConfig)
        } else {
            player.setMediaItem(mediaItem, startPositionMs)
            player.prepare()
        }

        player.playWhenReady = true

        // ═══════════════════════════════════════════════════════════
        //  ✅ إدارة أخطاء ذكية: لا تعيد التهيئة عند كل تقطيع
        //  - بحد أقصى 3 محاولات
        //  - فترة تهدئة 30 ثانية قبل إعادة العد
        // ═══════════════════════════════════════════════════════════
        var errorCount = 0
        var lastErrorTime = 0L

        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                val now = System.currentTimeMillis()

                // صفّر العدّاد إذا مر أكثر من 30 ثانية على آخر خطأ
                if (now - lastErrorTime > 30_000) {
                    errorCount = 0
                }
                errorCount++
                lastErrorTime = now

                // بعد 3 أخطاء متتالية: توقف عن إعادة المحاولة
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
