package com.stitten.stitteniptv.ui.vod

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stitten.stitteniptv.database.entity.MovieEntity
import com.stitten.stitteniptv.ui.components.FocusStyle
import com.stitten.stitteniptv.ui.components.PosterCard
import com.stitten.stitteniptv.ui.components.tvFocusable
import com.stitten.stitteniptv.ui.theme.AppIcons
import com.stitten.stitteniptv.ui.theme.Radius
import com.stitten.stitteniptv.ui.theme.Spacing
import com.stitten.stitteniptv.ui.theme.TvPrimary
import com.stitten.stitteniptv.ui.theme.TvSkeleton
import com.stitten.stitteniptv.ui.theme.TvSkeletonHighlight
import com.stitten.stitteniptv.ui.theme.TvSurface
import com.stitten.stitteniptv.ui.theme.TvText
import com.stitten.stitteniptv.ui.theme.TvTextSecondary
import com.stitten.stitteniptv.ui.theme.TvTextTertiary

// ═══════════════════════════════════════════════════════════════
//  الشريط العلوي
// ═══════════════════════════════════════════════════════════════

@Composable
fun MoviesTopBar(
    movieCount: Int,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xxl, vertical = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        // ─── الشعار ───
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(Radius.md))
                .background(
                    Brush.linearGradient(
                        colors = listOf(TvPrimary, Color(0xFF2563EB))
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = AppIcons.Movies,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }

        // ─── العنوان + العدد ───
        Column {
            Text(
                text = "الأفلام",
                color = TvText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "$movieCount فيلم",
                color = TvTextSecondary,
                fontSize = 12.sp,
            )
        }

        Spacer(Modifier.weight(1f))

        // ─── البحث ───
        MoviesSearchField(
            query = searchQuery,
            onQueryChange = onSearchChange,
        )

        // ─── زر التحديث ───
        IconButton(
            onClick = onRefresh,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(Radius.md))
                .background(TvSurface),
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "تحديث",
                tint = TvText,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun MoviesSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .width(280.dp)
            .height(48.dp),
        placeholder = {
            Text(
                text = "ابحث عن فيلم...",
                color = TvTextTertiary,
                fontSize = 14.sp,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = TvTextSecondary,
                modifier = Modifier.size(20.dp),
            )
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "مسح",
                        tint = TvTextSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(Radius.md),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = TvSurface,
            unfocusedContainerColor = TvSurface,
            disabledContainerColor = TvSurface,
            focusedTextColor = TvText,
            unfocusedTextColor = TvText,
            cursorColor = TvPrimary,
            focusedIndicatorColor = TvPrimary,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}

// ═══════════════════════════════════════════════════════════════
//  شريط التصنيفات
// ═══════════════════════════════════════════════════════════════

@Composable
fun MoviesCategoryChipsRow(
    categories: List<String>,
    selectedCategory: String?,
    onSelectCategory: (String?) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Spacing.xxl),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item {
            CategoryChip(
                label = "الكل",
                selected = selectedCategory == null,
                onClick = { onSelectCategory(null) },
            )
        }
        items(categories) { cat ->
            CategoryChip(
                label = cat,
                selected = selectedCategory == cat,
                onClick = { onSelectCategory(cat) },
            )
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (selected) TvPrimary else TvSurface
    val textColor = if (selected) Color.White else TvTextSecondary

    Text(
        text = label,
        color = textColor,
        fontSize = 14.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .tvFocusable(
                style = FocusStyle.Chip,
                onClick = onClick,
            )
            .clip(RoundedCornerShape(Radius.pill))
            .background(bg)
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
    )
}

// ═══════════════════════════════════════════════════════════════
//  بطاقة فيلم
// ═══════════════════════════════════════════════════════════════

@Composable
fun MovieGridCard(
    movie: MovieEntity,
    onClick: () -> Unit,
) {
    PosterCard(
        title = movie.name,
        imageUrl = movie.poster,
        subtitle = buildSubtitle(movie.rating, movie.year),
        rating = movie.rating,
        onClick = onClick,
    )
}

private fun buildSubtitle(rating: String?, year: String?): String? {
    val parts = buildList {
        if (!year.isNullOrBlank()) add(year)
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString("  ·  ")
}

// ═══════════════════════════════════════════════════════════════
//  Skeleton Grid
// ═══════════════════════════════════════════════════════════════

@Composable
fun MoviesSkeletonGrid() {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 180.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.xxl,
            end = Spacing.xxl,
            top = Spacing.sm,
            bottom = Spacing.xxxl,
        ),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        items(count = 15) {
            SkeletonMovieCard()
        }
    }
}

@Composable
private fun SkeletonMovieCard() {
    Column(modifier = Modifier.fillMaxWidth()) {
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            cornerRadius = Radius.md,
        )
        Spacer(Modifier.height(Spacing.sm))
        SkeletonBox(
            modifier = Modifier.fillMaxWidth(0.92f).height(13.dp),
            cornerRadius = Radius.xs,
        )
        Spacer(Modifier.height(4.dp))
        SkeletonBox(
            modifier = Modifier.fillMaxWidth(0.55f).height(11.dp),
            cornerRadius = Radius.xs,
        )
    }
}

@Composable
private fun SkeletonBox(
    modifier: Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(TvSkeleton, TvSkeletonHighlight, TvSkeleton)
                )
            )
    )
}
