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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.stitten.stitteniptv.data.FavoritesManager
import com.stitten.stitteniptv.database.entity.ChannelEntity
import com.stitten.stitteniptv.ui.components.tvFocusable
import com.stitten.stitteniptv.ui.navigation.Routes
import com.stitten.stitteniptv.ui.theme.AppIcons

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

    val favoriteIds = remember(favoritesVersion) {
        try { favorites.getFavoriteChannelIds() } catch (_: Exception) { emptySet<String>() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        LiveTopBar(
            channelCount = state.totalCount,
            onRefresh = { channels.refresh() },
        )

        if (state.categories.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    CategoryChip(
                        label = "الكل",
                        selected = state.selectedCategory == null,
                        onClick = { viewModel.selectCategory(null) },
                    )
                }
                items(state.categories) { cat ->
                    CategoryChip(
                        label = cat,
                        selected = state.selectedCategory == cat,
                        onClick = { viewModel.selectCategory(cat) },
                    )
                }
            }
        }

        when {
            state.isLoading && channels.itemCount == 0 -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("جارٍ التحميل...", color = Color.White, fontSize = 16.sp)
                }
            }

            channels.itemCount == 0 -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = AppIcons.LiveTv,
                            contentDescription = null,
                            tint = Color(0xFF30363D),
                            modifier = Modifier.size(80.dp),
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("لا توجد قنوات", color = Color.White, fontSize = 18.sp)
                    }
                }
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 180.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
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
                                    } catch (_: Exception) { }
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

private fun extractRawId(fullId: String): String {
    return if (fullId.contains("_")) fullId.substringAfter("_")
    else fullId
}

// ═══════════════════════════════════════════════════════════════
//  الشريط العلوي — مع أيقونة LiveTv
// ═══════════════════════════════════════════════════════════════

@Composable
private fun LiveTopBar(
    channelCount: Int,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = AppIcons.LiveTv,
            contentDescription = null,
            tint = Color(0xFF1F6FEB),
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = "القنوات المباشرة ($channelCount)",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onRefresh) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = "تحديث",
                tint = Color.White,
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
    val bg = if (selected) Color(0xFF1F6FEB) else Color(0xFF21262D)
    Text(
        text = label,
        color = Color.White,
        fontSize = 14.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier
            .tvFocusable(onClick = onClick)
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun ChannelGridCard(
    channel: ChannelEntity,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF161B22))
                    .tvFocusable(onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = channel.logo,
                    contentDescription = channel.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .tvFocusable(onClick = onToggleFavorite),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (isFavorite) "★" else "☆",
                    color = if (isFavorite) Color(0xFFF85149) else Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = channel.name,
            color = Color.White,
            fontSize = 13.sp,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}
