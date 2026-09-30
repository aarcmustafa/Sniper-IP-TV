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
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import androidx.navigation.NavHostController
import com.stitten.stitteniptv.data.ErrorLogger
import com.stitten.stitteniptv.data.ExternalPlayerManager
import com.stitten.stitteniptv.data.HttpClientProvider
import com.stitten.stitteniptv.data.PlayerSettingsManager
import com.stitten.stitteniptv.data.PrefsManager
import com.stitten.stitteniptv.data.WatchHistoryManager
import com.stitten.stitteniptv.database.entity.WatchHistoryEntity
import com.stitten.stitteniptv.player.CacheManager
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
    var showExternalMenu by remember { mutableStateOf(false) }

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

    val exoPlayer = remember {
        // Cache ديناميكي: 30 MB في Lite / 200 MB عادي
        val cache = CacheManager.get(ctx, liteMode)

        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(HttpClientProvider.getUserAgent())
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(if (liteMode) 60_000 else 30_000)
            .setReadTimeoutMs(if (liteMode) 120_000 else 60_000)

        val cacheFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(httpFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        // Buffer ديناميكي
        val userBufferMs = if (liteMode) {
            settingsMgr.bufferMs.coerceAtLeast(15_000)
        } else {
            settingsMgr.bufferMs.coerceAtLeast(30_000)
        }
        val minBuffer = userBufferMs
        val maxBuffer = if (liteMode) userBufferMs * 2 else userBufferMs * 3
        val playbackBuffer = if (liteMode) 2000 else 1500
        val rebufferBuffer = if (liteMode) 4000 else 3000

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                minBuffer,
                maxBuffer,
                playbackBuffer,
                rebufferBuffer
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .setBackBuffer(if (liteMode) 10_000 else 30_000, true)
            .build()

        // DefaultMediaSourceFactory يدعم: MP4, TS, HLS, DASH, MKV تلقائياً
        val mediaSourceFactory = DefaultMediaSourceFactory(cacheFactory)
        val mediaSource = mediaSourceFactory
            .createMediaSource(MediaItem.fromUri(url))

        val player = ExoPlayer.Builder(ctx)
            .setLoadControl(loadControl)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        player.setMediaSource(mediaSource)
        player.playWhenReady = true

        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                if (retryCount < 3) {
                    retryCount++
                    scope.launch {
                        delay(1000L * retryCount)
                        player.prepare()
                        player.play()
                    }
                } else {
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
                if (state == Player.STATE_ENDED) {
                    scope.launch { historyMgr.delete(url) }
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

    // حفظ موضع التوقف كل 5 ثوانٍ
    LaunchedEffect(Unit) {
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

    // استئناف الموضع المحفوظ
    LaunchedEffect(Unit) {
        delay(1500)
        val saved = historyMgr.getById(url)
        if (saved != null && saved.positionMs > 5000) {
            exoPlayer.seekTo(saved.positionMs)
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
            Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))

            // PiP: معطّل في Lite Mode لتوفير الموارد
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

            TextButton(onClick = {
                launchExternal(ctx, url, settingsMgr)
            }) {
                Text("مشغل خارجي", color = Color.White, fontSize = 16.sp)
            }
        }

        if (showTrackMenu && tracks != null) {
            TrackMenuOverlay(
                tracks = tracks!!,
                exoPlayer = exoPlayer,
                onDismiss = { showTrackMenu = false }
            )
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
                            exoPlayer.prepare()
                            exoPlayer.play()
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

@Composable
private fun TrackMenuOverlay(
    tracks: Tracks,
    exoPlayer: ExoPlayer,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 80.dp, end = 16.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .padding(8.dp),
            color = Color(0xEE161B22)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "المسارات",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))

                tracks.groups.forEach { group ->
                    if (group.type == C.TRACK_TYPE_AUDIO ||
                        group.type == C.TRACK_TYPE_TEXT
                    ) {
                        val type = if (group.type == C.TRACK_TYPE_AUDIO)
                            "🎵 صوت"
                        else
                            "💬 ترجمة"

                        Text(type, color = Color.Gray, fontSize = 14.sp)

                        for (i in 0 until group.length) {
                            val format = group.getTrackFormat(i)
                            val label = format.label
                                ?: format.language
                                ?: "مسار $i"
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
                                                    TrackSelectionOverride(
                                                        group.mediaTrackGroup, i
                                                    )
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
}

private fun launchExternal(
    ctx: Context,
    url: String,
    settingsMgr: PlayerSettingsManager
) {
    val preferred = settingsMgr.externalPlayerPackage
    if (preferred.isBlank()) {
        ExternalPlayerManager.launchWithChooser(ctx, url)
    } else {
        val success = ExternalPlayerManager.launchInPackage(ctx, url, preferred)
        if (!success) {
            ExternalPlayerManager.launchWithChooser(ctx, url)
        }
    }
}
