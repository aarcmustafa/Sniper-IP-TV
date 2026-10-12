package com.stitten.stitteniptv.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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

// ═══════════════════════════════════════════════════════════════
//  Bottom Bar
// ═══════════════════════════════════════════════════════════════

@Composable
internal fun OSDBottomBar(
    player: Player?,
    isLive: Boolean,
    currentPos: Long,
    duration: Long,
    bufferedPos: Long,
    volume: Float,
    locked: Boolean,
    settingsOpen: Boolean,
    onToggleLock: () -> Unit,
    onToggleSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = Spacing.xxl,
                vertical = Spacing.xl,
            ),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        // ─── شريط التقدم ───
        if (!isLive && duration > 0) {
            OSDProgressBar(
                currentPos = currentPos,
                duration = duration,
                bufferedPos = bufferedPos,
            )
        }

        // ─── صف الأزرار ───
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            // الوقت أو LIVE
            if (!isLive && duration > 0) {
                Text(
                    text = "${formatTime(currentPos)} / ${formatTime(duration)}",
                    color = TvText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.width(Spacing.md))
            } else if (isLive) {
                Text(
                    text = "بث مباشر",
                    color = TvLive,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(Spacing.md))
            }

            // الصوت (للفيديو فقط)
            if (!isLive) {
                OSDIconButton(
                    icon = if (volume <= 0f) Icons.Default.VolumeOff
                    else Icons.Default.VolumeUp,
                    contentDescription = "الصوت",
                    onClick = {
                        player?.volume = if (volume > 0f) 0f else 1f
                    },
                )
            }

            // الترجمة (للفيديو فقط)
            if (!isLive) {
                OSDIconButton(
                    icon = Icons.Default.ClosedCaption,
                    contentDescription = "الترجمات",
                    onClick = { /* TODO */ },
                )
            }

            // المسار الصوتي
            OSDIconButton(
                icon = Icons.Default.MusicNote,
                contentDescription = "المسار الصوتي",
                onClick = { /* TODO */ },
            )

            // الإعدادات
            OSDIconButton(
                icon = Icons.Default.Settings,
                contentDescription = "الإعدادات",
                isSelected = settingsOpen,
                onClick = onToggleSettings,
            )

            Spacer(Modifier.weight(1f))

            // القفل
            OSDIconButton(
                icon = if (locked) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = if (locked) "فتح القفل" else "قفل",
                isSelected = locked,
                onClick = onToggleLock,
            )

            // المعلومات
            OSDIconButton(
                icon = Icons.Default.Info,
                contentDescription = "معلومات",
                onClick = { /* TODO */ },
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  Progress Bar
// ═══════════════════════════════════════════════════════════════

@Composable
private fun OSDProgressBar(
    currentPos: Long,
    duration: Long,
    bufferedPos: Long,
) {
    val progress = (currentPos.toFloat() / duration).coerceIn(0f, 1f)
    val bufferedProgress = (bufferedPos.toFloat() / duration).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        // الخلفية
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(Radius.pill))
                .background(Color.White.copy(alpha = 0.2f))
        )

        // Buffered
        Box(
            modifier = Modifier
                .fillMaxWidth(bufferedProgress)
                .height(6.dp)
                .clip(RoundedCornerShape(Radius.pill))
                .background(Color.White.copy(alpha = 0.35f))
        )

        // Progress
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(6.dp)
                .clip(RoundedCornerShape(Radius.pill))
                .background(TvPrimary)
        )

        // المؤشر
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(6.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  زر دائري عام (يُستخدم في Top + Bottom)
// ═══════════════════════════════════════════════════════════════

@Composable
internal fun OSDIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    isSelected: Boolean = false,
) {
    val bg = if (isSelected) TvPrimary else Color.Black.copy(alpha = 0.55f)

    Box(
        modifier = Modifier
            .size(48.dp)
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
            modifier = Modifier.size(22.dp),
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  تنسيق الوقت
// ═══════════════════════════════════════════════════════════════

internal fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"

    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        "%02d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
