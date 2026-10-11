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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.stitten.stitteniptv.data.ChannelRepository
import com.stitten.stitteniptv.data.ContentRepository
import com.stitten.stitteniptv.data.Episode
import com.stitten.stitteniptv.data.FavoritesManager
import com.stitten.stitteniptv.database.entity.SeriesEntity
import com.stitten.stitteniptv.ui.components.tvFocusable
import com.stitten.stitteniptv.ui.navigation.Routes
import com.stitten.stitteniptv.viewmodel.MainViewModel
import dagger.hilt.android.EntryPointAccessors

@Composable
fun SeriesDetailsScreen(
    navController: NavHostController,
    seriesId: String,
    mainViewModel: MainViewModel = viewModel(),
) {
    val ctx = LocalContext.current

    val entryPoint = remember {
        EntryPointAccessors.fromApplication(
            ctx.applicationContext,
            PlayerEntryPoint::class.java
        )
    }
    val channelRepo: ChannelRepository = remember { entryPoint.channelRepository() }
    val favorites = remember { FavoritesManager(ctx) }

    var episodes by remember { mutableStateOf<List<Episode>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var selectedSeason by remember { mutableStateOf<Int?>(null) }
    var series by remember { mutableStateOf<SeriesEntity?>(null) }
    var isFavorite by remember { mutableStateOf(false) }

    LaunchedEffect(seriesId) {
        // ─── 1) جلب المسلسل من Room ───
        try {
            series = channelRepo.getSeriesById(seriesId)
        } catch (_: Exception) { }

        // ─── 2) احتياطي: من الذاكرة ───
        if (series == null) {
            try {
                val found = ContentRepository.series.firstOrNull {
                    it.id == seriesId || it.id.endsWith("_$seriesId")
                }
                if (found != null) {
                    series = SeriesEntity(
                        id = found.id,
                        name = found.name,
                        poster = found.poster,
                        category = found.category,
                        backdrop = found.backdrop,
                        plot = found.plot,
                        rating = found.rating,
                        year = found.year,
                        genre = found.genre,
                        cast = found.cast,
                        director = found.director,
                        episodeCount = found.episodeCount,
                        seasonCount = found.seasonCount,
                        addedAt = found.addedAt,
                        sourceId = found.sourceId,
                    )
                }
            } catch (_: Exception) { }
        }

        // ─── 3) حالة المفضلة ───
        isFavorite = try {
            favorites.getFavoriteSeriesIds().contains(seriesId)
        } catch (_: Exception) { false }

        // ─── 4) استخراج seriesId الرقمي ───
        val numericId = if (seriesId.contains("_")) {
            seriesId.substringAfterLast("_")
        } else {
            seriesId
        }

        // ─── 5) ✅ جلب الحلقات عبر MainViewModel (Room + الذاكرة + Xtream) ───
        mainViewModel.loadEpisodes(numericId) { eps ->
            episodes = eps
            selectedSeason = eps.firstOrNull()?.season
            loading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        when {
            loading -> LoadingSeriesView()

            episodes.isEmpty() -> EmptyEpisodesView(
                onBack = { navController.popBackStack() },
            )

            else -> SeriesContent(
                series = series,
                episodes = episodes,
                selectedSeason = selectedSeason,
                isFavorite = isFavorite,
                onSeasonSelected = { selectedSeason = it },
                onEpisodeClick = { ep ->
                    navController.navigate(Routes.player(ep.url, ep.title))
                },
                onToggleFavorite = {
                    val newState = try {
                        favorites.toggleSeries(seriesId, series?.name ?: "")
                    } catch (_: Exception) { false }
                    isFavorite = newState
                },
                onBack = { navController.popBackStack() },
            )
        }
    }
}

@Composable
private fun LoadingSeriesView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color(0xFF1F6FEB))
            Spacer(Modifier.height(16.dp))
            Text("جارٍ تحميل الحلقات...", color = Color.White, fontSize = 16.sp)
        }
    }
}

@Composable
private fun EmptyEpisodesView(onBack: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📺", fontSize = 64.sp)
            Spacer(Modifier.height(16.dp))
            Text("لا توجد حلقات", color = Color.White, fontSize = 20.sp)
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier
                    .tvFocusable(onClick = onBack)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1F6FEB))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text("← رجوع", color = Color.White, fontSize = 15.sp)
            }
        }
    }
}
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.stitten.stitteniptv.data.ChannelRepository
import com.stitten.stitteniptv.data.ContentRepository
import com.stitten.stitteniptv.data.Episode
import com.stitten.stitteniptv.data.FavoritesManager
import com.stitten.stitteniptv.database.entity.SeriesEntity
import com.stitten.stitteniptv.ui.components.tvFocusable
import com.stitten.stitteniptv.ui.navigation.Routes
import com.stitten.stitteniptv.viewmodel.MainViewModel
import dagger.hilt.android.EntryPointAccessors

@Composable
fun SeriesDetailsScreen(
    navController: NavHostController,
    seriesId: String,
    mainViewModel: MainViewModel = viewModel(),
) {
    val ctx = LocalContext.current

    val entryPoint = remember {
        EntryPointAccessors.fromApplication(
            ctx.applicationContext,
            PlayerEntryPoint::class.java
        )
    }
    val channelRepo: ChannelRepository = remember { entryPoint.channelRepository() }
    val favorites = remember { FavoritesManager(ctx) }

    var episodes by remember { mutableStateOf<List<Episode>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var selectedSeason by remember { mutableStateOf<Int?>(null) }
    var series by remember { mutableStateOf<SeriesEntity?>(null) }
    var isFavorite by remember { mutableStateOf(false) }

    LaunchedEffect(seriesId) {
        // ─── 1) جلب المسلسل من Room ───
        try {
            series = channelRepo.getSeriesById(seriesId)
        } catch (_: Exception) { }

        // ─── 2) احتياطي: من الذاكرة ───
        if (series == null) {
            try {
                val found = ContentRepository.series.firstOrNull {
                    it.id == seriesId || it.id.endsWith("_$seriesId")
                }
                if (found != null) {
                    series = SeriesEntity(
                        id = found.id,
                        name = found.name,
                        poster = found.poster,
                        category = found.category,
                        backdrop = found.backdrop,
                        plot = found.plot,
                        rating = found.rating,
                        year = found.year,
                        genre = found.genre,
                        cast = found.cast,
                        director = found.director,
                        episodeCount = found.episodeCount,
                        seasonCount = found.seasonCount,
                        addedAt = found.addedAt,
                        sourceId = found.sourceId,
                    )
                }
            } catch (_: Exception) { }
        }

        // ─── 3) حالة المفضلة ───
        isFavorite = try {
            favorites.getFavoriteSeriesIds().contains(seriesId)
        } catch (_: Exception) { false }

        // ─── 4) استخراج seriesId الرقمي ───
        val numericId = if (seriesId.contains("_")) {
            seriesId.substringAfterLast("_")
        } else {
            seriesId
        }

        // ─── 5) ✅ جلب الحلقات عبر MainViewModel (Room + الذاكرة + Xtream) ───
        mainViewModel.loadEpisodes(numericId) { eps ->
            episodes = eps
            selectedSeason = eps.firstOrNull()?.season
            loading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        when {
            loading -> LoadingSeriesView()

            episodes.isEmpty() -> EmptyEpisodesView(
                onBack = { navController.popBackStack() },
            )

            else -> SeriesContent(
                series = series,
                episodes = episodes,
                selectedSeason = selectedSeason,
                isFavorite = isFavorite,
                onSeasonSelected = { selectedSeason = it },
                onEpisodeClick = { ep ->
                    navController.navigate(Routes.player(ep.url, ep.title))
                },
                onToggleFavorite = {
                    val newState = try {
                        favorites.toggleSeries(seriesId, series?.name ?: "")
                    } catch (_: Exception) { false }
                    isFavorite = newState
                },
                onBack = { navController.popBackStack() },
            )
        }
    }
}

@Composable
private fun LoadingSeriesView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color(0xFF1F6FEB))
            Spacer(Modifier.height(16.dp))
            Text("جارٍ تحميل الحلقات...", color = Color.White, fontSize = 16.sp)
        }
    }
}

@Composable
private fun EmptyEpisodesView(onBack: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📺", fontSize = 64.sp)
            Spacer(Modifier.height(16.dp))
            Text("لا توجد حلقات", color = Color.White, fontSize = 20.sp)
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier
                    .tvFocusable(onClick = onBack)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1F6FEB))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text("← رجوع", color = Color.White, fontSize = 15.sp)
            }
        }
    }
}
