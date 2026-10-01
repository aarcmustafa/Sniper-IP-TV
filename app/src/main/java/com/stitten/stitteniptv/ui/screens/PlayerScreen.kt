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

            // زر التبديل بين HLS و TS يدوياً
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
                exoPlayer.prepare()
                exoPlayer.play()
                scope.launch {
                    delay(2000)
                    isReconnecting = false
                }
            }) {
                Icon(Icons.Default.Refresh, contentDescription = "إعادة التشغيل", tint = Color.White)
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

        if (isReconnecting) {
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
                    Text(
                        "🔄 جاري إعادة الاتصال...",
                        color = Color.White,
                        fontSize = 16.sp
                    )
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
                        }) { Text("إعادة إنشاء المشغل") }
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

@Composable
private fun TrackMenuOverlay(
    tracks: Tracks,
    exoPlayer: ExoPlayer,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(0.5f).padding(8.dp),
        color = Color(0xEE161B22)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("المسارات", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            tracks.groups.forEach { group ->
                if (group.type == C.TRACK_TYPE_AUDIO || group.type == C.TRACK_TYPE_TEXT) {
                    val type = if (group.type == C.TRACK_TYPE_AUDIO) "🎵 صوت" else "💬 ترجمة"
                    Text(type, color = Color.Gray, fontSize = 14.sp)
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        val label = format.label ?: format.language ?: "مسار $i"
                        val selected = group.isTrackSelected(i)
                        Text(
                            "${if (selected) "✓" else "○"} $label",
                            color = Color.White,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    exoPlayer.trackSelectionParameters =
                                        exoPlayer.trackSelectionParameters
                                            .buildUpon()
                                            .setOverrideForType(
                                                TrackSelectionOverride(group.mediaTrackGroup, i)
                                            ).build()
                                    onDismiss()
                                }
                        )
                    }
                }
            }
        }
    }
}

private fun launchExternal(ctx: Context, url: String, settingsMgr: PlayerSettingsManager) {
    val preferred = settingsMgr.externalPlayerPackage
    if (preferred.isBlank()) {
        ExternalPlayerManager.launchWithChooser(ctx, url)
    } else {
        val success = ExternalPlayerManager.launchInPackage(ctx, url, preferred)
        if (!success) ExternalPlayerManager.launchWithChooser(ctx, url)
    }
}
