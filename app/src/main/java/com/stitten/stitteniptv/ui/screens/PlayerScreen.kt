package com.stitten.stitteniptv.ui.screens

import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Rational
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.ui.PlayerView
import androidx.navigation.NavHostController
import com.stitten.stitteniptv.data.ErrorLogger
import com.stitten.stitteniptv.data.ExternalPlayerManager
import com.stitten.stitteniptv.data.HttpClientProvider
import com.stitten.stitteniptv.data.PlayerSettingsManager
import com.stitten.stitteniptv.data.PrefsManager
import com.stitten.stitteniptv.data.WatchHistoryManager
import com.stitten.stitteniptv.database.entity.WatchHistoryEntity
import com.stitten.stitteniptv.player.RetryDataSource
import com.stitten.stitteniptv.player.StreamMediaSourceFactory
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(
    navController: NavHostController,
    url: String,
    title: String
) {
    val ctx = LocalContext.current
    val prefs = remember { PrefsManager(ctx) }
    val liteMode = prefs.liteModeEnabled

    var errorMsg by remember { mutableStateOf<String?>(null) }
    var retryCount by remember { mutableStateOf(0) }
    var tracks by remember { mutableStateOf<Tracks?>(null) }
    var showTrackMenu by remember { mutableStateOf(false) }
    var isReconnecting by remember { mutableStateOf(false) }
    var useHls by remember { mutableStateOf(true) }
    var playerRecreated by remember { mutableStateOf(0) }

    val entryPoint = remember {
        EntryPointAccessors.fromApplication(
            ctx.applicationContext,
            PlayerEntryPoint::class.java
        )
    }
    val historyMgr: WatchHistoryManager = entryPoint.watchHistoryManager()
    val errorLogger: ErrorLogger = entryPoint.errorLogger()
    val settingsMgr: PlayerSettingsManager = entryPoint.playerSettingsManager()
    val scope = rememberCoroutineScope()

    val isLive = remember(url) {
        url.contains("/live/") || url.endsWith(".ts") || url.endsWith(".m3u8")
    }

    val exoPlayer = remember(playerRecreated, useHls) {
        // ============== DataSource ==============
        val okHttpSource = OkHttpDataSource.Factory(
            HttpClientProvider.streamingClient
        )
            .setUserAgent(HttpClientProvider.getUserAgent())
            .setDefaultRequestProperties(
                mapOf(
                    "Accept" to "*/*",
                    "Accept-Encoding" to "identity",
                    "Connection" to "keep-alive"
                )
            )

        // لف الـ OkHttpDataSource بـ RetryDataSource
        val dataSourceFactory = object : DataSource.Factory {
            override fun createDataSource(): DataSource {
                return RetryDataSource(
                    okHttpSource.createDataSource(),
                    maxRetries = 15,
                    retryDelayMs = 200
                )
            }
        }

        // ============== اختيار نوع المصدر ==============
        val mediaSource: MediaSource = if (isLive && useHls) {
            StreamMediaSourceFactory.create(url, dataSourceFactory)
        } else {
            StreamMediaSourceFactory.createProgressive(url, dataSourceFactory)
        }

        // ============== Buffer ضخم ==============
        val minBufferMs = if (liteMode) 120_000 else 180_000
        val maxBufferMs = if (liteMode) 240_000 else 360_000
        val playbackBufferMs = 3000
        val rebufferBufferMs = 15_000

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                minBufferMs,
                maxBufferMs,
                playbackBufferMs,
                rebufferBufferMs
            )
            .setTargetBufferBytes(
                if (liteMode) 40 * 1024 * 1024 else 150 * 1024 * 1024
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .setBackBuffer(60_000, true)
            .build()

        // ============== Load Error Handling: إعادة محاولة لا محدودة ==============
        val errorPolicy = object : DefaultLoadErrorHandlingPolicy(15) {
            override fun getRetryDelayMsFor(
                loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo
            ): Long {
                // تأخير تدريجي: 500ms، 1000ms، 1500ms...
                val count = loadErrorInfo.errorCount
                return (500L * count).coerceAtMost(5000L)
            }

            override fun getMinimumLoadableRetryCount(dataType: Int): Int {
                // إعادة محاولة لا محدودة تقريباً
                return 20
            }
        }

        val player = ExoPlayer.Builder(ctx)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(
                androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)
                    .setLoadErrorHandlingPolicy(errorPolicy)
            )
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setPauseAtEndOfMediaItems(false)
            .build()

        player.setMediaSource(mediaSource)
        player.playWhenReady = true

        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                // إذا فشل مع HLS → جرّب TS
                if (useHls && isLive && error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED) {
                    useHls = false
                    playerRecreated++
                    return
                }
                if (!useHls && isLive && error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED) {
                    useHls = true
                    playerRecreated++
                    return
                }

                if (retryCount < 5) {
                    retryCount++
                    val delayMs = (1500L * retryCount).coerceAtMost(10_000L)
                    isReconnecting = true
                    scope.launch {
                        delay(delayMs)
                        try {
                            player.prepare()
                            player.play()
                        } catch (e: Exception) { }
                        delay(2000)
                        isReconnecting = false
                    }
                } else {
                    // حاول إعادة إنشاء المشغل بالكامل
                    if (retryCount < 7) {
                        retryCount++
                        playerRecreated++
                    } else {
                        isReconnecting = false
                        errorMsg = "فشل التشغيل: ${error.message}"
                        scope.launch {
                            errorLogger.log(url, error.message ?: "unknown", error.errorCode)
                        }
                    }
                }
            }

            override fun onTracksChanged(tracks1: Tracks) {
                tracks = tracks1
            }

            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_READY -> {
                        isReconnecting = false
                        scope.launch {
                            delay(30_000)
                            if (player.playbackState == Player.STATE_READY && player.isPlaying) {
                                retryCount = 0
                            }
                        }
                    }
                    Player.STATE_ENDED -> {
                        // ✅ حالة مهمة: البث "انتهى" — أعد التشغيل من البداية
                        // هذا هو الحل الرئيسي لمشكلة Connection: close!
                        if (isLive) {
                            scope.launch {
                                delay(500)
                                try {
                                    player.seekTo(0)
                                    player.prepare()
                                    player.play()
                                } catch (e: Exception) { }
                            }
                        } else {
                            scope.launch { historyMgr.delete(url) }
                        }
                    }
                }
            }
        })
        player
    }
