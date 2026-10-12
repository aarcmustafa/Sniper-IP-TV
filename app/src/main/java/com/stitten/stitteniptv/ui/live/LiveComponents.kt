package com.stitten.stitteniptv.ui.live

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.stitten.stitteniptv.database.entity.ChannelEntity
import com.stitten.stitteniptv.ui.components.FocusStyle
import com.stitten.stitteniptv.ui.components.tvFocusable
import com.stitten.stitteniptv.ui.theme.AppIcons
import com.stitten.stitteniptv.ui.theme.Radius
import com.stitten.stitteniptv.ui.theme.Spacing
import com.stitten.stitteniptv.ui.theme.TvGold
import com.stitten.stitteniptv.ui.theme.TvLive
import com.stitten.stitteniptv.ui.theme.TvPrimary
import com.stitten.stitteniptv.ui.theme.TvSkeleton
import com.stitten.stitteniptv.ui.theme.TvSkeletonHighlight
import com.stitten.stitteniptv.ui.theme.TvSurface
import com.stitten.stitteniptv.ui.theme.TvSurfaceHigh
import com.stitten.stitteniptv.ui.theme.TvText
import com.stitten.stitteniptv.ui.theme.TvTextSecondary
import com.stitten.stitteniptv.ui.theme.TvTextTertiary

// ═══════════════════════════════════════════════════════════════
//  الشريط العلوي
// ═══════════════════════════════════════════════════════════════

@Composable
fun LiveTopBar(
    channelCount: Int,
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
        // ─── الشعار والأيقونة ───
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(Radius.md))
                .background(
                    Brush.linearGradient(
                        colors = listOf(TvLive, Color(0xFFB91C1C))
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = AppIcons.LiveTv,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }

        // ─── العنوان ───
        Column {
            Text(
                text = "القنوات المباشرة",
                color = TvText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "$channelCount قناة",
                color = TvTextSecondary,
                fontSize = 12.sp,
            )
        }

        Spacer(Modifier.weight(1f))

        // ─── البحث ───
        SearchField(
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
private fun SearchField(
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
                text = "ابحث عن قناة...",
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
fun CategoryChipsRow(
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
//  بطاقة قناة للـ Grid
// ═══════════════════════════════════════════════════════════════

@Composable
fun ChannelGridCard(
    channel: ChannelEntity,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {

        // ─── الصورة ───
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(125.dp)
                .clip(RoundedCornerShape(Radius.md))
                .background(TvSurfaceHigh)
                .tvFocusable(
                    style = FocusStyle.Channel,
                    onClick = onClick,
                ),
        ) {
            // ─── الشعار ───
            AsyncImage(
                model = channel.logo,
                contentDescription = channel.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.md),
            )

            // ─── طبقة تعتيم سفلية ───
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f)
                            )
                        )
                    )
            )

            // ─── شارة LIVE (أعلى يسار) ───
            LiveBadge(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(Spacing.sm)
            )

            // ─── زر المفضلة (أعلى يمين) ───
            FavoriteButton(
                isFavorite = isFavorite,
                onClick = onToggleFavorite,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(Spacing.sm)
            )
        }

        // ─── اسم القناة ───
        Spacer(Modifier.height(Spacing.sm))

        Text(
            text = channel.name,
            color = TvText,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )

        if (channel.groupName.isNotBlank()) {
            Text(
                text = channel.groupName,
                color = TvTextTertiary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  مكونات مساعدة
// ═══════════════════════════════════════════════════════════════

@Composable
private fun LiveBadge(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .clip(RoundedCornerShape(Radius.xs))
            .background(TvLive)
            .padding(horizontal = 6.dp, vertical = 3.dp),
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
        Text(
            text = "LIVE",
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp,
        )
    }
}

@Composable
private fun FavoriteButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.6f))
            .tvFocusable(
                style = FocusStyle.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (isFavorite) "★" else "☆",
            color = if (isFavorite) TvGold else Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  Skeleton Grid
// ═══════════════════════════════════════════════════════════════

@Composable
fun LiveSkeletonGrid() {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 200.dp),
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
        items(count = 12) {
            SkeletonChannelCard()
        }
    }
}

@Composable
private fun SkeletonChannelCard() {
    Column(modifier = Modifier.fillMaxWidth()) {
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(125.dp),
            cornerRadius = Radius.md,
        )
        Spacer(Modifier.height(Spacing.sm))
        SkeletonBox(
            modifier = Modifier.fillMaxWidth(0.85f).height(13.dp),
            cornerRadius = Radius.xs,
        )
        Spacer(Modifier.height(4.dp))
        SkeletonBox(
            modifier = Modifier.fillMaxWidth(0.5f).height(11.dp),
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
