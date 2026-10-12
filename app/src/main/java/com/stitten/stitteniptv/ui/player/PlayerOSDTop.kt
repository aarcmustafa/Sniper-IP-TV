package com.stitten.stitteniptv.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.stitten.stitteniptv.ui.components.FocusStyle
import com.stitten.stitteniptv.ui.components.tvFocusable
import com.stitten.stitteniptv.ui.theme.Radius
import com.stitten.stitteniptv.ui.theme.Spacing
import com.stitten.stitteniptv.ui.theme.TvLive
import com.stitten.stitteniptv.ui.theme.TvPrimary
import com.stitten.stitteniptv.ui.theme.TvText
import com.stitten.stitteniptv.ui.theme.TvTextSecondary

// ═══════════════════════════════════════════════════════════════
//  Top Bar
// ═══════════════════════════════════════════════════════════════

@Composable
internal fun OSDTopBar(
    title: String,
    subtitle: String?,
    isLive: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = Spacing.xxl,
                vertical = Spacing.xl,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        OSDIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "رجوع",
            onClick = onClose,
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TvText,
                fontSize = 20.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = TvTextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (isLive) LiveIndicator()
    }
}

@Composable
private fun LiveIndicator() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.sm))
            .background(TvLive)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
        Text(
            text = "LIVE",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.8.sp,
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  Center Controls
// ═══════════════════════════════════════════════════════════════

@Composable
internal fun OSDCenterControls(
    player: Player?,
    isLive: Boolean,
    locked: Boolean,
    modifier: Modifier = Modifier,
) {
    if (locked) return

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xxl),
    ) {
        if (!isLive) {
            OSDCenterButton(
                icon = Icons.Default.Replay10,
                contentDescription = "تأخير 10 ثوان",
                size = 56.dp,
                onClick = {
                    player?.let {
                        it.seekTo((it.currentPosition - 10_000).coerceAtLeast(0L))
                    }
                },
            )
        }

        OSDCenterButton(
            icon = if (player?.isPlaying == true) Icons.Default.Pause
            else Icons.Default.PlayArrow,
            contentDescription = if (player?.isPlaying == true) "إيقاف" else "تشغيل",
            size = 80.dp,
            isPrimary = true,
            onClick = {
                player?.let {
                    if (it.isPlaying) it.pause() else it.play()
                }
            },
        )

        if (!isLive) {
            OSDCenterButton(
                icon = Icons.Default.Forward10,
                contentDescription = "تقديم 10 ثوان",
                size = 56.dp,
                onClick = {
                    player?.let {
                        val max = if (it.duration > 0) it.duration else Long.MAX_VALUE
                        it.seekTo((it.currentPosition + 10_000).coerceAtMost(max))
                    }
                },
            )
        }
    }
}

@Composable
private fun OSDCenterButton(
    icon: ImageVector,
    contentDescription: String,
    size: Dp,
    isPrimary: Boolean = false,
    onClick: () -> Unit,
) {
    val bg = if (isPrimary) TvPrimary else Color.Black.copy(alpha = 0.6f)

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(bg)
            .tvFocusable(
                style = FocusStyle.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(size * 0.45f),
        )
    }
}
