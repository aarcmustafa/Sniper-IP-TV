package com.stitten.stitteniptv.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
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
import com.stitten.stitteniptv.database.entity.SeriesEntity
import com.stitten.stitteniptv.ui.components.FocusStyle
import com.stitten.stitteniptv.ui.components.tvFocusable
import com.stitten.stitteniptv.ui.theme.Radius
import com.stitten.stitteniptv.ui.theme.Spacing
import com.stitten.stitteniptv.ui.theme.TvBackground
import com.stitten.stitteniptv.ui.theme.TvGold
import com.stitten.stitteniptv.ui.theme.TvSurfaceHigh
import com.stitten.stitteniptv.ui.theme.TvText
import com.stitten.stitteniptv.ui.theme.TvTextSecondary

// ═══════════════════════════════════════════════════════════════
//  اللون البنفسجي للمسلسلات (يُستخدم في الملفات الثلاثة)
// ═══════════════════════════════════════════════════════════════

internal val SeriesAccent = Color(0xFF8B5CF6)

// ═══════════════════════════════════════════════════════════════
//  Hero Section
// ═══════════════════════════════════════════════════════════════

@Composable
fun SeriesHero(
    series: SeriesEntity?,
    episodeCount: Int,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(520.dp)
    ) {
        // ─── الخلفية ───
        AsyncImage(
            model = series?.backdrop ?: series?.poster,
            contentDescription = series?.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // ─── Gradient عمودي (4 مراحل) ───
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.35f),
                        0.4f to Color.Black.copy(alpha = 0.55f),
                        0.75f to Color.Black.copy(alpha = 0.85f),
                        1f to TvBackground,
                    )
                )
        )

        // ─── زر الرجوع ───
        BackFab(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(Spacing.xxl),
        )

        // ─── المحتوى السفلي ───
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = Spacing.xxxl, vertical = Spacing.xxl),
        ) {
            // ─── Badges ───
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!series?.rating.isNullOrBlank()) {
                    RatingBadge(rating = series!!.rating!!)
                }
                if (!series?.year.isNullOrBlank()) {
                    MetadataBadge(text = series!!.year!!)
                }
                if (episodeCount > 0) {
                    MetadataBadge(text = "$episodeCount حلقة")
                }
            }

            Spacer(Modifier.height(Spacing.md))

            // ─── العنوان ───
            Text(
                text = series?.name ?: "المسلسل",
                color = TvText,
                fontSize = 44.sp,
                lineHeight = 50.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            // ─── التصنيف ───
            if (!series?.genre.isNullOrBlank()) {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = series!!.genre!!,
                    color = TvTextSecondary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(Modifier.height(Spacing.xxl))

            // ─── زر المفضلة ───
            FavoriteSeriesButton(
                isFavorite = isFavorite,
                onClick = onToggleFavorite,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  Badges
// ═══════════════════════════════════════════════════════════════

@Composable
private fun RatingBadge(rating: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.xs))
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = TvGold,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = rating,
            color = TvText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun MetadataBadge(text: String) {
    Text(
        text = text,
        color = TvTextSecondary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.xs))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

// ═══════════════════════════════════════════════════════════════
//  الأزرار
// ═══════════════════════════════════════════════════════════════

@Composable
private fun BackFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.7f))
            .tvFocusable(
                style = FocusStyle.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "رجوع",
            tint = Color.White,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
fun FavoriteSeriesButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (isFavorite) SeriesAccent.copy(alpha = 0.25f) else TvSurfaceHigh
    val iconTint = if (isFavorite) SeriesAccent else TvText

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier
            .tvFocusable(
                style = FocusStyle.Button,
                onClick = onClick,
            )
            .clip(RoundedCornerShape(Radius.md))
            .background(bg)
            .padding(horizontal = Spacing.xxl, vertical = Spacing.md),
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Default.Favorite
            else Icons.Default.FavoriteBorder,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = if (isFavorite) "في المفضلة" else "أضف للمفضلة",
            color = iconTint,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  Tabs
// ═══════════════════════════════════════════════════════════════

@Composable
fun DetailTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (selected) SeriesAccent else TvSurfaceHigh
    val textColor = if (selected) Color.White else TvTextSecondary

    Text(
        text = label,
        color = textColor,
        fontSize = 14.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        modifier = Modifier
            .tvFocusable(
                style = FocusStyle.Chip,
                onClick = onClick,
            )
            .clip(RoundedCornerShape(Radius.pill))
            .background(bg)
            .padding(horizontal = Spacing.xl, vertical = Spacing.md),
    )
}
