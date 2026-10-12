package com.stitten.stitteniptv.ui.live

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.paging.compose.collectAsLazyPagingItems
import com.stitten.stitteniptv.data.FavoritesManager
import com.stitten.stitteniptv.database.entity.ChannelEntity
import com.stitten.stitteniptv.ui.navigation.Routes
import com.stitten.stitteniptv.ui.theme.AppIcons
import com.stitten.stitteniptv.ui.theme.Radius
import com.stitten.stitteniptv.ui.theme.Spacing
import com.stitten.stitteniptv.ui.theme.TvBackground
import com.stitten.stitteniptv.ui.theme.TvSurface
import com.stitten.stitteniptv.ui.theme.TvText
import com.stitten.stitteniptv.ui.theme.TvTextSecondary

@Composable
fun LiveScreen(
    navController: NavHostController,
    viewModel: LiveViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val channels = viewModel.channelsPaged.collectAsLazyPagingItems()

    val ctx = LocalContext.current
    val favorites = remember { FavoritesManager(ctx) }
    var favoritesVersion by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    val favoriteIds = remember(favoritesVersion) {
        try {
            favorites.getFavoriteChannelIds()
        } catch (_: Exception) {
            emptySet<String>()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(TvBackground, Color(0xFF0D1014))
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ═══════════════════════════════════════════════════
            //  الشريط العلوي
            // ═══════════════════════════════════════════════════
            LiveTopBar(
                channelCount = state.totalCount,
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                onRefresh = { channels.refresh() },
            )

            // ═══════════════════════════════════════════════════
            //  التصنيفات
            // ═══════════════════════════════════════════════════
            if (state.categories.isNotEmpty()) {
                CategoryChipsRow(
                    categories = state.categories,
                    selectedCategory = state.selectedCategory,
                    onSelectCategory = viewModel::selectCategory,
                )
                Spacer(Modifier.height(Spacing.md))
            }

            // ═══════════════════════════════════════════════════
            //  المحتوى
            // ═══════════════════════════════════════════════════
            when {
                // ─── تحميل أولي ───
                state.isLoading && channels.itemCount == 0 -> {
                    LiveSkeletonGrid()
                }

                // ─── لا يوجد محتوى ───
                channels.itemCount == 0 -> {
                    LiveEmptyState(
                        hasSearch = searchQuery.isNotBlank(),
                        onClearSearch = { searchQuery = "" },
                    )
                }

                // ─── المحتوى ───
                else -> {
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
                        items(
                            count = channels.itemCount,
                            key = { index -> channels[index]?.id ?: index },
                        ) { index ->
                            val channel = channels[index]
                            if (channel != null) {
                                val rawId = extractRawId(channel.id)
                                val isFavorite = favoriteIds.contains(rawId) ||
                                    favoriteIds.contains(channel.id)

                                ChannelGridCard(
                                    channel = channel,
                                    isFavorite = isFavorite,
                                    onClick = {
                                        navController.navigate(
                                            Routes.player(channel.url, channel.name)
                                        )
                                    },
                                    onToggleFavorite = {
                                        try {
                                            favorites.toggleChannel(rawId, channel.name)
                                        } catch (_: Exception) {}
                                        favoritesVersion++
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  الحالات الخاصة
// ═══════════════════════════════════════════════════════════════

@Composable
private fun LiveEmptyState(
    hasSearch: Boolean,
    onClearSearch: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            modifier = Modifier.padding(Spacing.xxxl),
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(Radius.xl))
                    .background(TvSurface),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = AppIcons.LiveTv,
                    contentDescription = null,
                    tint = TvTextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp),
                )
            }

            Spacer(Modifier.height(Spacing.sm))

            Text(
                text = if (hasSearch) "لا توجد نتائج" else "لا توجد قنوات",
                color = TvText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = if (hasSearch)
                    "جرّب البحث بكلمة أخرى"
                else
                    "أضف مصدرًا من الإعدادات لتبدأ المشاهدة",
                color = TvTextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  أدوات
// ═══════════════════════════════════════════════════════════════

private fun extractRawId(fullId: String): String {
    return if (fullId.contains("_")) fullId.substringAfter("_")
    else fullId
}
