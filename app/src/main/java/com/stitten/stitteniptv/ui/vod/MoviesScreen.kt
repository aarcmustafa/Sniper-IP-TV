package com.stitten.stitteniptv.ui.vod

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.navigation.NavHostController
import androidx.paging.compose.collectAsLazyPagingItems
import com.stitten.stitteniptv.ui.navigation.Routes
import com.stitten.stitteniptv.ui.theme.AppIcons
import com.stitten.stitteniptv.ui.theme.Radius
import com.stitten.stitteniptv.ui.theme.Spacing
import com.stitten.stitteniptv.ui.theme.TvBackground
import com.stitten.stitteniptv.ui.theme.TvSurface
import com.stitten.stitteniptv.ui.theme.TvText
import com.stitten.stitteniptv.ui.theme.TvTextSecondary

@Composable
fun MoviesScreen(
    navController: NavHostController,
    viewModel: MoviesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val movies = viewModel.moviesPaged.collectAsLazyPagingItems()

    var searchQuery by remember { mutableStateOf("") }

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
            MoviesTopBar(
                movieCount = state.totalCount,
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                onRefresh = { movies.refresh() },
            )

            // ═══════════════════════════════════════════════════
            //  التصنيفات
            // ═══════════════════════════════════════════════════
            if (state.categories.isNotEmpty()) {
                MoviesCategoryChipsRow(
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
                state.isLoading && movies.itemCount == 0 -> {
                    MoviesSkeletonGrid()
                }

                // ─── لا يوجد محتوى ───
                movies.itemCount == 0 -> {
                    MoviesEmptyState(
                        hasSearch = searchQuery.isNotBlank(),
                    )
                }

                // ─── المحتوى ───
                else -> {
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
                        items(
                            count = movies.itemCount,
                            key = { index -> movies[index]?.id ?: index },
                        ) { index ->
                            val movie = movies[index]
                            if (movie != null) {
                                MovieGridCard(
                                    movie = movie,
                                    onClick = {
                                        navController.navigate(
                                            Routes.movieDetails(movie.id)
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
}

// ═══════════════════════════════════════════════════════════════
//  الحالات الخاصة
// ═══════════════════════════════════════════════════════════════

@Composable
private fun MoviesEmptyState(hasSearch: Boolean) {
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
                    imageVector = AppIcons.Movies,
                    contentDescription = null,
                    tint = TvTextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp),
                )
            }

            Spacer(Modifier.height(Spacing.sm))

            Text(
                text = if (hasSearch) "لا توجد نتائج" else "لا توجد أفلام",
                color = TvText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = if (hasSearch)
                    "جرّب البحث بكلمة أخرى"
                else
                    "أضف مصدرًا يحتوي على أفلام",
                color = TvTextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
