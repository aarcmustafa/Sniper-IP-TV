package com.stitten.stitteniptv.ui.vod

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
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
import androidx.paging.compose.collectAsLazyPagingItems
import com.stitten.stitteniptv.database.entity.MovieEntity
import com.stitten.stitteniptv.ui.components.PosterCard
import com.stitten.stitteniptv.ui.components.tvFocusable
import com.stitten.stitteniptv.ui.navigation.Routes
import com.stitten.stitteniptv.ui.theme.AppIcons

@Composable
fun MoviesScreen(
    navController: NavHostController,
    viewModel: MoviesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val movies = viewModel.moviesPaged.collectAsLazyPagingItems()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        MoviesTopBar(
            movieCount = state.totalCount,
            onRefresh = { movies.refresh() },
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
            state.isLoading && movies.itemCount == 0 -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("جارٍ التحميل...", color = Color.White, fontSize = 16.sp)
                }
            }

            movies.itemCount == 0 -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = AppIcons.Movies,
                            contentDescription = null,
                            tint = Color(0xFF30363D),
                            modifier = Modifier.size(80.dp),
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("لا توجد أفلام", color = Color.White, fontSize = 18.sp)
                    }
                }
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 180.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
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
                                    navController.navigate(Routes.movieDetails(movie.id))
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
//  الشريط العلوي — مع أيقونة Movie
// ═══════════════════════════════════════════════════════════════

@Composable
private fun MoviesTopBar(
    movieCount: Int,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = AppIcons.Movies,
            contentDescription = null,
            tint = Color(0xFF1F6FEB),
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = "الأفلام ($movieCount)",
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
private fun MovieGridCard(
    movie: MovieEntity,
    onClick: () -> Unit,
) {
    PosterCard(
        title = movie.name,
        imageUrl = movie.poster,
        subtitle = buildSubtitle(movie.rating, movie.year),
        onClick = onClick,
    )
}

private fun buildSubtitle(rating: String?, year: String?): String? {
    val parts = buildList {
        if (!rating.isNullOrBlank()) add("⭐ $rating")
        if (!year.isNullOrBlank()) add(year)
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString("  ·  ")
}
