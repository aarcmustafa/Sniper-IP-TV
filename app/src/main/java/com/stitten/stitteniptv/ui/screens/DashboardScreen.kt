package com.stitten.stitteniptv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.stitten.stitteniptv.ui.components.FocusStyle
import com.stitten.stitteniptv.ui.components.HeroBanner
import com.stitten.stitteniptv.ui.components.LoadingProgress
import com.stitten.stitteniptv.ui.components.PosterCard
import com.stitten.stitteniptv.ui.components.SkeletonHomeScreen
import com.stitten.stitteniptv.ui.components.tvFocusable
import com.stitten.stitteniptv.ui.navigation.Routes
import com.stitten.stitteniptv.ui.theme.AppIcons
import com.stitten.stitteniptv.ui.theme.Radius
import com.stitten.stitteniptv.ui.theme.Spacing
import com.stitten.stitteniptv.ui.theme.TvBackground
import com.stitten.stitteniptv.ui.theme.TvPrimary
import com.stitten.stitteniptv.ui.theme.TvSurfaceHigh
import com.stitten.stitteniptv.ui.theme.TvText
import com.stitten.stitteniptv.ui.theme.TvTextSecondary
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

    val showFullLoading = mainState.isLoading || isRefreshing

    LaunchedEffect(mainState.isLoading, isRefreshing) {
        if (isRefreshing && !mainState.isLoading) {
            viewModel.retry()
            isRefreshing = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        TvBackground,
                        Color(0xFF0D1014),
                    )
                )
            )
    ) {
        when {
            // 1️⃣ التحميل الكامل مع Skeleton احترافي
            showFullLoading -> SkeletonHomeScreen()

            // 2️⃣ تحميل بيانات الواجهة
            state.isLoading -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                LoadingProgress(
                    progress = null,
                    label = "جارٍ تجهيز الواجهة...",
                )
            }

            // 3️⃣ خطأ
            state.errorMessage != null -> ErrorView(
                message = state.errorMessage!!,
                onRetry = viewModel::retry,
            )

            // 4️⃣ المحتوى
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

// ═══════════════════════════════════════════════════════════════
//  المحتوى الرئيسي
// ═══════════════════════════════════════════════════════════════

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
    Row(modifier = Modifier.fillMaxSize()) {

        // ═══════ الشريط الجانبي (من ملف منفصل) ═══════
        DashboardSidebar(
            onMoviesClick = onMoviesClick,
            onSeriesListClick = onSeriesListClick,
            onLiveClick = onLiveClick,
            onFavoritesClick = onFavoritesClick,
            onSearchClick = onSearchClick,
            onRefreshClick = onRefreshClick,
            onSettingsClick = onSettingsClick,
        )

        // ═══════ المحتوى ═══════
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = Spacing.md),
            contentPadding = PaddingValues(bottom = Spacing.xxxl),
        ) {
            // ─── Hero Banner ───
            state.featuredMovie?.let { movie ->
                item(key = "hero") {
                    Spacer(Modifier.height(Spacing.xl))
                    Box(modifier = Modifier.padding(horizontal = Spacing.xxl)) {
                        HeroBanner(
                            title = movie.name,
                            posterUrl = movie.poster,
                            backdropUrl = movie.backdrop,
                            plot = movie.plot,
                            rating = movie.rating,
                            year = movie.year,
                            subtitle = movie.genre ?: "فيلم",
                            quality = "HD",
                            onPlay = { onMovieClick(movie) },
                            onDetails = { onMovieClick(movie) },
                        )
                    }
                }
            }

            // ─── متابعة المشاهدة ───
            if (state.continueWatching.isNotEmpty()) {
                item(key = "continue") {
                    Spacer(Modifier.height(Spacing.xxl))
                    ContinueWatchingSection(
                        items = state.continueWatching,
                        onItemClick = onContinueClick,
                        onRemoveItem = onRemoveHistory,
                    )
                }
            }

            // ─── أحدث الأفلام ───
            if (state.latestMovies.isNotEmpty()) {
                item(key = "movies") {
                    Spacer(Modifier.height(Spacing.xxl))
                    ContentRow(
                        title = "أحدث الأفلام",
                        subtitle = "إصدارات جديدة",
                        icon = AppIcons.Movies,
                        items = state.latestMovies,
                        key = { it.id },
                        onSeeAll = onMoviesClick,
                    ) { movie ->
                        PosterCard(
                            title = movie.name,
                            imageUrl = movie.poster,
                            subtitle = buildSubtitle(movie.rating, movie.year),
                            rating = movie.rating,
                            onClick = { onMovieClick(movie) },
                        )
                    }
                }
            }

            // ─── الأعلى تقييمًا ───
            if (state.topRatedMovies.isNotEmpty()) {
                item(key = "top_rated") {
                    Spacer(Modifier.height(Spacing.xxl))
                    ContentRow(
                        title = "الأعلى تقييمًا",
                        subtitle = "الأفضل حسب IMDb",
                        icon = AppIcons.Trending,
                        items = state.topRatedMovies,
                        key = { "top_${it.id}" },
                        onSeeAll = onMoviesClick,
                    ) { movie ->
                        PosterCard(
                            title = movie.name,
                            imageUrl = movie.poster,
                            subtitle = buildSubtitle(movie.rating, movie.year),
                            rating = movie.rating,
                            onClick = { onMovieClick(movie) },
                        )
                    }
                }
            }

            // ─── المسلسلات ───
            if (state.allSeries.isNotEmpty()) {
                item(key = "series") {
                    Spacer(Modifier.height(Spacing.xxl))
                    ContentRow(
                        title = "المسلسلات",
                        subtitle = "أحدث المسلسلات",
                        icon = AppIcons.Series,
                        items = state.allSeries,
                        key = { it.id },
                        onSeeAll = onSeriesListClick,
                    ) { series ->
                        PosterCard(
                            title = series.name,
                            imageUrl = series.poster,
                            subtitle = buildSubtitle(series.rating, series.year),
                            rating = series.rating,
                            onClick = { onSeriesClick(series) },
                        )
                    }
                }
            }

            // ─── قنوات مباشرة ───
            if (state.liveChannels.isNotEmpty()) {
                item(key = "channels") {
                    Spacer(Modifier.height(Spacing.xxl))
                    ContentRow(
                        title = "قنوات مباشرة",
                        subtitle = "بث حي 24/7",
                        icon = AppIcons.LiveTv,
                        items = state.liveChannels,
                        key = { it.id },
                        onSeeAll = onLiveClick,
                    ) { channel ->
                        PosterCard(
                            title = channel.name,
                            imageUrl = channel.logo,
                            subtitle = channel.groupName.takeIf { it.isNotBlank() },
                            isChannel = true,
                            isLive = true,
                            onClick = { onChannelClick(channel) },
                        )
                    }
                }
            }

            // ─── حالة فارغة ───
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
}

// ═══════════════════════════════════════════════════════════════
//  أدوات مساعدة
// ═══════════════════════════════════════════════════════════════

private fun buildSubtitle(rating: String?, year: String?): String? {
    val parts = buildList {
        if (!rating.isNullOrBlank()) add("⭐ $rating")
        if (!year.isNullOrBlank()) add(year)
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString("  ·  ")
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            modifier = Modifier
                .clip(RoundedCornerShape(Radius.lg))
                .background(TvSurfaceHigh)
                .padding(Spacing.xxxl),
        ) {
            Text("⚠️", fontSize = 56.sp)

            Text(
                text = "حدث خطأ",
                color = TvText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = message,
                color = TvTextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(Spacing.sm))

            Row(
                modifier = Modifier
                    .tvFocusable(
                        style = FocusStyle.Button,
                        onClick = onRetry,
                    )
                    .clip(RoundedCornerShape(Radius.md))
                    .background(TvPrimary)
                    .padding(horizontal = Spacing.xxl, vertical = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "إعادة المحاولة",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun EmptyStateView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text("📭", fontSize = 72.sp)
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = "لا يوجد محتوى بعد",
                color = TvText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "أضف مصدرًا من الإعدادات لتبدأ المشاهدة",
                color = TvTextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
