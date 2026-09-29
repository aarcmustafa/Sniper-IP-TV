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
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import androidx.navigation.NavHostController
import com.stitten.stitteniptv.data.ErrorLogger
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
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var retryCount by remember { mutableStateOf(0) }
    var tracks by remember { mutableStateOf<Tracks?>(null) }
    var showTrackMenu by remember { mutableStateOf(false) }

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
        val cache = CacheManager.get(ctx)
        val cacheFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(DefaultHttpDataSource.Factory())
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                settingsMgr.bufferMs,
                settingsMgr.bufferMs * 3,
                2500,
                5000
            ).build()

        val mediaSource = ProgressiveMediaSource.Factory(cacheFactory)
            .createMediaSource(MediaItem.fromUri(url))

        val player = ExoPlayer.Builder(ctx)
            .setLoadControl(loadControl)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .build()

        player.setMediaSource(mediaSource)
        player.playWhenReady = true

        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                if (retryCount < 3) {
                    retryCount++
                    player.prepare()
                    player.play()
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
            openExternal(ctx, url)
            navController.popBackStack()
        } else {
            exoPlayer.prepare()
        }
    }

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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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

            TextButton(onClick = { openExternal(ctx, url) }) {
                Text("مشغل خارجي", color = Color.White, fontSize = 16.sp)
            }
        }

        if (showTrackMenu && tracks != null) {
            TrackMenu(
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
                        Button(onClick = { openExternal(ctx, url) }) {
                            Text("مشغل خارجي")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackMenu(tracks: Tracks, exoPlayer: ExoPlayer, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(0.5f)
            .padding(top = 80.dp)
            .align(Alignment.TopEnd),
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

private fun openExternal(ctx: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(Uri.parse(url), "video/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        ctx.startActivity(Intent.createChooser(intent, "فتح بواسطة"))
    } catch (_: Exception) { }
}
