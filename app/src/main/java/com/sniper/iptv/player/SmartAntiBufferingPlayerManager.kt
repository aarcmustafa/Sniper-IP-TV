package com.sniper.iptv.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector

class SmartAntiBufferingPlayerManager(context: Context, private val onStatusChanged: (String) -> Unit) {
    
    // متطلبات السيرفرات: User-Agent محترف + تتبع التحويلات (Redirects) + مهلات اتصال
    private val dataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) SniperIPTV/Pro")
        .setConnectTimeoutMs(15000)
        .setReadTimeoutMs(15000)
        .setAllowCrossProtocolRedirects(true)

    private val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(10000, 50000, 1500, 3000)
        .build()

    private val trackSelector = DefaultTrackSelector(context)

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
        .setLoadControl(loadControl)
        .setTrackSelector(trackSelector)
        .build().apply {
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    super.onPlayerError(error)
                    handleAutoRetry()
                }
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> onStatusChanged("جاري التخزين المؤقت...")
                        Player.STATE_READY -> onStatusChanged("")
                        Player.STATE_ENDED -> onStatusChanged("انتهى البث")
                        else -> {}
                    }
                }
            })
        }

    private var currentUrl: String = ""
    private var retryCount = 0
    private val maxRetries = 3

    fun applyAntiBuffering(lowQuality: Boolean) {
        val builder = trackSelector.buildUponParameters()
        if (lowQuality) builder.setMaxVideoSize(640, 360) else builder.clearVideoSizeConstraints()
        trackSelector.setParameters(builder)
    }

    fun play(url: String) {
        currentUrl = url
        retryCount = 0
        executePlay()
    }

    private fun executePlay() {
        if (currentUrl.isBlank()) return
        val mediaItem = MediaItem.fromUri(currentUrl)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
    }

    private fun handleAutoRetry() {
        if (retryCount < maxRetries) {
            retryCount++
            onStatusChanged("التقطّع مكتشف، إعادة محاولة الاتصال ($retryCount/$maxRetries)...")
            Handler(Looper.getMainLooper()).postDelayed({
                if (currentUrl.isNotBlank()) {
                    player.prepare()
                    player.play()
                }
            }, (retryCount * 1500L))
        } else {
            onStatusChanged("فشل الاتصال بالسيرفر. تحقق من الرابط أو اشتراكك.")
        }
    }

    fun release() { player.release() }
}

