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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.stitten.stitteniptv.data.ChannelRepository
import com.stitten.stitteniptv.data.ContentRepository
import com.stitten.stitteniptv.data.Episode
import com.stitten.stitteniptv.data.FavoritesManager
import com.stitten.stitteniptv.database.entity.SeriesEntity
import com.stitten.stitteniptv.ui.components.FocusStyle
import com.stitten.stitteniptv.ui.components.tvFocusable
import com.stitten.stitteniptv.ui.navigation.Routes
import com.stitten.stitteniptv.ui.theme.Radius
import com.stitten.stitteniptv.ui.theme.Spacing
import com.stitten.stitteniptv.ui.theme.TvBackground
import com.stitten.stitteniptv.ui.theme.TvSurfaceHigh
import com.stitten.stitteniptv.ui.theme.TvText
import com.stitten.stitteniptv.viewmodel.MainViewModel
import dagger.hilt.android.EntryPointAccessors

enum class SeriesDetailTab { OVERVIEW, CAST, EPISODES }

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
    var selectedTab by remember { mutableStateOf(SeriesDetailTab.EPISODES) }

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

        // ─── 4) تحميل الحلقات ───
        mainViewModel.loadEpisodes(seriesId) { eps ->
            episodes = eps
            selectedSeason = eps.firstOrNull()?.season
            loading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvBackground)
    ) {
        when {
            loading -> SeriesDetailsSkeleton()

            series == null && episodes.isEmpty() -> EmptySeriesView(
                onBack = { navController.popBackStack() },
            )

            else -> SeriesContent(
                series = series,
                episodes = episodes,
                selectedSeason = selectedSeason,
                selectedTab = selectedTab,
                isFavorite = isFavorite,
                onTabSelected = { selectedTab = it },
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

// ═══════════════════════════════════════════════════════════════
//  المحتوى الرئيسي
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SeriesContent(
    series: SeriesEntity?,
    episodes: List<Episode>,
    selectedSeason: Int?,
    selectedTab: SeriesDetailTab,
    isFavorite: Boolean,
    onTabSelected: (SeriesDetailTab) -> Unit,
    onSeasonSelected: (Int) -> Unit,
    onEpisodeClick: (Episode) -> Unit,
    onToggleFavorite: () -> Unit,
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
            SeriesHero(
                series = series,
                episodeCount = episodes.size,
                isFavorite = isFavorite,
                onToggleFavorite = onToggleFavorite,
                onBack = onBack,
            )
        }

        // ═══════════════════════════════════════════════════════
        //  التبويبات
        // ═══════════════════════════════════════════════════════
        item(key = "tabs") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = Spacing.xxxl,
                        vertical = Spacing.lg,
                    ),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                DetailTab(
                    label = "نظرة عامة",
                    selected = selectedTab == SeriesDetailTab.OVERVIEW,
                    onClick = { onTabSelected(SeriesDetailTab.OVERVIEW) },
                )
                DetailTab(
                    label = "الفريق",
                    selected = selectedTab == SeriesDetailTab.CAST,
                    onClick = { onTabSelected(SeriesDetailTab.CAST) },
                )
                DetailTab(
                    label = "الحلقات (${episodes.size})",
                    selected = selectedTab == SeriesDetailTab.EPISODES,
                    onClick = { onTabSelected(SeriesDetailTab.EPISODES) },
                )
            }
        }

        // ═══════════════════════════════════════════════════════
        //  محتوى التبويب
        // ═══════════════════════════════════════════════════════
        when (selectedTab) {
            SeriesDetailTab.OVERVIEW -> {
                item(key = "overview") {
                    SeriesOverviewTab(
                        series = series,
                        episodeCount = episodes.size,
                    )
                }
            }

            SeriesDetailTab.CAST -> {
                item(key = "cast") {
                    SeriesCastTab(series = series)
                }
            }

            SeriesDetailTab.EPISODES -> {
                val seasons = episodes.map { it.season }.distinct().sorted()
                if (seasons.size > 1) {
                    item(key = "seasons") {
                        Spacer(Modifier.height(Spacing.sm))
                        SeasonSelector(
                            seasons = seasons,
                            selectedSeason = selectedSeason,
                            onSeasonSelected = onSeasonSelected,
                        )
                    }
                }

                val visible = if (selectedSeason == null) episodes
                else episodes.filter { it.season == selectedSeason }

                itemsIndexed(
                    items = visible,
                    key = { _, ep -> ep.id },
                ) { index, ep ->
                    EpisodeRow(
                        episode = ep,
                        episodeNumber = index + 1,
                        onClick = { onEpisodeClick(ep) },
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  شريط المواسم
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SeasonSelector(
    seasons: List<Int>,
    selectedSeason: Int?,
    onSeasonSelected: (Int) -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.md),
        contentPadding = PaddingValues(horizontal = Spacing.xxxl),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items(seasons) { season ->
            SeasonChip(
                label = "الموسم $season",
                selected = season == selectedSeason,
                onClick = { onSeasonSelected(season) },
            )
        }
    }
}

@Composable
private fun SeasonChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (selected) SeriesAccent else TvSurfaceHigh
    val textColor = if (selected) Color.White else TvText

    Text(
        text = label,
        color = textColor,
        fontSize = 15.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        modifier = Modifier
            .tvFocusable(
                style = FocusStyle.Chip,
                onClick = onClick,
            )
            .background(
                color = bg,
                shape = RoundedCornerShape(Radius.pill),
            )
            .padding(horizontal = Spacing.xl, vertical = Spacing.sm),
    )
}
