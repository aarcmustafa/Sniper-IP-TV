package com.stitten.stitteniptv.ui.vod

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.navigation.NavHostController
import com.stitten.stitteniptv.database.entity.MovieEntity
import com.stitten.stitteniptv.ui.components.FocusStyle
import com.stitten.stitteniptv.ui.components.tvFocusable
import com.stitten.stitteniptv.ui.navigation.Routes
import com.stitten.stitteniptv.ui.theme.Radius
import com.stitten.stitteniptv.ui.theme.Spacing
import com.stitten.stitteniptv.ui.theme.TvBackground
import com.stitten.stitteniptv.ui.theme.TvPrimary
import com.stitten.stitteniptv.ui.theme.TvSurfaceHigh
import com.stitten.stitteniptv.ui.theme.TvText
import com.stitten.stitteniptv.ui.theme.TvTextSecondary

@Composable
fun MovieDetailsScreen(
    navController: NavHostController,
    viewModel: MovieDetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvBackground)
    ) {
        when {
            state.isLoading -> MovieDetailsSkeleton()

            state.errorMessage != null -> DetailsErrorView(
                message = state.errorMessage!!,
                onRetry = viewModel::retry,
                onBack = { navController.popBackStack() },
            )

            state.movie != null -> MovieDetailsContent(
                movie = state.movie!!,
                isFavorite = state.isFavorite,
                selectedTab = state.selectedTab,
                similarMovies = state.similarMovies,
                onTabSelected = viewModel::selectTab,
                onPlay = {
                    navController.navigate(
                        Routes.player(state.movie!!.url, state.movie!!.name)
                    )
                },
                onToggleFavorite = viewModel::toggleFavorite,
                onSimilarClick = { similar ->
                    navController.navigate(Routes.movieDetails(similar.id))
                },
                onBack = { navController.popBackStack() },
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  المحتوى الرئيسي
// ═══════════════════════════════════════════════════════════════

@Composable
private fun MovieDetailsContent(
    movie: MovieEntity,
    isFavorite: Boolean,
    selectedTab: MovieDetailTab,
    similarMovies: List<MovieEntity>,
    onTabSelected: (MovieDetailTab) -> Unit,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSimilarClick: (MovieEntity) -> Unit,
    onBack: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Spacing.xxxl),
    ) {
        // ═══════════════════════════════════════════════════════
        //  Hero
        // ═══════════════════════════════════════════════════════
        item(key = "hero") {
            MovieHero(
                movie = movie,
                isFavorite = isFavorite,
                onPlay = onPlay,
                onToggleFavorite = onToggleFavorite,
                onBack = onBack,
            )
        }

        // ═══════════════════════════════════════════════════════
        //  Tabs
        // ═══════════════════════════════════════════════════════
        item(key = "tabs") {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = Spacing.xxxl,
                        end = Spacing.xxxl,
                        top = Spacing.lg,
                        bottom = Spacing.lg,
                    ),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                DetailTab(
                    label = "نظرة عامة",
                    selected = selectedTab == MovieDetailTab.OVERVIEW,
                    onClick = { onTabSelected(MovieDetailTab.OVERVIEW) },
                )
                DetailTab(
                    label = "الفريق",
                    selected = selectedTab == MovieDetailTab.CAST,
                    onClick = { onTabSelected(MovieDetailTab.CAST) },
                )
                DetailTab(
                    label = "مماثل",
                    selected = selectedTab == MovieDetailTab.SIMILAR,
                    onClick = { onTabSelected(MovieDetailTab.SIMILAR) },
                )
            }
        }

        // ═══════════════════════════════════════════════════════
        //  محتوى التبويب المختار
        // ═══════════════════════════════════════════════════════
        when (selectedTab) {
            MovieDetailTab.OVERVIEW -> {
                item(key = "overview") {
                    OverviewTab(movie = movie)
                }
            }
            MovieDetailTab.CAST -> {
                item(key = "cast") {
                    CastTab(movie = movie)
                }
            }
            MovieDetailTab.SIMILAR -> {
                item(key = "similar") {
                    SimilarTab(
                        movies = similarMovies,
                        onClick = onSimilarClick,
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  Skeleton Loading
// ═══════════════════════════════════════════════════════════════

@Composable
private fun MovieDetailsSkeleton() {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = Spacing.xxl)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            TvSurfaceHigh,
                            TvBackground,
                        )
                    )
                )
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  Error View
// ═══════════════════════════════════════════════════════════════

@Composable
private fun DetailsErrorView(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
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

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                // ─── زر إعادة المحاولة ───
                Row(
                    modifier = Modifier
                        .tvFocusable(
                            style = FocusStyle.Button,
                            onClick = onRetry,
                        )
                        .clip(RoundedCornerShape(Radius.md))
                        .background(TvPrimary)
                        .padding(
                            horizontal = Spacing.xxl,
                            vertical = Spacing.md,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.height(20.dp),
                    )
                    Text(
                        text = "إعادة المحاولة",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                // ─── زر الرجوع ───
                Text(
                    text = "رجوع",
                    color = TvText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .tvFocusable(
                            style = FocusStyle.Button,
                            onClick = onBack,
                        )
                        .clip(RoundedCornerShape(Radius.md))
                        .background(TvSurfaceHigh)
                        .padding(
                            horizontal = Spacing.xxl,
                            vertical = Spacing.md,
                        ),
                )
            }
        }
    }
}
