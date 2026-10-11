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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.stitten.stitteniptv.ui.theme.AppIcons
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
        try {
            series = channelRepo.getSeriesById(seriesId)
        } catch (_: Exception) { }

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

        isFavorite = try {
            favorites.getFavoriteSeriesIds().contains(seriesId)
        } catch (_: Exception) { false }

        val numericId = if (seriesId.contains("_")) {
            seriesId.substringAfterLast("_")
        } else {
            seriesId
        }

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

            series == null && episodes.isEmpty() -> EmptyEpisodesView(
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
            androidx.compose.material3.Icon(
                imageVector = AppIcons.Series,
                contentDescription = null,
                tint = Color(0xFF30363D),
                modifier = Modifier.size(80.dp),
            )
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
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        // ═══════════════════════════════════════════════════════
        //  Hero
        // ═══════════════════════════════════════════════════════
        item(key = "hero") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
            ) {
                AsyncImage(
                    model = series?.backdrop ?: series?.poster,
                    contentDescription = series?.name,
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

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(20.dp)
                        .tvFocusable(onClick = onBack)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text("← رجوع", color = Color.White, fontSize = 14.sp)
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 24.dp)
                ) {
                    Text(
                        text = series?.name ?: "المسلسل",
                        color = Color.White,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    val infoLine = buildList {
                        if (!series?.year.isNullOrBlank()) add(series!!.year!!)
                        if (!series?.rating.isNullOrBlank()) add("⭐ ${series!!.rating}")
                        if (!series?.genre.isNullOrBlank()) add(series!!.genre!!)
                        if (episodes.isNotEmpty()) add("${episodes.size} حلقة")
                    }.joinToString("  ·  ")

                    if (infoLine.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = infoLine,
                            color = Color(0xFFD0D7DE),
                            fontSize = 15.sp,
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    FavoriteSeriesButton(
                        isFavorite = isFavorite,
                        onClick = onToggleFavorite,
                    )
                }
            }
        }

        // ═══════════════════════════════════════════════════════
        //  التبويبات
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
                    SeriesOverviewTab(series = series)
                }
            }

            SeriesDetailTab.CAST -> {
                item(key = "cast") {
                    SeriesCastTab(series = series)
                }
            }

            SeriesDetailTab.EPISODES -> {
                // ─── المواسم ───
                val seasons = episodes.map { it.season }.distinct().sorted()
                if (seasons.size > 1) {
                    item(key = "seasons") {
                        Spacer(Modifier.height(8.dp))
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentPadding = PaddingValues(horizontal = 32.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(seasons) { season ->
                                val isSelected = season == selectedSeason
                                val bg = if (isSelected) Color(0xFF1F6FEB)
                                else Color(0xFF21262D)
                                Text(
                                    text = "الموسم $season",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold
                                    else FontWeight.Normal,
                                    modifier = Modifier
                                        .tvFocusable { onSeasonSelected(season) }
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(bg)
                                        .padding(horizontal = 20.dp, vertical = 10.dp),
                                )
                            }
                        }
                    }
                }

                // ─── الحلقات ───
                val visible = if (selectedSeason == null) episodes
                else episodes.filter { it.season == selectedSeason }

                items(visible, key = { it.id }) { ep ->
                    EpisodeRow(
                        episode = ep,
                        onClick = { onEpisodeClick(ep) },
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
private fun SeriesOverviewTab(series: SeriesEntity?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 8.dp)
    ) {
        if (!series?.plot.isNullOrBlank()) {
            SectionTitle(text = "القصة")
            Text(
                text = series!!.plot!!,
                color = Color(0xFFD0D7DE),
                fontSize = 15.sp,
                lineHeight = 24.sp,
            )
            Spacer(Modifier.height(24.dp))
        }

        val details = buildList {
            if (!series?.director.isNullOrBlank()) add("المخرج" to series!!.director!!)
            if (!series?.genre.isNullOrBlank()) add("التصنيف" to series!!.genre!!)
            if (!series?.year.isNullOrBlank()) add("السنة" to series!!.year!!)
            if (!series?.category.isNullOrBlank()) add("الفئة" to series!!.category)
            if (series != null && series.episodeCount > 0) {
                add("عدد الحلقات" to "${series.episodeCount}")
            }
            if (series != null && series.seasonCount > 0) {
                add("عدد المواسم" to "${series.seasonCount}")
            }
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
private fun SeriesCastTab(series: SeriesEntity?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 8.dp)
    ) {
        if (series?.cast.isNullOrBlank() && series?.director.isNullOrBlank()) {
            EmptyTabState(message = "لا توجد معلومات عن الفريق")
            return@Column
        }

        if (!series?.director.isNullOrBlank()) {
            SectionTitle(text = "المخرج")
            PersonChip(name = series!!.director!!, role = "مخرج")
            Spacer(Modifier.height(20.dp))
        }

        if (!series?.cast.isNullOrBlank()) {
            SectionTitle(text = "الممثلون")
            val actors = series!!.cast!!
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

@Composable
private fun EpisodeRow(
    episode: Episode,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 6.dp)
            .tvFocusable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF161B22),
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1F6FEB).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${episode.episode}",
                    color = Color(0xFF1F6FEB),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = episode.title.ifBlank { "الحلقة ${episode.episode}" },
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                val meta = buildList {
                    if (!episode.duration.isNullOrBlank()) add(episode.duration!!)
                    if (!episode.rating.isNullOrBlank()) add("⭐ ${episode.rating}")
                }.joinToString("  ·  ")

                if (meta.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = meta,
                        color = Color(0xFF8B949E),
                        fontSize = 13.sp,
                    )
                }
            }

            Text(
                text = "▶",
                color = Color(0xFF1F6FEB),
                fontSize = 22.sp,
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
private fun FavoriteSeriesButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
) {
    val text = if (isFavorite) "★  في المفضلة" else "☆  أضف للمفضلة"
    val bg = if (isFavorite) Color(0xFFF85149).copy(alpha = 0.25f) else Color(0xFF30363D)
    val textColor = if (isFavorite) Color(0xFFF85149) else Color.White

    Text(
        text = text,
        color = textColor,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .tvFocusable(onClick = onClick)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    )
}
