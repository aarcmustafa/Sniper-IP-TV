package com.stitten.stitteniptv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.stitten.stitteniptv.database.entity.WatchHistoryEntity
import com.stitten.stitteniptv.ui.theme.AppIcons
import com.stitten.stitteniptv.ui.theme.Radius
import com.stitten.stitteniptv.ui.theme.Spacing
import com.stitten.stitteniptv.ui.theme.TvLive
import com.stitten.stitteniptv.ui.theme.TvPrimary
import com.stitten.stitteniptv.ui.theme.TvSuccess
import com.stitten.stitteniptv.ui.theme.TvText
import com.stitten.stitteniptv.ui.theme.TvTextSecondary
import com.stitten.stitteniptv.ui.theme.TvWarning

/**
 * قسم "متابعة المشاهدة" — نسخة احترافية:
 *  - شارة نوع المحتوى مع أيقونة ولون
 *  - نسبة التقدم الظاهرة على الشارة
 *  - شريط تقدم أنيق مع توهج
 *  - الوقت المتبقي بصيغة نظيفة (س/د)
 *  - زر حذف دائري أنيق
 */
@Composable
fun ContinueWatchingSection(
    items: List<WatchHistoryEntity>,
    onItemClick: (WatchHistoryEntity) -> Unit,
    onRemoveItem: (WatchHistoryEntity) -> Unit,
) {
    if (items.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth()) {

        SectionHeader(
            title = "متابعة المشاهدة",
            subtitle = "أكمل من حيث توقفت",
            icon = Icons.Default.PlayArrow,
            count = items.size,
        )

        Spacer(Modifier.height(Spacing.md))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
            contentPadding = PaddingValues(horizontal = Spacing.xxl),
        ) {
            items(items, key = { "continue_${it.contentId}" }) { item ->
                ContinueWatchingCard(
                    item = item,
                    onClick = { onItemClick(item) },
                    onRemove = { onRemoveItem(item) },
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  بطاقة "متابعة المشاهدة"
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ContinueWatchingCard(
    item: WatchHistoryEntity,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    // ─── حساب التقدم ───
    val progress = if (item.durationMs > 0) {
        (item.positionMs.toFloat() / item.durationMs).coerceIn(0f, 1f)
    } else 0f

    val progressPercent = (progress * 100).toInt()

    val remainingMs = (item.durationMs - item.positionMs).coerceAtLeast(0)
    val remainingText = formatRemainingTime(remainingMs)

    // ─── تحديد نوع المحتوى واللون ───
    val contentType = item.contentType.uppercase()
    val meta = when {
        contentType.contains("LIVE") -> ContentMeta(
            icon = AppIcons.LiveTv,
            label = "بث مباشر",
            accent = TvLive,
        )
        contentType.contains("SERIES") || contentType.contains("EPISODE") -> ContentMeta(
            icon = AppIcons.Series,
            label = "حلقة",
            accent = TvPrimary,
        )
        else -> ContentMeta(
            icon = AppIcons.Movies,
            label = "فيلم",
            accent = TvPrimary,
        )
    }

    Box(
        modifier = Modifier
            .width(300.dp)
            .height(180.dp)
            .clip(RoundedCornerShape(Radius.lg))
            .tvFocusable(onClick = onClick)
    ) {
        // ═══════════════════════════════════════════════════════
        //  صورة الخلفية أو Placeholder
        // ═══════════════════════════════════════════════════════
        if (item.poster.isNotBlank()) {
            AsyncImage(
                model = item.poster,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                meta.accent.copy(alpha = 0.5f),
                                Color(0xFF0D1117),
                            )
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = meta.icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(48.dp),
                )
            }
        }

        // ═══════════════════════════════════════════════════════
        //  طبقة تعتيم (Gradient سفلية)
        // ═══════════════════════════════════════════════════════
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.05f),
                        0.5f to Color.Black.copy(alpha = 0.35f),
                        1f to Color.Black.copy(alpha = 0.92f),
                    )
                )
        )

        // ═══════════════════════════════════════════════════════
        //  زر الإزالة (أعلى اليمين)
        // ═══════════════════════════════════════════════════════
        RemoveButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(Spacing.sm),
        )

        // ═══════════════════════════════════════════════════════
        //  شارة نوع المحتوى (أعلى اليسار)
        // ═══════════════════════════════════════════════════════
        ContentTypeBadge(
            meta = meta,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(Spacing.sm),
        )

        // ═══════════════════════════════════════════════════════
        //  معلومات أسفل البطاقة
        // ═══════════════════════════════════════════════════════
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(
                    start = Spacing.md,
                    end = Spacing.md,
                    bottom = Spacing.md + 6.dp,
                ),
        ) {
            // ─── العنوان ───
            Text(
                text = item.title,
                color = TvText,
                fontSize = 15.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(Spacing.sm))

            // ─── الوقت المتبقي + النسبة ───
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = meta.accent,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = remainingText,
                    color = TvTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )

                if (progressPercent in 1..99) {
                    Text(
                        text = "·",
                        color = TvTextSecondary,
                        fontSize = 12.sp,
                    )
                    Text(
                        text = "$progressPercent%",
                        color = meta.accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        // ═══════════════════════════════════════════════════════
        //  شريط التقدم (أسفل البطاقة)
        // ═══════════════════════════════════════════════════════
        if (progress > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.12f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(4.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    meta.accent.copy(alpha = 0.9f),
                                    meta.accent,
                                )
                            )
                        )
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  مكونات داخلية
// ═══════════════════════════════════════════════════════════════

private data class ContentMeta(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val accent: Color,
)

@Composable
private fun ContentTypeBadge(
    meta: ContentMeta,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.sm))
            .background(Color.Black.copy(alpha = 0.65f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = meta.icon,
            contentDescription = null,
            tint = meta.accent,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = meta.label,
            color = TvText,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun RemoveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.6f))
            .tvFocusable(
                style = FocusStyle.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "إزالة",
            tint = Color.White,
            modifier = Modifier.size(16.dp),
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  صيغة الوقت المتبقي
// ═══════════════════════════════════════════════════════════════

private fun formatRemainingTime(ms: Long): String {
    if (ms <= 0) return "أوشك على الانتهاء"

    val totalSeconds = (ms / 1000).toInt()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60

    return when {
        hours > 0 && minutes > 0 -> "متبقي $hours س $minutes د"
        hours > 0 -> "متبقي $hours ساعة"
        minutes > 0 -> "متبقي $minutes دقيقة"
        totalSeconds > 0 -> "متبقي أقل من دقيقة"
        else -> "أوشك على الانتهاء"
    }
}
