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
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.stitten.stitteniptv.database.entity.WatchHistoryEntity
import com.stitten.stitteniptv.ui.theme.AppIcons

/**
 * قسم "متابعة المشاهدة" — تصميم محسّن مع:
 *  - أيقونة نوع المحتوى (LIVE/MOVIE/EPISODE)
 *  - شريط تقدم عريض
 *  - الوقت المتبقي بصيغة أنيقة
 *  - زر إزالة عند التركيز
 *  - خلفية متدرجة لقراءة النص
 */
@Composable
fun ContinueWatchingSection(
    items: List<WatchHistoryEntity>,
    onItemClick: (WatchHistoryEntity) -> Unit,
    onRemoveItem: (WatchHistoryEntity) -> Unit,
) {
    if (items.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth()) {

        // ═══════════════════════════════════════════════════════
        //  العنوان
        // ═══════════════════════════════════════════════════════
        SectionHeader(
            title = "متابعة المشاهدة",
            icon = Icons.Default.PlayArrow,
            count = items.size,
        )

        Spacer(Modifier.height(12.dp))

        // ═══════════════════════════════════════════════════════
        //  الصف الأفقي
        // ═══════════════════════════════════════════════════════
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 20.dp),
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
    // حساب النسبة والوقت المتبقي
    val progress = if (item.durationMs > 0) {
        (item.positionMs.toFloat() / item.durationMs).coerceIn(0f, 1f)
    } else 0f

    val minutesLeft = if (item.durationMs > item.positionMs) {
        ((item.durationMs - item.positionMs) / 60_000).toInt()
    } else 0

    // نوع المحتوى
    val contentType = item.contentType.uppercase()
    val (icon, label, accentColor) = when {
        contentType.contains("LIVE") -> Triple(
            AppIcons.LiveTv,
            "بث مباشر",
            Color(0xFFF85149),
        )
        contentType.contains("SERIES") || contentType.contains("EPISODE") -> Triple(
            AppIcons.Series,
            "حلقة",
            Color(0xFF8B5CF6),
        )
        else -> Triple(
            AppIcons.Movies,
            "فيلم",
            Color(0xFF1F6FEB),
        )
    }

    Box(
        modifier = Modifier
            .width(280.dp)
            .height(170.dp)
            .clip(RoundedCornerShape(14.dp))
            .tvFocusable(onClick = onClick)
    ) {
        // ═══════════════════════════════════════════════════════
        //  صورة الخلفية
        // ═══════════════════════════════════════════════════════
        if (item.poster.isNotBlank()) {
            AsyncImage(
                model = item.poster,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            // خلفية افتراضية بلون النوع
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.6f),
                                Color(0xFF0D1117),
                            )
                        )
                    )
            )
        }

        // ═══════════════════════════════════════════════════════
        //  طبقة تعتيم
        // ═══════════════════════════════════════════════════════
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.1f),
                            Color.Black.copy(alpha = 0.4f),
                            Color.Black.copy(alpha = 0.92f),
                        )
                    )
                )
        )

        // ═══════════════════════════════════════════════════════
        //  زر الإزالة (أعلى اليمين)
        // ═══════════════════════════════════════════════════════
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.7f))
                .tvFocusable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "إزالة",
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }

        // ═══════════════════════════════════════════════════════
        //  شارة النوع (أعلى اليسار)
        // ═══════════════════════════════════════════════════════
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(accentColor.copy(alpha = 0.9f))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        // ═══════════════════════════════════════════════════════
        //  المحتوى (أسفل البطاقة)
        // ═══════════════════════════════════════════════════════
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            // العنوان
            Text(
                text = item.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(6.dp))

            // المدة المتبقية
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = when {
                        minutesLeft > 60 -> "متبقي ${minutesLeft / 60}س ${minutesLeft % 60}د"
                        minutesLeft > 0 -> "متبقي $minutesLeft دقيقة"
                        else -> "أوشك على الانتهاء"
                    },
                    color = Color(0xFFD0D7DE),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        // ═══════════════════════════════════════════════════════
        //  شريط التقدم (أسفل البطاقة — عريض)
        // ═══════════════════════════════════════════════════════
        if (progress > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.15f))
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(4.dp),
                    color = accentColor,
                    trackColor = Color.Transparent,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  عنوان القسم الموحّد
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector,
    count: Int? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF1F6FEB),
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        if (count != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = "($count)",
                color = Color(0xFF8B949E),
                fontSize = 14.sp,
            )
        }
    }
}
