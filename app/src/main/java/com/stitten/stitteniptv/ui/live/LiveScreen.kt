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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.stitten.stitteniptv.database.entity.ChannelEntity
import com.stitten.stitteniptv.ui.components.tvFocusable
import com.stitten.stitteniptv.ui.navigation.Routes

@Composable
fun LiveScreen(
    navController: NavHostController,
    viewModel: LiveViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val channels = viewModel.channelsPaged.collectAsLazyPagingItems()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        // ── شريط علوي ──
        LiveTopBar(channelCount = state.totalCount)

        // ── قائمة التصنيفات ──
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

        // ── شبكة القنوات ──
        when {
            state.isLoading && channels.itemCount == 0 -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("جارٍ التحميل...", color = Color.White, fontSize = 16.sp)
                }
            }

            channels.itemCount == 0 -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📡", fontSize = 64.sp)
                        Spacer(Modifier.height(12.dp))
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
                            ChannelGridCard(
                                channel = channel,
                                onClick = {
                                    navController.navigate(
                                        Routes.player(channel.url, channel.name)
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  مكونات مساعدة
// ═══════════════════════════════════════════════════════════════

@Composable
private fun LiveTopBar(channelCount: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "📡 القنوات المباشرة ($channelCount)",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
        )
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
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun ChannelGridCard(
    channel: ChannelEntity,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF161B22)),
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
