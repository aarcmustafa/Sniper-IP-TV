package com.stitten.stitteniptv.ui.screens

import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Rational
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
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
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.navigation.NavHostController
import com.stitten.stitteniptv.data.AdaptiveBuffer
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
    var tracks by remember { mutableStateOf<Tracks?>(null) }
    var showTrackMenu by remember { mutableStateOf(false) }
    var aspectRatioMode by remember { mutableStateOf(0) }
    var retryCount by remember { mutableStateOf(0) }

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

    val exoPlayer = remember {
        // ========== مصدر البيانات ==========
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(HttpClientProvider.getUserAgent())
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(30_000)
            .setReadTimeoutMs(60_000)

        val dataSourceFactory = if (isLive) {
            httpFactory
        } else {
            val cache = CacheManager.get(ctx, liteMode)
            CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(httpFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        }

        // ========== Buffer تكيفي ==========
        val minBufferMs = if (isLive) {
            AdaptiveBuffer.getMinBufferMs(ctx, liteMode)
        } else {
            30_000
        }
        val maxBufferMs = if (isLive) {
            AdaptiveBuffer.getMaxBufferMs(ctx, liteMode)
        } else {
            60_000
        }

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                minBufferMs,
                maxBufferMs,
                AdaptiveBuffer.getPlaybackBufferMs(liteMode),
                AdaptiveBuffer.getRebufferBufferMs(liteMode)
            )
            .setTargetBufferBytes(
                AdaptiveBuffer.getTargetBufferBytes(liteMode)
            )
            .setPrioritizeTimeOverSizeThresholds(false)
            .setBackBuffer(AdaptiveBuffer.getBackBufferMs(liteMode), true)
            .build()

        // ========== LoadErrorHandlingPolicy (10 محاولات) ==========
        val loadErrorPolicy = object : DefaultLoadErrorHandlingPolicy(10) {
            override fun getRetryDelayMsFor(
                loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo
            ): Long {
                return (loadErrorInfo.errorCount * 500L).coerceAtMost(5000L)
            }

            override fun getMinimumLoadableRetryCount(dataType: Int): Int {
                return 10
            }
        }

        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
            .setLoadErrorHandlingPolicy(loadErrorPolicy)

        // ========== Renderers (فك تشفير HW/SW) ==========
        val renderersFactory = DefaultRenderersFactory(ctx)
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(
                DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
            )
            .forceEnableMediaCodecAsynchronousQueueing()

        // ========== Track Selector ==========
        val trackSelector = DefaultTrackSelector(ctx).apply {
            setParameters(
                buildUponParameters()
                    .setPreferredAudioLanguage(settingsMgr.preferredAudioLanguage)
                    .setPreferredTextLanguage(settingsMgr.preferredSubtitleLanguage)
                    .setExceedRendererCapabilitiesIfNecessary(true)
                    .setExceedVideoConstraintsIfNecessary(true)
                    .setAllowVideoMixedMimeTypeAdaptiveness(true)
                    .setAllowVideoNonSeamlessAdaptiveness(true)
                    .setForceHighestSupportedBitrate(false)
                    .build()
            )
        }

        // ========== ExoPlayer ==========
        val player = ExoPlayer.Builder(ctx, renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(mediaSourceFactory)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT)
            .build()

        val mediaSource = mediaSourceFactory.createMediaSource(
            MediaItem.fromUri(url)
        )

        player.setMediaSource(mediaSource)
        player.playWhenReady = true

        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                if (retryCount < 3) {
                    retryCount++
                    scope.launch {
                        delay(2000L * retryCount)
                        player.prepare()
                        player.play()
                    }
                } else {
                    errorMsg = "تعذّر التشغيل: ${error.message}"
                    scope.launch {
                        errorLogger.log(url, error.message ?: "unknown", error.errorCode)
                    }
                }
            }

            override fun onTracksChanged(tracks1: Tracks) {
                tracks = tracks1
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    retryCount = 0
                }
                if (state == Player.STATE_ENDED) {
                    scope.launch { historyMgr.delete(url) }
                }
            }
        })
        player
    }
    
// ============ قائمة المسارات ============
@Composable
private fun TrackMenuOverlay(
    tracks: Tracks,
    exoPlayer: ExoPlayer,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(0.55f).padding(8.dp),
        color = Color(0xEE161B22),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .heightIn(max = 500.dp)
        ) {
            Text(
                "🎛️ المسارات",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            var hasAudio = false
            var hasText = false

            tracks.groups.forEach { group ->
                if (group.type == C.TRACK_TYPE_AUDIO) hasAudio = true
                if (group.type == C.TRACK_TYPE_TEXT) hasText = true
            }

            if (!hasAudio && !hasText) {
                Text(
                    "لا توجد مسارات إضافية",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }

            tracks.groups.forEach { group ->
                if (group.type == C.TRACK_TYPE_AUDIO ||
                    group.type == C.TRACK_TYPE_TEXT
                ) {
                    val typeLabel = if (group.type == C.TRACK_TYPE_AUDIO)
                        "🎵 الصوت" else "💬 الترجمة"

                    Text(
                        typeLabel,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))

                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        val label = format.label
                            ?: format.language
                            ?: "مسار $i"
                        val selected = group.isTrackSelected(i)

                        TrackItem(
                            label = label,
                            selected = selected,
                            onClick = {
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
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun TrackItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) Color(0xFF58A6FF) else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            )
            .background(
                color = if (isFocused) Color(0xFF21262D) else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            )
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter)
                ) {
                    onClick()
                    true
                } else false
            }
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (selected) "●" else "○",
            color = if (selected) Color(0xFF238636) else Color.Gray,
            fontSize = 16.sp
        )
        Spacer(Modifier.width(10.dp))
        Text(
            label,
            color = Color.White,
            fontSize = 14.sp,
            maxLines = 1
        )
    }
}
