package com.stitten.stitteniptv.ui.favorites

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.stitten.stitteniptv.data.Channel
import com.stitten.stitteniptv.data.Movie
import com.stitten.stitteniptv.data.Series
import com.stitten.stitteniptv.ui.components.PosterCard
import com.stitten.stitteniptv.ui.components.tvFocusable
import com.stitten.stitteniptv.ui.navigation.Routes

@Composable
fun FavoritesScreen(
    navController: NavHostController,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        // ─── الشريط العلوي ───
        FavoritesTopBar(
            onBack = { navController.popBackStack() },
            moviesCount = state.movies.size,
            seriesCount = state.series.size,
            channelsCount = state.channels.size,
        )

        // ─── التبويبات ───
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TabChip(
                label = "🎬 أفلام (${state.movies.size})",
                selected = state.selectedTab == FavoritesTab.MOVIES,
                onClick = { viewModel.selectTab(FavoritesTab.MOVIES) },
            )
            TabChip(
                label = "📺 مسلسلات (${state.series.size})",
                selected = state.selectedTab == FavoritesTab.SERIES,
                onClick = { viewModel.selectTab(FavoritesTab.SERIES) },
            )
            TabChip(
                label = "📡 قنوات (${state.channels.size})",
                selected = state.selectedTab == FavoritesTab.CHANNELS,
                onClick = { viewModel.selectTab(FavoritesTab.CHANNELS) },
            )
        }

        Spacer(Modifier.height(8.dp))

        // ─── المحتوى ───
        when (state.selectedTab) {
            FavoritesTab.MOVIES -> {
                if (state.movies.isEmpty()) {
                    EmptyFavoritesView(
                        icon = "🎬",
                        title = "لا توجد أفلام مفضلة",
                        subtitle = "أضف أفلامًا من شاشة التفاصيل",
                    )
                } else {
                    MoviesGrid(
                        movies = state.movies,
                        onClick = { movie ->
                            navController.navigate(Routes.movieDetails(movie.id))
                        },
                        onLongRemove = { movie ->
                            viewModel.removeMovieFavorite(movie.id)
                        },
                    )
                }
            }

            FavoritesTab.SERIES -> {
                if (state.series.isEmpty()) {
                    EmptyFavoritesView(
                        icon = "📺",
                        title = "لا توجد مسلسلات مفضلة",
                        subtitle = "أضف مسلسلات من شاشة التفاصيل",
                    )
                } else {
                    SeriesGrid(
                        series = state.series,
                        onClick = { s ->
                            navController.navigate(Routes.seriesDetails(s.id))
                        },
                        onLongRemove = { s ->
                            viewModel.removeSeriesFavorite(s.id)
                        },
                    )
                }
            }

            FavoritesTab.CHANNELS -> {
                if (state.channels.isEmpty()) {
                    EmptyFavoritesView(
                        icon = "📡",
                        title = "لا توجد قنوات مفضلة",
                        subtitle = "أضف قنوات من قائمة القنوات",
                    )
                } else {
                    ChannelsGrid(
                        channels = state.channels,
                        onClick = { c ->
                            navController.navigate(Routes.player(c.url, c.name))
                        },
                        onLongRemove = { c ->
                            viewModel.removeChannelFavorite(c.id)
                        },
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  الشريط العلوي
// ═══════════════════════════════════════════════════════════════

@Composable
private fun FavoritesTopBar(
    onBack: () -> Unit,
    moviesCount: Int,
    seriesCount: Int,
    channelsCount: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "رجوع",
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
        }

        Spacer(Modifier.width(8.dp))

        Text(
            text = "⭐ المفضلة",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.weight(1f))

        Text(
            text = "${moviesCount + seriesCount + channelsCount} عنصر",
            color = Color(0xFF8B949E),
            fontSize = 14.sp,
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  التبويب
// ═══════════════════════════════════════════════════════════════

@Composable
private fun TabChip(
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

// ═══════════════════════════════════════════════════════════════
//  الشبكات
// ═══════════════════════════════════════════════════════════════

@Composable
private fun MoviesGrid(
    movies: List<Movie>,
    onClick: (Movie) -> Unit,
    onLongRemove: (Movie) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 180.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(
            count = movies.size,
            key = { idx -> movies[idx].id },
        ) { idx ->
            val movie = movies[idx]
            PosterCard(
                title = movie.name,
                imageUrl = movie.poster,
                subtitle = buildSubtitle(movie.rating, movie.year),
                onClick = { onClick(movie) },
            )
        }
    }
}

@Composable
private fun SeriesGrid(
    series: List<Series>,
    onClick: (Series) -> Unit,
    onLongRemove: (Series) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 180.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(
            count = series.size,
            key = { idx -> series[idx].id },
        ) { idx ->
            val s = series[idx]
            PosterCard(
                title = s.name,
                imageUrl = s.poster,
                subtitle = buildSubtitle(s.rating, s.year),
                onClick = { onClick(s) },
            )
        }
    }
}

@Composable
private fun ChannelsGrid(
    channels: List<Channel>,
    onClick: (Channel) -> Unit,
    onLongRemove: (Channel) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 180.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(
            count = channels.size,
            key = { idx -> channels[idx].id },
        ) { idx ->
            val c = channels[idx]
            PosterCard(
                title = c.name,
                imageUrl = c.logo,
                subtitle = c.group.takeIf { it.isNotBlank() },
                isChannel = true,
                onClick = { onClick(c) },
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  حالة فارغة
// ═══════════════════════════════════════════════════════════════

@Composable
private fun EmptyFavoritesView(
    icon: String,
    title: String,
    subtitle: String,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 72.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                title,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                subtitle,
                color = Color(0xFF8B949E),
                fontSize = 14.sp,
            )
        }
    }
}

private fun buildSubtitle(rating: String?, year: String?): String? {
    val parts = buildList {
        if (!rating.isNullOrBlank()) add("⭐ $rating")
        if (!year.isNullOrBlank()) add(year)
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString("  ·  ")
}
