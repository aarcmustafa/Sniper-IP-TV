package com.stitten.stitteniptv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.stitten.stitteniptv.ui.theme.Radius
import com.stitten.stitteniptv.ui.theme.Spacing
import com.stitten.stitteniptv.ui.theme.TvBackground
import com.stitten.stitteniptv.ui.theme.TvGold
import com.stitten.stitteniptv.ui.theme.TvPrimary
import com.stitten.stitteniptv.ui.theme.TvSurfaceHigh
import com.stitten.stitteniptv.ui.theme.TvText
import com.stitten.stitteniptv.ui.theme.TvTextSecondary
import com.stitten.stitteniptv.ui.theme.TvTextTertiary

/**
 * بانر العنصر المميز في أعلى الشاشة الرئيسية.
 * تصميم احترافي بـ:
 *   - Gradients متعددة الطبقات
 *   - Badges (IMDb, السنة, الجودة)
 *   - أزرار Action بأيقونات
 *   - Gradient overlay من جهة النص
 */
@Composable
fun HeroBanner(
    title: String,
    posterUrl: String?,
    onPlay: () -> Unit,
    onDetails: () -> Unit,
    modifier: Modifier = Modifier,
    backdropUrl: String? = null,
    subtitle: String? = null,
    plot: String? = null,
    rating: String? = null,
    year: String? = null,
    quality: String? = null,      // "HD" / "4K" / "FHD"
    duration: String? = null,      // "2h 15m"
    primaryLabel: String = "تشغيل",
    secondaryLabel: String = "التفاصيل",
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(460.dp)
            .clip(RoundedCornerShape(Radius.lg))
    ) {
        // ═══════════════════════════════════════════════════════
        //  الصورة الخلفية
        // ═══════════════════════════════════════════════════════
        AsyncImage(
            model = backdropUrl ?: posterUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // ═══════════════════════════════════════════════════════
        //  طبقة تدرج سفلية (تحاكي Netflix)
        //  تبدأ شفافة في الأعلى، تنتهي بلون الخلفية نفسه
        // ═══════════════════════════════════════════════════════
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.4f to Color.Black.copy(alpha = 0.30f),
                        0.75f to Color.Black.copy(alpha = 0.75f),
                        1f to TvBackground,
                    )
                )
        )

        // ═══════════════════════════════════════════════════════
        //  طبقة تدرج جانبية (تعتم النص من جهة اليمين)
        // ═══════════════════════════════════════════════════════
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to TvBackground.copy(alpha = 0.85f),
                        0.35f to Color.Transparent,
                        1f to Color.Transparent,
                    )
                )
        )

        // ═══════════════════════════════════════════════════════
        //  المحتوى
        // ═══════════════════════════════════════════════════════
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.55f)
                .padding(start = 40.dp, bottom = 40.dp, end = 16.dp)
        ) {

            // ─── Badges العلوية ───
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!quality.isNullOrBlank()) {
                    QualityBadge(text = quality)
                }
                if (!year.isNullOrBlank()) {
                    MetadataBadge(text = year)
                }
                if (!duration.isNullOrBlank()) {
                    MetadataBadge(text = duration)
                }
                if (!rating.isNullOrBlank()) {
                    RatingBadge(rating = rating)
                }
            }

            Spacer(Modifier.height(Spacing.md))

            // ─── العنوان ───
            Text(
                text = title,
                color = TvText,
                fontSize = 42.sp,
                lineHeight = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            // ─── Subtitle (التصنيف) ───
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = subtitle,
                    color = TvTextSecondary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            // ─── الوصف ───
            if (!plot.isNullOrBlank()) {
                Spacer(Modifier.height(Spacing.md))
                Text(
                    text = plot,
                    color = TvTextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(Spacing.xxl))

            // ─── الأزرار ───
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HeroPrimaryButton(
                    label = primaryLabel,
                    onClick = onPlay,
                )
                HeroSecondaryButton(
                    label = secondaryLabel,
                    onClick = onDetails,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  Badges
// ═══════════════════════════════════════════════════════════════

@Composable
private fun QualityBadge(text: String) {
    Text(
        text = text,
        color = TvGold,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 0.8.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.xs))
            .background(TvGold.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
private fun MetadataBadge(text: String) {
    Text(
        text = text,
        color = TvTextSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.xs))
            .background(TvSurfaceHigh.copy(alpha = 0.7f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
private fun RatingBadge(rating: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.xs))
            .background(TvSurfaceHigh.copy(alpha = 0.7f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = TvGold,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = rating,
            color = TvText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  الأزرار
// ═══════════════════════════════════════════════════════════════

@Composable
private fun HeroPrimaryButton(
    label: String,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .tvFocusable(onClick = onClick)
            .clip(RoundedCornerShape(Radius.md))
            .background(TvPrimary)
            .padding(horizontal = 28.dp, vertical = 14.dp)
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun HeroSecondaryButton(
    label: String,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .tvFocusable(onClick = onClick)
            .clip(RoundedCornerShape(Radius.md))
            .background(TvSurfaceHigh.copy(alpha = 0.85f))
            .padding(horizontal = 28.dp, vertical = 14.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = TvText,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            color = TvText,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
