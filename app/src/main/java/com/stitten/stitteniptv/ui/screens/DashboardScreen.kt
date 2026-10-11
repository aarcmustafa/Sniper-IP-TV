package com.stitten.stitteniptv.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.stitten.stitteniptv.database.entity.ChannelEntity
import com.stitten.stitteniptv.database.entity.MovieEntity
import com.stitten.stitteniptv.database.entity.SeriesEntity
import com.stitten.stitteniptv.database.entity.WatchHistoryEntity
import com.stitten.stitteniptv.ui.components.ContentRow
import com.stitten.stitteniptv.ui.components.ContinueWatchingSection
import com.stitten.stitteniptv.ui.components.HeroBanner
import com.stitten.stitteniptv.ui.components.LoadingProgress
import com.stitten.stitteniptv.ui.components.PosterCard
import com.stitten.stitteniptv.ui.components.tvFocusable
import com.stitten.stitteniptv.ui.navigation.Routes

@Composable
fun DashboardScreen(
    navController: NavHostController,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        when {
            state.isLoading -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                LoadingProgress(
                    progress = state.progress,
                    label = state.progressLabel,
                )
            }

            state.errorMessage != null -> ErrorView(
                message = state.errorMessage!!,
                onRetry = viewModel::retry,
            )

            else -> DashboardContent(
                state = state,
                onMovieClick = { movie ->
                    navController.navigate(Routes.movieDetails(movie.id))
                },
                onSeriesClick = { series ->
                    navController.navigate(Routes.seriesDetails(series.id))
                },
                onChannelClick = { channel ->
                    navController.navigate(Routes.player(channel.url, channel.name))
                },
                onContinueClick = { history ->
                    navController.navigate(Routes.player(history.url, history.title))
                },
                onRemoveHistory = viewModel::removeFromHistory,
                onSearchClick = { navController.navigate(Routes.SEARCH) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                onLiveClick = { navController.navigate(Routes.LIVE) },
                onMoviesClick = { navController.navigate(Routes.MOVIES) },
                onSeriesListClick = { navController.navigate(Routes.SERIES_LIST) },
            )
        }
    }
}

@Composable
private fun DashboardContent(
    state: DashboardUiState,
    onMovieClick: (MovieEntity) -> Unit,
    onSeriesClick: (SeriesEntity) -> Unit,
    onChannelClick: (ChannelEntity) -> Unit,
    onContinueClick: (WatchHistoryEntity) -> Unit,
    onRemoveHistory: (WatchHistoryEntity) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLiveClick: () -> Unit,
    onMoviesClick: () -> Unit,
    onSeriesListClick: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item(key = "topbar") {
            DashboardTopBar(
                onSearchClick = onSearchClick,
                onSettingsClick = onSettingsClick,
                onLiveClick = onLiveClick,
                onMoviesClick = onMoviesClick,
                onSeriesListClick = onSeriesListClick,
            )
        }

        state.featuredMovie?.let { movie ->
            item(key = "hero") {
                HeroBanner(
                    title = movie.name,
                    posterUrl = movie.poster,
                    backdropUrl = movie.backdrop,
                    plot = movie.plot,
                    rating = movie.rating,
                    year = movie.year,
                    subtitle = movie.genre ?: "فيلم",
                    onPlay = { onMovieClick(movie) },
                    onDetails = { onMovieClick(movie) },
                )
            }
        }

        if (state.continueWatching.isNotEmpty()) {
            item(key = "continue") {
                Spacer(Modifier.height(16.dp))
                ContinueWatchingSection(
                    items = state.continueWatching,
                    onItemClick = onContinueClick,
                    onRemoveItem = onRemoveHistory,
                )
            }
        }

        if (state.latestMovies.isNotEmpty()) {
            item(key = "movies") {
                Spacer(Modifier.height(16.dp))
                ContentRow(
                    title = "🎬 أحدث الأفلام",
                    items = state.latestMovies,
                    key = { it.id },
                ) { movie ->
                    PosterCard(
                        title = movie.name,
                        imageUrl = movie.poster,
                        subtitle = buildSubtitle(movie.rating, movie.year),
                        onClick = { onMovieClick(movie) },
                    )
                }
            }
        }

        if (state.allSeries.isNotEmpty()) {
            item(key = "series") {
                Spacer(Modifier.height(16.dp))
                ContentRow(
                    title = "📺 المسلسلات",
                    items = state.allSeries,
                    key = { it.id },
                ) { series ->
                    PosterCard(
                        title = series.name,
                        imageUrl = series.poster,
                        subtitle = buildSubtitle(series.rating, series.year),
                        onClick = { onSeriesClick(series) },
                    )
                }
            }
        }

        if (state.liveChannels.isNotEmpty()) {
            item(key = "channels") {
                Spacer(Modifier.height(16.dp))
                ContentRow(
                    title = "📡 قنوات مباشرة",
                    items = state.liveChannels,
                    key = { it.id },
                ) { channel ->
                    PosterCard(
                        title = channel.name,
                        imageUrl = channel.logo,
                        subtitle = channel.groupName.takeIf { it.isNotBlank() },
                        isChannel = true,
                        onClick = { onChannelClick(channel) },
                    )
                }
            }
        }

        if (
            state.featuredMovie == null &&
            state.latestMovies.isEmpty() &&
            state.allSeries.isEmpty() &&
            state.liveChannels.isEmpty() &&
            state.continueWatching.isEmpty()
        ) {
            item(key = "empty") { EmptyStateView() }
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

@Composable
private fun DashboardTopBar(
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLiveClick: () -> Unit,
    onMoviesClick: () -> Unit,
    onSeriesListClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "🎬 SNIPER IPTV",
            color = Color(0xFF1F6FEB),
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
        )

        Spacer(Modifier.weight(1f))

        IconButton(onClick = onLiveClick) {
            Icon(Icons.Default.LiveTv, "قنوات مباشرة", tint = Color.White)
        }
        Spacer(Modifier.width(4.dp))

        IconButton(onClick = onMoviesClick) {
            Icon(Icons.Default.Movie, "أفلام", tint = Color.White)
        }
        Spacer(Modifier.width(4.dp))

        IconButton(onClick = onSeriesListClick) {
            Icon(Icons.Default.Tv, "مسلسلات", tint = Color.White)
        }
        Spacer(Modifier.width(8.dp))

        IconButton(onClick = onSearchClick) {
            Icon(Icons.Default.Search, "بحث", tint = Color.White)
        }
        Spacer(Modifier.width(4.dp))

        IconButton(onClick = onSettingsClick) {
            Icon(Icons.Default.Settings, "إعدادات", tint = Color.White)
        }
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("⚠️", fontSize = 48.sp)
            Text(message, color = Color.White, fontSize = 16.sp)
            Box(
                Modifier
                    .tvFocusable(onClick = onRetry)
                    .background(Color(0xFF1F6FEB), shape = RoundedCornerShape(10.dp))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text("🔄 إعادة المحاولة", color = Color.White, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun EmptyStateView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📭", fontSize = 64.sp)
            Spacer(Modifier.height(16.dp))
            Text("لا يوجد محتوى", color = Color.White, fontSize = 20.sp)
            Spacer(Modifier.height(8.dp))
            Text("أضف مصدرًا من الإعدادات", color = Color(0xFF8B949E), fontSize = 14.sp)
        }
    }
}
