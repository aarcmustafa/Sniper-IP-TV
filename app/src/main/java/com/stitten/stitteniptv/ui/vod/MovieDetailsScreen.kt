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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.stitten.stitteniptv.database.entity.MovieEntity
import com.stitten.stitteniptv.ui.components.PosterCard
import com.stitten.stitteniptv.ui.components.tvFocusable
import com.stitten.stitteniptv.ui.navigation.Routes
import com.stitten.stitteniptv.ui.theme.AppIcons

@Composable
fun MovieDetailsScreen(
    navController: NavHostController,
    viewModel: MovieDetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        when {
            state.isLoading -> LoadingView()

            state.errorMessage != null -> ErrorView(
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
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        // ═══════════════════════════════════════════════════════
        //  Hero Section
        // ═══════════════════════════════════════════════════════
        item(key = "hero") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
            ) {
                AsyncImage(
                    model = movie.backdrop ?: movie.poster,
                    contentDescription = movie.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.3f),
                                    Color.Black.copy(alpha = 0.5f),
                                    Color(0xFF0D1117).copy(alpha = 1f),
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 24.dp)
                ) {
                    Text(
                        text = movie.name,
                        color = Color.White,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    val infoLine = buildList {
                        if (!movie.year.isNullOrBlank()) add(movie.year)
                        if (!movie.rating.isNullOrBlank()) add("⭐ ${movie.rating}")
                        if (!movie.duration.isNullOrBlank()) add(movie.duration)
                        if (!movie.genre.isNullOrBlank()) add(movie.genre)
                    }.joinToString("  ·  ")

                    if (infoLine.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = infoLine,
                            color = Color(0xFFD0D7DE),
                            fontSize = 15.sp,
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PlayButton(onClick = onPlay)
                        FavoriteButton(
                            isFavorite = isFavorite,
                            onClick = onToggleFavorite,
                        )
                        BackButton(onClick = onBack)
                    }
                }
            }
        }

        // ═══════════════════════════════════════════════════════
        //  Tabs
        // ═══════════════════════════════════════════════════════
        item(key = "tabs") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
//  تبويب: نظرة عامة
// ═══════════════════════════════════════════════════════════════

@Composable
private fun OverviewTab(movie: MovieEntity) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 8.dp)
    ) {
        if (!movie.plot.isNullOrBlank()) {
            SectionTitle(text = "القصة")
            Text(
                text = movie.plot,
                color = Color(0xFFD0D7DE),
                fontSize = 15.sp,
                lineHeight = 24.sp,
            )
            Spacer(Modifier.height(24.dp))
        }

        val details = buildList {
            if (!movie.director.isNullOrBlank()) add("المخرج" to movie.director!!)
            if (!movie.genre.isNullOrBlank()) add("التصنيف" to movie.genre!!)
            if (!movie.year.isNullOrBlank()) add("السنة" to movie.year!!)
            if (!movie.duration.isNullOrBlank()) add("المدة" to movie.duration!!)
            if (!movie.category.isNullOrBlank()) add("الفئة" to movie.category)
        }

        if (details.isNotEmpty()) {
            SectionTitle(text = "معلومات")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                details.forEach { (label, value) ->
                    Row {
                        Text(
                            text = "$label: ",
                            color = Color(0xFF8B949E),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = value,
                            color = Color(0xFFD0D7DE),
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  تبويب: الفريق
// ═══════════════════════════════════════════════════════════════

@Composable
private fun CastTab(movie: MovieEntity) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 8.dp)
    ) {
        if (movie.cast.isNullOrBlank() && movie.director.isNullOrBlank()) {
            EmptyTabState(message = "لا توجد معلومات عن الفريق")
            return@Column
        }

        if (!movie.director.isNullOrBlank()) {
            SectionTitle(text = "المخرج")
            PersonChip(name = movie.director!!, role = "مخرج")
            Spacer(Modifier.height(20.dp))
        }

        if (!movie.cast.isNullOrBlank()) {
            SectionTitle(text = "الممثلون")
            val actors = movie.cast!!
                .split(",", "،")
                .map { it.trim() }
                .filter { it.isNotBlank() }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                actors.forEach { actor ->
                    PersonChip(name = actor, role = "ممثل")
                }
            }
        }
    }
}

@Composable
private fun PersonChip(name: String, role: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF161B22))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFF1F6FEB).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.take(1),
                color = Color(0xFF1F6FEB),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.width(12.dp))

        Column {
            Text(
                text = name,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = role,
                color = Color(0xFF8B949E),
                fontSize = 12.sp,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  تبويب: مماثل
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SimilarTab(
    movies: List<MovieEntity>,
    onClick: (MovieEntity) -> Unit,
) {
    if (movies.isEmpty()) {
        EmptyTabState(message = "لا توجد أفلام مشابهة")
        return
    }

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(movies, key = { it.id }) { movie ->
            PosterCard(
                title = movie.name,
                imageUrl = movie.poster,
                subtitle = buildSubtitle(movie.rating, movie.year),
                onClick = { onClick(movie) },
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  المكونات المساعدة
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 12.dp),
    )
}

@Composable
private fun DetailTab(
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
            .padding(horizontal = 20.dp, vertical = 10.dp),
    )
}

@Composable
private fun EmptyTabState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            color = Color(0xFF8B949E),
            fontSize = 15.sp,
        )
    }
}

@Composable
private fun PlayButton(onClick: () -> Unit) {
    Text(
        text = "▶  تشغيل",
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .tvFocusable(onClick = onClick)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1F6FEB))
            .padding(horizontal = 28.dp, vertical = 12.dp)
    )
}

@Composable
private fun FavoriteButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
) {
    val text = if (isFavorite) "★  في المفضلة" else "☆  أضف للمفضلة"
    val bg = if (isFavorite) Color(0xFFF85149).copy(alpha = 0.2f) else Color(0xFF30363D)
    val textColor = if (isFavorite) Color(0xFFF85149) else Color.White

    Text(
        text = text,
        color = textColor,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .tvFocusable(onClick = onClick)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(horizontal = 24.dp, vertical = 12.dp)
    )
}

@Composable
private fun BackButton(onClick: () -> Unit) {
    Text(
        text = "← رجوع",
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .tvFocusable(onClick = onClick)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF30363D))
            .padding(horizontal = 24.dp, vertical = 12.dp)
    )
}

private fun buildSubtitle(rating: String?, year: String?): String? {
    val parts = buildList {
        if (!rating.isNullOrBlank()) add("⭐ $rating")
        if (!year.isNullOrBlank()) add(year)
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString("  ·  ")
}

@Composable
private fun LoadingView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Color(0xFF1F6FEB))
    }
}

@Composable
private fun ErrorView(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("⚠️", fontSize = 48.sp)
            Text(message, color = Color.White, fontSize = 16.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PlayButton(onClick = onRetry)
                BackButton(onClick = onBack)
            }
        }
    }
}
