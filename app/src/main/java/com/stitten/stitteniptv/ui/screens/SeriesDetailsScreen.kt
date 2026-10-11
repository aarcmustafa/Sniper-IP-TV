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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.stitten.stitteniptv.data.Episode
import com.stitten.stitteniptv.data.Series
import com.stitten.stitteniptv.ui.components.tvFocusable
import com.stitten.stitteniptv.ui.navigation.Routes
import com.stitten.stitteniptv.viewmodel.MainViewModel

@Composable
fun SeriesDetailsScreen(
    navController: NavHostController,
    seriesId: String,
    viewModel: MainViewModel = viewModel()
) {
    var episodes by remember { mutableStateOf<List<Episode>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var selectedSeason by remember { mutableStateOf<Int?>(null) }
    var series by remember { mutableStateOf<Series?>(null) }

    LaunchedEffect(seriesId) {
        // جلب معلومات المسلسل من الذاكرة (ContentRepository)
        series = com.stitten.stitteniptv.data.ContentRepository.series
            .firstOrNull { it.id == seriesId || it.id.endsWith("_$seriesId") }

        viewModel.loadEpisodes(seriesId) {
            episodes = it
            loading = false
            selectedSeason = it.firstOrNull()?.season
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        when {
            loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF1F6FEB))
                        Spacer(Modifier.height(16.dp))
                        Text("جارٍ تحميل الحلقات...", color = Color.White, fontSize = 16.sp)
                    }
                }
            }

            episodes.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📺", fontSize = 64.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("لا توجد حلقات", color = Color.White, fontSize = 20.sp)
                        Spacer(Modifier.height(16.dp))
                        Box(
                            Modifier
                                .tvFocusable { navController.popBackStack() }
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1F6FEB))
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Text("← رجوع", color = Color.White, fontSize = 15.sp)
                        }
                    }
                }
            }

            else -> SeriesContent(
                series = series,
                episodes = episodes,
                selectedSeason = selectedSeason,
                onSeasonSelected = { selectedSeason = it },
                onEpisodeClick = { ep ->
                    navController.navigate(Routes.player(ep.url, ep.title))
                },
                onBack = { navController.popBackStack() },
            )
        }
    }
}

@Composable
private fun SeriesContent(
    series: Series?,
    episodes: List<Episode>,
    selectedSeason: Int?,
    onSeasonSelected: (Int) -> Unit,
    onEpisodeClick: (Episode) -> Unit,
    onBack: () -> Unit,
) {
    val seasons = episodes.map { it.season }.distinct().sorted()
    val visibleEpisodes = if (selectedSeason == null) episodes
    else episodes.filter { it.season == selectedSeason }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        // ─── Hero ───
        item(key = "hero") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
            ) {
                AsyncImage(
                    model = series?.backdrop ?: series?.poster,
                    contentDescription = series?.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )

                // تدرج داكن
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

                // زر الرجوع أعلى يمين
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

                // معلومات المسلسل أسفل
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 24.dp)
                ) {
                    Text(
                        text = series?.name ?: "المسلسل",
                        color = Color.White,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    val infoLine = buildList {
                        if (!series?.year.isNullOrBlank()) add(series!!.year!!)
                        if (!series?.rating.isNullOrBlank()) add("⭐ ${series!!.rating}")
                        if (!series?.genre.isNullOrBlank()) add(series!!.genre!!)
                        add("${episodes.size} حلقة")
                    }.joinToString("  ·  ")

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = infoLine,
                        color = Color(0xFFD0D7DE),
                        fontSize = 15.sp,
                    )

                    if (!series?.plot.isNullOrBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = series!!.plot!!,
                            color = Color(0xFFA8B3C1),
                            fontSize = 14.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        // ─── تبويبات المواسم ───
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
                        val bg = if (isSelected) Color(0xFF1F6FEB) else Color(0xFF21262D)
                        Text(
                            text = "الموسم $season",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
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

        // ─── عنوان الحلقات ───
        item(key = "episodes_header") {
            Text(
                text = "الحلقات",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
            )
        }

        // ─── قائمة الحلقات ───
        items(visibleEpisodes, key = { it.id }) { ep ->
            EpisodeRow(
                episode = ep,
                onClick = { onEpisodeClick(ep) },
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
            // رقم الحلقة
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
                    if (episode.duration?.isNotBlank() == true) add(episode.duration!!)
                    if (episode.rating?.isNotBlank() == true) add("⭐ ${episode.rating}")
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
