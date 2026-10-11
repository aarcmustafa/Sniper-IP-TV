package com.stitten.stitteniptv.ui.search

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.paging.compose.collectAsLazyPagingItems
import com.stitten.stitteniptv.database.entity.ChannelEntity
import com.stitten.stitteniptv.database.entity.MovieEntity
import com.stitten.stitteniptv.database.entity.SeriesEntity
import com.stitten.stitteniptv.ui.components.PosterCard
import com.stitten.stitteniptv.ui.components.tvFocusable
import com.stitten.stitteniptv.ui.navigation.Routes
import com.stitten.stitteniptv.ui.theme.AppIcons

@Composable
fun SearchScreen(
    navController: NavHostController,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val movies = viewModel.moviesPaged.collectAsLazyPagingItems()
    val series = viewModel.seriesPaged.collectAsLazyPagingItems()
    val channels = viewModel.channelsPaged.collectAsLazyPagingItems()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        SearchBar(
            query = state.query,
            onQueryChange = viewModel::onQueryChange,
            onClear = viewModel::clear,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TabChip(
                label = "أفلام",
                icon = AppIcons.Movies,
                selected = state.selectedTab == SearchTab.MOVIES,
                onClick = { viewModel.selectTab(SearchTab.MOVIES) },
            )
            TabChip(
                label = "مسلسلات",
                icon = AppIcons.Series,
                selected = state.selectedTab == SearchTab.SERIES,
                onClick = { viewModel.selectTab(SearchTab.SERIES) },
            )
            TabChip(
                label = "قنوات",
                icon = AppIcons.LiveTv,
                selected = state.selectedTab == SearchTab.CHANNELS,
                onClick = { viewModel.selectTab(SearchTab.CHANNELS) },
            )
        }

        Spacer(Modifier.height(8.dp))

        if (state.query.isBlank()) {
            EmptySearchState()
            return@Column
        }

        when (state.selectedTab) {
            SearchTab.MOVIES -> MoviesResults(
                movies = movies,
                onMovieClick = { movie ->
                    navController.navigate(Routes.movieDetails(movie.id))
                },
            )
            SearchTab.SERIES -> SeriesResults(
                series = series,
                onSeriesClick = { s ->
                    navController.navigate(Routes.seriesDetails(s.id))
                },
            )
            SearchTab.CHANNELS -> ChannelsResults(
                channels = channels,
                onChannelClick = { c ->
                    navController.navigate(Routes.player(c.url, c.name))
                },
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  شريط البحث
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try { focusRequester.requestFocus() } catch (_: Exception) { }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF161B22))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = "بحث",
            tint = Color(0xFF8B949E),
            modifier = Modifier.size(24.dp),
        )

        Spacer(Modifier.width(12.dp))

        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = "ابحث عن فيلم، مسلسل، أو قناة...",
                    color = Color(0xFF8B949E),
                    fontSize = 16.sp,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 16.sp,
                ),
                cursorBrush = SolidColor(Color(0xFF1F6FEB)),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
        }

        if (query.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Default.Close,
                contentDescription = "مسح",
                tint = Color.White,
                modifier = Modifier
                    .size(24.dp)
                    .tvFocusable(onClick = onClear),
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  تبويب مع أيقونة
// ═══════════════════════════════════════════════════════════════

@Composable
private fun TabChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (selected) Color(0xFF1F6FEB) else Color(0xFF21262D)
    Row(
        modifier = Modifier
            .tvFocusable(onClick = onClick)
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  نتائج الأفلام
// ═══════════════════════════════════════════════════════════════

@Composable
private fun MoviesResults(
    movies: androidx.paging.compose.LazyPagingItems<MovieEntity>,
    onMovieClick: (MovieEntity) -> Unit,
) {
    if (movies.itemCount == 0) {
        NoResultsState()
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 180.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(
            count = movies.itemCount,
            key = { idx -> movies[idx]?.id ?: idx },
        ) { idx ->
            val movie = movies[idx]
            if (movie != null) {
                PosterCard(
                    title = movie.name,
                    imageUrl = movie.poster,
                    subtitle = buildSubtitle(movie.rating, movie.year),
                    onClick = { onMovieClick(movie) },
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  نتائج المسلسلات
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SeriesResults(
    series: androidx.paging.compose.LazyPagingItems<SeriesEntity>,
    onSeriesClick: (SeriesEntity) -> Unit,
) {
    if (series.itemCount == 0) {
        NoResultsState()
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 180.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(
            count = series.itemCount,
            key = { idx -> series[idx]?.id ?: idx },
        ) { idx ->
            val s = series[idx]
            if (s != null) {
                PosterCard(
                    title = s.name,
                    imageUrl = s.poster,
                    subtitle = buildSubtitle(s.rating, s.year),
                    onClick = { onSeriesClick(s) },
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  نتائج القنوات
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ChannelsResults(
    channels: androidx.paging.compose.LazyPagingItems<ChannelEntity>,
    onChannelClick: (ChannelEntity) -> Unit,
) {
    if (channels.itemCount == 0) {
        NoResultsState()
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 180.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(
            count = channels.itemCount,
            key = { idx -> channels[idx]?.id ?: idx },
        ) { idx ->
            val c = channels[idx]
            if (c != null) {
                PosterCard(
                    title = c.name,
                    imageUrl = c.logo,
                    subtitle = c.groupName.takeIf { it.isNotBlank() },
                    isChannel = true,
                    onClick = { onChannelClick(c) },
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  الحالات الفارغة — بأيقونات Material
// ═══════════════════════════════════════════════════════════════

@Composable
private fun EmptySearchState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = Color(0xFF30363D),
                modifier = Modifier.size(100.dp),
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "ابحث في مكتبتك",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "اكتب اسم فيلم أو مسلسل أو قناة",
                color = Color(0xFF8B949E),
                fontSize = 15.sp,
            )
        }
    }
}

@Composable
private fun NoResultsState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = Color(0xFF30363D),
                modifier = Modifier.size(80.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "لا توجد نتائج",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "جرّب كلمة أخرى",
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
