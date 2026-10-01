package com.stitten.stitteniptv.ui.screens

import com.stitten.stitteniptv.data.ChannelRepository
import com.stitten.stitteniptv.data.ErrorLogger
import com.stitten.stitteniptv.data.FavoritesSyncManager
import com.stitten.stitteniptv.data.ParentalControlManager
import com.stitten.stitteniptv.data.PlayerSettingsManager
import com.stitten.stitteniptv.data.SourceManager
import com.stitten.stitteniptv.data.WatchHistoryManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PlayerEntryPoint {
    fun watchHistoryManager(): WatchHistoryManager
    fun errorLogger(): ErrorLogger
    fun playerSettingsManager(): PlayerSettingsManager
    fun parentalControlManager(): ParentalControlManager
    fun sourceManager(): SourceManager
    fun favoritesSyncManager(): FavoritesSyncManager
    fun channelRepository(): ChannelRepository
}

    LaunchedEffect(Unit) {
        exoPlayer.prepare()
    }

    // حفظ موضع التوقف (لـ VOD فقط)
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

    // استئناف الموضع
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

        // ============ PlayerView ============
        AndroidView(
            factory = { c ->
                PlayerView(c).apply {
                    player = exoPlayer
                    useController = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    setControllerShowTimeoutMs(4000)
                    setControllerAutoShow(true)
                    setControllerHideDuringAds(false)
                    resizeMode = when (aspectRatioMode) {
                        0 -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        1 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        2 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                }
            },
            update = { view ->
                view.resizeMode = when (aspectRatioMode) {
                    0 -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    1 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    2 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // ============ الشريط العلوي ============
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

            // زر Aspect Ratio
            IconButton(onClick = {
                aspectRatioMode = (aspectRatioMode + 1) % 3
            }) {
                Icon(
                    Icons.Default.AspectRatio,
                    contentDescription = "أبعاد الشاشة",
                    tint = Color.White
                )
            }

            // زر الترجمة
            IconButton(onClick = { showTrackMenu = !showTrackMenu }) {
                Icon(
                    Icons.Default.Subtitles,
                    contentDescription = "ترجمات",
                    tint = Color.White
                )
            }

            // زر إعادة التشغيل
            IconButton(onClick = {
                retryCount = 0
                errorMsg = null
                exoPlayer.seekTo(exoPlayer.currentPosition)
                exoPlayer.prepare()
                exoPlayer.play()
            }) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "إعادة",
                    tint = Color.White
                )
            }

            // PiP (للبث فقط)
            if (!liteMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                IconButton(onClick = {
                    val activity = ctx as? android.app.Activity
                    activity?.enterPictureInPictureMode(
                        PictureInPictureParams.Builder()
                            .setAspectRatio(Rational(16, 9)).build()
                    )
                }) {
                    Text("PiP", color = Color.White, fontSize = 14.sp)
                }
            }
        }

        // ============ قائمة المسارات ============
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

        // ============ رسالة الخطأ ============
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
                        Button(onClick = {
                            val preferred = settingsMgr.externalPlayerPackage
                            if (preferred.isBlank()) {
                                ExternalPlayerManager.launchWithChooser(ctx, url)
                            } else {
                                ExternalPlayerManager.launchInPackage(ctx, url, preferred)
                            }
                        }) { Text("مشغل خارجي") }
                    }
                }
            }
        }
    }
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
