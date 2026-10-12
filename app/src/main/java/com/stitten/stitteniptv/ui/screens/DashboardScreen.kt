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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.stitten.stitteniptv.ui.components.SkeletonHomeScreen
import com.stitten.stitteniptv.ui.components.tvFocusable
import com.stitten.stitteniptv.ui.navigation.Routes
import com.stitten.stitteniptv.ui.theme.AppIcons
import com.stitten.stitteniptv.viewmodel.MainViewModel

@Composable
fun DashboardScreen(
    navController: NavHostController,
    viewModel: DashboardViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val mainState by mainViewModel.uiState.collectAsStateWithLifecycle()

    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(mainState.isLoading, isRefreshing) {
        if (isRefreshing && !mainState.isLoading) {
            viewModel.retry()
            isRefreshing = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        when {
            isRefreshing -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                LoadingProgress(
                    progress = mainState.loadingProgress / 100f,
                    label = mainState.loadingMessage,
                )
            }

            state.isLoading -> SkeletonHomeScreen()

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
                onFavoritesClick = { navController.navigate(Routes.FAVORITES) },
                onRefreshClick = {
                    isRefreshing = true
                    mainViewModel.refreshM3uSource()
                },
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
    onFavoritesClick: () -> Unit,
    onRefreshClick: () -> Unit,
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
                onFavoritesClick = onFavoritesClick,
                onRefreshClick = onRefreshClick,
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
                Spacer(Modifier.height(20.dp))
                ContentRow(
                    title = "أحدث الأفلام",
                    icon = AppIcons.Movies,
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

        if (state.topRatedMovies.isNotEmpty()) {
            item(key = "top_rated") {
                Spacer(Modifier.height(20.dp))
                ContentRow(
                    title = "الأعلى تقييمًا",
                    icon = AppIcons.Trending,
                    items = state.topRatedMovies,
                    key = { "top_${it.id}" },
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
                Spacer(Modifier.height(20.dp))
                ContentRow(
                    title = "المسلسلات",
                    icon = AppIcons.Series,
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
                Spacer(Modifier.height(20.dp))
                ContentRow(
                    title = "قنوات مباشرة",
                    icon = AppIcons.LiveTv,
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
            state.topRatedMovies.isEmpty() &&
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
    onFavoritesClick: () -> Unit,
    onRefreshClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // ✅ الشعار بـ LTR (لمنع انعكاس النص)
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = AppIcons.Movies,
                    contentDescription = null,
                    tint = Color(0xFF1F6FEB),
                    modifier = Modifier.size(26.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "STTITEN IP TV",
                    color = Color(0xFF1F6FEB),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }

        Spacer(Modifier.weight(1f))

        TopBarButton(
            icon = Icons.Default.Movie,
            contentDescription = "أفلام",
            onClick = onMoviesClick,
        )
        TopBarButton(
            icon = Icons.Default.Tv,
            contentDescription = "مسلسلات",
            onClick = onSeriesListClick,
        )
        TopBarButton(
            icon = Icons.Default.LiveTv,
            contentDescription = "قنوات مباشرة",
            onClick = onLiveClick,
        )
        TopBarButton(
            icon = Icons.Default.Favorite,
            contentDescription = "المفضلة",
            tint = Color(0xFFF85149),
            onClick = onFavoritesClick,
        )
        TopBarButton(
            icon = Icons.Default.Search,
            contentDescription = "بحث",
            onClick = onSearchClick,
        )
        TopBarButton(
            icon = Icons.Default.Refresh,
            contentDescription = "تحديث من المصدر",
            tint = Color(0xFF66BB6A),
            onClick = onRefreshClick,
        )
        TopBarButton(
            icon = Icons.Default.Settings,
            contentDescription = "إعدادات",
            onClick = onSettingsClick,
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  ✅ زر TopBar — يستخدم tvFocusable لإضاءة واضحة بالريموت
// ═══════════════════════════════════════════════════════════════

@Composable
private fun TopBarButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = Color.White,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .tvFocusable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
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
