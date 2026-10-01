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
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import androidx.navigation.NavHostController
import com.stitten.stitteniptv.data.ContentRepository
import com.stitten.stitteniptv.data.ErrorLogger
import com.stitten.stitteniptv.data.ExternalPlayerManager
import com.stitten.stitteniptv.data.HttpClientProvider
import com.stitten.stitteniptv.data.PlayerSettingsManager
import com.stitten.stitteniptv.data.PrefsManager
import com.stitten.stitteniptv.data.WatchHistoryManager
import com.stitten.stitteniptv.database.entity.WatchHistoryEntity
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
    var resolvedUrl by remember { mutableStateOf(url) }
    var isResolving by remember { mutableStateOf(false) }

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

    // ============== حل 302 Redirect ==============
    LaunchedEffect(url) {
        if (isLive) {
            // تحقق أولاً من الـ Cache
            val cached = ContentRepository.getDirectUrl(url)
            if (cached != null) {
                resolvedUrl = cached
                return@LaunchedEffect
            }

            // حل الرابط
            isResolving = true
            try {
                val direct = HttpClientProvider.resolveRedirect(url)
                if (direct != url) {
                    ContentRepository.saveDirectUrl(url, direct)
                    resolvedUrl = direct
                }
            } catch (e: Exception) {
                // استخدام الرابط الأصلي
            }
            isResolving = false
        }
    }

    val exoPlayer = remember(resolvedUrl) {
        // ============== OkHttpDataSource للبث ==============
        val httpFactory = OkHttpDataSource.Factory(
            HttpClientProvider.streamingClient
        )
            .setUserAgent(HttpClientProvider.getUserAgent())
            .setDefaultRequestProperties(
                mapOf(
                    "Accept" to "*/*",
                    "Connection" to "keep-alive"
                )
            )

        // ============== Buffer ضخم ==============
        val minBufferMs = if (liteMode) 60_000 else 120_000    // 60s أو 120s
        val maxBufferMs = if (liteMode) 120_000 else 300_000   // 120s أو 300s
        val playbackBufferMs = 2500                             // ابدأ بعد 2.5 ثانية
        val rebufferBufferMs = 10_000                           // استأنف بعد 10 ثوانٍ

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                minBufferMs,
                maxBufferMs,
                playbackBufferMs,
                rebufferBufferMs
            )
            .setTargetBufferBytes(
                if (liteMode) 30 * 1024 * 1024  // 30 MB
                else 100 * 1024 * 1024            // 100 MB
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .setBackBuffer(30_000, true)
            .build()

        val mediaSourceFactory = DefaultMediaSourceFactory(httpFactory)
        val mediaSource = mediaSourceFactory.createMediaSource(
            MediaItem.fromUri(resolvedUrl)
        )

        val player = ExoPlayer.Builder(ctx)
            .setLoadControl(loadControl)
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
                if (retryCount < 3) {
                    retryCount++
                    val delayMs = when (retryCount) {
                        1 -> 2000L
                        2 -> 4000L
                        else -> 8000L
                    }
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
                    isReconnecting = false
                    errorMsg = "فشل التشغيل بعد 3 محاولات: ${error.message}"
                    scope.launch {
                        errorLogger.log(url, error.message ?: "unknown", error.errorCode)
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
                            delay(15_000)
                            if (player.playbackState == Player.STATE_READY && player.isPlaying) {
                                retryCount = 0
                            }
                        }
                    }
                    Player.STATE_ENDED -> {
                        scope.launch { historyMgr.delete(url) }
                    }
                }
            }
        })
        player
    }
