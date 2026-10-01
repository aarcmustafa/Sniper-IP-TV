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
    var statusText by remember { mutableStateOf("") }

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
        isReconnecting = false

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

        val dataSourceFactory = object : DataSource.Factory {
            override fun createDataSource(): DataSource {
                return RetryDataSource(
                    okHttpSource.createDataSource(),
                    maxRetries = 20,
                    retryDelayMs = 200
                )
            }
        }

        val mediaSource: MediaSource = if (isLive && useHls) {
            StreamMediaSourceFactory.create(url, dataSourceFactory)
        } else {
            StreamMediaSourceFactory.createProgressive(url, dataSourceFactory)
        }

        val minBufferMs = if (liteMode) 120_000 else 180_000
        val maxBufferMs = if (liteMode) 240_000 else 360_000

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                minBufferMs,
                maxBufferMs,
                3000,
                15_000
            )
            .setTargetBufferBytes(
                if (liteMode) 40 * 1024 * 1024 else 150 * 1024 * 1024
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .setBackBuffer(60_000, true)
            .build()

        val errorPolicy = object : DefaultLoadErrorHandlingPolicy(20) {
            override fun getRetryDelayMsFor(
                loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo
            ): Long {
                val count = loadErrorInfo.errorCount
                return (500L * count).coerceAtMost(5000L)
            }

            override fun getMinimumLoadableRetryCount(dataType: Int): Int = 20
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
                if (useHls && isLive &&
                    error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED) {
                    useHls = false
                    playerRecreated++
                    return
                }
                if (!useHls && isLive &&
                    error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED) {
                    useHls = true
                    playerRecreated++
                    return
                }

                if (retryCount < 5) {
                    retryCount++
                    isReconnecting = true
                    scope.launch {
                        delay((1500L * retryCount).coerceAtMost(10_000L))
                        try {
                            player.prepare()
                            player.play()
                        } catch (e: Exception) { }
                        delay(2000)
                        isReconnecting = false
                    }
                } else {
                    retryCount++
                    playerRecreated++
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
                        if (isLive) {
                            // البث "انتهى" وهمياً (Connection: close)
                            scope.launch {
                                delay(300)
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
    
    // ============================================
    // ⭐ WATCHDOG — يفحص المشغل كل 3 ثوانٍ
    // ============================================
    LaunchedEffect(exoPlayer) {
        var idleCount = 0
        var bufferingCount = 0
        var consecutiveNoProgress = 0
        var lastPosition = 0L

        while (true) {
            delay(3000)
            if (!isLive) continue

            try {
                val state = exoPlayer.playbackState
                val isPlaying = exoPlayer.isPlaying
                val position = exoPlayer.currentPosition

                when (state) {
                    Player.STATE_IDLE -> {
                        idleCount++
                        statusText = "⏸ IDLE ($idleCount)"
                        // 4 دورات × 3 ثوانٍ = 12 ثانية في IDLE
                        if (idleCount >= 4) {
                            idleCount = 0
                            isReconnecting = true
                            playerRecreated++
                        }
                    }

                    Player.STATE_BUFFERING -> {
                        bufferingCount++
                        statusText = "⏳ BUFFERING ($bufferingCount)"
                        // 10 دورات × 3 ثوانٍ = 30 ثانية في BUFFERING
                        if (bufferingCount >= 10) {
                            bufferingCount = 0
                            isReconnecting = true
                            playerRecreated++
                        }
                    }

                    Player.STATE_READY -> {
                        if (isPlaying) {
                            idleCount = 0
                            bufferingCount = 0
                            statusText = ""

                            // كشف "الجمود" (يعمل لكن الموضع ثابت)
                            if (position == lastPosition && position > 0) {
                                consecutiveNoProgress++
                                if (consecutiveNoProgress >= 5) { // 15 ثانية
                                    consecutiveNoProgress = 0
                                    isReconnecting = true
                                    playerRecreated++
                                }
                            } else {
                                consecutiveNoProgress = 0
                            }
                            lastPosition = position
                        } else {
                            // READY لكن لا يعمل — حالة غريبة
                            idleCount++
                            if (idleCount >= 4) {
                                idleCount = 0
                                isReconnecting = true
                                playerRecreated++
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // في حال حدث خطأ في الفحص نفسه
                playerRecreated++
                break
            }
        }
    }

    LaunchedEffect(playerRecreated) {
        if (playerRecreated > 0) {
            delay(2000)
            isReconnecting = false
        }
    }

    LaunchedEffect(Unit) {
        if (!prefs.useInternalPlayer) {
            launchExternal(ctx, url, settingsMgr)
            navController.popBackStack()
        } else {
            exoPlayer.prepare()
        }
    }

    LaunchedEffect(Unit) {
        if (!isLive) {
            while (true) {
                delay(5000)
                val pos = exoPlayer.currentPosition
                val dur = exoPlayer.duration
                if (pos > 0 && dur > 0 && pos < dur - 3000) {
                    historyMgr.save(
                        WatchHistoryEntity(
                            contentId = url,
                            contentType = "VOD",
                            title = title,
                            poster = "",
                            url = url,
                            positionMs = pos,
                            durationMs = dur
                        )
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!isLive) {
            delay(1500)
            val saved = historyMgr.getById(url)
            if (saved != null && saved.positionMs > 5000) {
                exoPlayer.seekTo(saved.positionMs)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { c ->
                PlayerView(c).apply {
                    player = exoPlayer
                    useController = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    setControllerShowTimeoutMs(4000)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
            }
            Text(
                title,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(Modifier.weight(1f))

            TextButton(onClick = {
                useHls = !useHls
                playerRecreated++
            }) {
                Text(
                    if (useHls) "HLS" else "TS",
                    color = Color(0xFF58A6FF),
                    fontSize = 12.sp
                )
            }

            IconButton(onClick = {
                isReconnecting = true
                retryCount = 0
                playerRecreated++
            }) {
                Icon(Icons.Default.Refresh, contentDescription = "إعادة", tint = Color.White)
            }

            if (!liteMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                IconButton(onClick = {
                    val activity = ctx as? android.app.Activity
                    activity?.enterPictureInPictureMode(
                        PictureInPictureParams.Builder()
                            .setAspectRatio(Rational(16, 9)).build()
                    )
                }) {
                    Icon(Icons.Default.AspectRatio, contentDescription = "PiP", tint = Color.White)
                }
            }

            IconButton(onClick = { showTrackMenu = !showTrackMenu }) {
                Icon(Icons.Default.Subtitles, contentDescription = "ترجمات", tint = Color.White)
            }

            TextButton(onClick = { launchExternal(ctx, url, settingsMgr) }) {
                Text("مشغل خارجي", color = Color.White, fontSize = 16.sp)
            }
        }

        // شريط الحالة (للتشخيص)
        if (statusText.isNotEmpty() && isReconnecting) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color(0xCC000000), shape = MaterialTheme.shapes.medium)
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "🔄 جاري إعادة الاتصال...",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        if (statusText.isNotEmpty()) {
                            Text(
                                statusText,
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        if (showTrackMenu && tracks != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 80.dp, end = 16.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                TrackMenuOverlay(
                    tracks = tracks!!,
                    exoPlayer = exoPlayer,
                    onDismiss = { showTrackMenu = false }
                )
            }
        }

        errorMsg?.let { msg ->
            Card(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D))
            ) {
                Column(
                    Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(msg, color = Color.White, fontSize = 18.sp)
                    Spacer(Modifier.height(12.dp))
                    Row {
                        Button(onClick = {
                            errorMsg = null
                            retryCount = 0
                            playerRecreated++
                        }) { Text("إعادة المحاولة") }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { launchExternal(ctx, url, settingsMgr) }) {
                            Text("مشغل خارجي")
                        }
                    }
                }
            }
        }
    }
}
