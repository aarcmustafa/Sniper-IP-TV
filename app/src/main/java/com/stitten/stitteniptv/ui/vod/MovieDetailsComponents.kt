package com.stitten.stitteniptv.ui.vod

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
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
import com.stitten.stitteniptv.database.entity.MovieEntity
import com.stitten.stitteniptv.ui.components.FocusStyle
import com.stitten.stitteniptv.ui.components.PosterCard
import com.stitten.stitteniptv.ui.components.tvFocusable
import com.stitten.stitteniptv.ui.theme.Radius
import com.stitten.stitteniptv.ui.theme.Spacing
import com.stitten.stitteniptv.ui.theme.TvBackground
import com.stitten.stitteniptv.ui.theme.TvGold
import com.stitten.stitteniptv.ui.theme.TvLive
import com.stitten.stitteniptv.ui.theme.TvPrimary
import com.stitten.stitteniptv.ui.theme.TvSurfaceHigh
import com.stitten.stitteniptv.ui.theme.TvText
import com.stitten.stitteniptv.ui.theme.TvTextSecondary

// ═══════════════════════════════════════════════════════════════
//  Hero Section
// ═══════════════════════════════════════════════════════════════

@Composable
fun MovieHero(
    movie: MovieEntity,
    isFavorite: Boolean,
    onPlay: () -> Unit,
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
            model = movie.backdrop ?: movie.poster,
            contentDescription = movie.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(520.dp),
        )

        // ─── Gradient عمودي ───
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(520.dp)
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

        // ─── المحتوى ───
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
                if (!movie.rating.isNullOrBlank()) {
                    RatingBadge(rating = movie.rating!!)
                }
                if (!movie.year.isNullOrBlank()) {
                    MetadataBadge(text = movie.year!!)
                }
                if (!movie.duration.isNullOrBlank()) {
                    MetadataBadge(text = movie.duration!!)
                }
            }

            Spacer(Modifier.height(Spacing.md))

            // ─── العنوان ───
            Text(
                text = movie.name,
                color = TvText,
                fontSize = 44.sp,
                lineHeight = 50.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            // ─── التصنيف ───
            if (!movie.genre.isNullOrBlank()) {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = movie.genre!!,
                    color = TvTextSecondary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(Modifier.height(Spacing.xxl))

            // ─── الأزرار ───
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlayButton(onClick = onPlay)
                FavoriteButton(
                    isFavorite = isFavorite,
                    onClick = onToggleFavorite,
                )
            }
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
private fun PlayButton(onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier
            .tvFocusable(
                style = FocusStyle.Button,
                onClick = onClick,
            )
            .clip(RoundedCornerShape(Radius.md))
            .background(TvPrimary)
            .padding(horizontal = Spacing.xxl, vertical = Spacing.md),
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = "تشغيل",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun FavoriteButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (isFavorite) TvLive.copy(alpha = 0.2f) else TvSurfaceHigh
    val iconTint = if (isFavorite) TvLive else TvText

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

// ═══════════════════════════════════════════════════════════════
//  Tabs
// ═══════════════════════════════════════════════════════════════

@Composable
fun DetailTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (selected) TvPrimary else TvSurfaceHigh
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

// ═══════════════════════════════════════════════════════════════
//  تبويب: نظرة عامة
// ═══════════════════════════════════════════════════════════════

@Composable
fun OverviewTab(movie: MovieEntity) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xxxl, vertical = Spacing.sm)
    ) {
        if (!movie.plot.isNullOrBlank()) {
            SectionTitle(text = "القصة")
            Text(
                text = movie.plot!!,
                color = TvTextSecondary,
                fontSize = 15.sp,
                lineHeight = 26.sp,
            )
            Spacer(Modifier.height(Spacing.xxl))
        }

        val details = buildList {
            if (!movie.director.isNullOrBlank()) add("المخرج" to movie.director!!)
            if (!movie.genre.isNullOrBlank()) add("التصنيف" to movie.genre!!)
            if (!movie.year.isNullOrBlank()) add("السنة" to movie.year!!)
            if (!movie.duration.isNullOrBlank()) add("المدة" to movie.duration!!)
            if (!movie.category.isNullOrBlank()) add("الفئة" to movie.category)
        }

        if (details.isNotEmpty()) {
            SectionTitle(text = "معلومات")
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                details.forEach { (label, value) ->
                    DetailRow(label = label, value = value)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            text = label,
            color = TvTextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(90.dp),
        )
        Text(
            text = value,
            color = TvText,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f),
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  تبويب: الفريق
// ═══════════════════════════════════════════════════════════════

@Composable
fun CastTab(movie: MovieEntity) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xxxl, vertical = Spacing.sm)
    ) {
        if (movie.cast.isNullOrBlank() && movie.director.isNullOrBlank()) {
            EmptyTabState(message = "لا توجد معلومات عن الفريق")
            return@Column
        }

        if (!movie.director.isNullOrBlank()) {
            SectionTitle(text = "المخرج")
            PersonChip(name = movie.director!!, role = "مخرج")
            Spacer(Modifier.height(Spacing.xl))
        }

        if (!movie.cast.isNullOrBlank()) {
            SectionTitle(text = "الممثلون")
            val actors = movie.cast!!
                .split(",", "،")
                .map { it.trim() }
                .filter { it.isNotBlank() }

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                actors.forEach { actor ->
                    PersonChip(name = actor, role = "ممثل")
                }
            }
        }
    }
}

@Composable
private fun PersonChip(name: String, role: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(TvSurfaceHigh)
            .padding(
                horizontal = Spacing.lg,
                vertical = Spacing.md,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            TvPrimary.copy(alpha = 0.3f),
                            TvPrimary.copy(alpha = 0.1f),
                        )
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.take(1).uppercase(),
                color = TvPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }

        Spacer(Modifier.width(Spacing.md))

        Column {
            Text(
                text = name,
                color = TvText,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = role,
                color = TvTextSecondary,
                fontSize = 12.sp,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  تبويب: مماثل
// ═══════════════════════════════════════════════════════════════

@Composable
fun SimilarTab(
    movies: List<MovieEntity>,
    onClick: (MovieEntity) -> Unit,
) {
    if (movies.isEmpty()) {
        EmptyTabState(message = "لا توجد أفلام مشابهة")
        return
    }

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Spacing.xxxl),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        items(movies, key = { it.id }) { movie ->
            PosterCard(
                title = movie.name,
                imageUrl = movie.poster,
                subtitle = movie.year,
                rating = movie.rating,
                onClick = { onClick(movie) },
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  مكونات مساعدة
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = TvText,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = Spacing.md),
    )
}

@Composable
private fun EmptyTabState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            color = TvTextSecondary,
            fontSize = 15.sp,
        )
    }
}
