package com.stitten.stitteniptv.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.stitten.stitteniptv.ui.theme.Radius
import com.stitten.stitteniptv.ui.theme.Spacing
import com.stitten.stitteniptv.ui.theme.TvLive
import com.stitten.stitteniptv.ui.theme.TvText
import kotlinx.coroutines.delay

/**
 * OSD احترافي فوق المشغل.
 * - يظهر عند أي ضغطة زر
 * - يختفي تلقائيًا بعد 4 ثوانٍ
 * - يدعم Lock لمنع الضغط بالخطأ
 */
@Composable
fun PlayerOSD(
    player: Player?,
    title: String,
    subtitle: String? = null,
    isLive: Boolean = false,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // ═══════════════════════════════════════════════════════════
    //  الحالة
    // ═══════════════════════════════════════════════════════════
    var visible by remember { mutableStateOf(true) }
    var locked by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }

    var currentPos by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var bufferedPos by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(player?.isPlaying == true) }
    var volume by remember { mutableStateOf(player?.volume ?: 1f) }

    // ═══════════════════════════════════════════════════════════
    //  مراقبة حالة Player
    // ═══════════════════════════════════════════════════════════
    LaunchedEffect(player) {
        while (player != null) {
            currentPos = player.currentPosition.coerceAtLeast(0L)
            duration = player.duration.coerceAtLeast(0L)
            bufferedPos = player.bufferedPosition.coerceAtLeast(0L)
            isPlaying = player.isPlaying
            volume = player.volume
            delay(500)
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  Auto-hide بعد 4 ثوان
    // ═══════════════════════════════════════════════════════════
    LaunchedEffect(visible, isPlaying, locked) {
        if (visible && isPlaying && !locked) {
            delay(4000)
            visible = false
            settingsOpen = false
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  استقبال أزرار الريموت
    // ═══════════════════════════════════════════════════════════
    Box(
        modifier = modifier
            .fillMaxSize()
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false

                if (!visible) {
                    visible = true
                    return@onKeyEvent true
                }

                if (locked) {
                    if (event.key == Key.Back || event.key == Key.Escape) {
                        locked = false
                        return@onKeyEvent true
                    }
                    return@onKeyEvent true
                }

                when (event.key) {
                    Key.DirectionCenter, Key.Enter,
                    Key.NumPadEnter, Key.Spacebar -> {
                        if (player?.isPlaying == true) player.pause() else player?.play()
                        visible = true
                        true
                    }
                    Key.DirectionLeft -> {
                        if (!isLive && player != null) {
                            player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0L))
                        }
                        visible = true
                        true
                    }
                    Key.DirectionRight -> {
                        if (!isLive && player != null) {
                            val max = if (player.duration > 0) player.duration else Long.MAX_VALUE
                            player.seekTo((player.currentPosition + 10_000).coerceAtMost(max))
                        }
                        visible = true
                        true
                    }
                    Key.DirectionUp -> {
                        player?.volume = (volume + 0.1f).coerceAtMost(1f)
                        visible = true
                        true
                    }
                    Key.DirectionDown -> {
                        player?.volume = (volume - 0.1f).coerceAtLeast(0f)
                        visible = true
                        true
                    }
                    Key.Back, Key.Escape -> {
                        onClose()
                        true
                    }
                    else -> false
                }
            }
    ) {
        // ═══════════════════════════════════════════════════════
        //  OSD
        // ═══════════════════════════════════════════════════════
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 8 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 8 }),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {

                // ─── تدرج علوي ───
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.85f),
                                    Color.Transparent,
                                )
                            )
                        )
                )

                // ─── تدرج سفلي ───
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.92f),
                                )
                            )
                        )
                )

                // ─── Top Bar ───
                OSDTopBar(
                    title = title,
                    subtitle = subtitle,
                    isLive = isLive,
                    onClose = onClose,
                    modifier = Modifier.align(Alignment.TopStart),
                )

                // ─── Center Controls ───
                OSDCenterControls(
                    player = player,
                    isLive = isLive,
                    locked = locked,
                    modifier = Modifier.align(Alignment.Center),
                )

                // ─── Bottom Bar ───
                OSDBottomBar(
                    player = player,
                    isLive = isLive,
                    currentPos = currentPos,
                    duration = duration,
                    bufferedPos = bufferedPos,
                    volume = volume,
                    locked = locked,
                    settingsOpen = settingsOpen,
                    onToggleLock = { locked = !locked; visible = true },
                    onToggleSettings = { settingsOpen = !settingsOpen },
                    modifier = Modifier.align(Alignment.BottomStart),
                )
            }
        }

        // ═══════════════════════════════════════════════════════
        //  شاشة القفل
        // ═══════════════════════════════════════════════════════
        if (locked && visible) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(Spacing.xxl)
                    .clip(RoundedCornerShape(Radius.md))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = TvLive,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "مقفل — اضغط ← للفتح",
                        color = TvText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
