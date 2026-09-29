package com.stitten.stitteniptv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.stitten.stitteniptv.data.CategoriesManager
import com.stitten.stitteniptv.data.Channel
import com.stitten.stitteniptv.data.ContentType
import com.stitten.stitteniptv.data.Movie
import com.stitten.stitteniptv.data.PrefsManager
import com.stitten.stitteniptv.data.Series
import com.stitten.stitteniptv.ui.components.tvFocusable
import com.stitten.stitteniptv.ui.navigation.Routes
import com.stitten.stitteniptv.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    navController: NavHostController,
    viewModel: MainViewModel = viewModel()
) {
    val ctx = LocalContext.current
    val prefs = remember { PrefsManager(ctx) }
    val uiState by viewModel.uiState.collectAsState()
    val favoritesVersion by viewModel.favoritesVersion.collectAsState()

    LaunchedEffect(Unit) {
        if (uiState.channels.isEmpty() && uiState.movies.isEmpty() && uiState.series.isEmpty()) {
            viewModel.loadCachedContent()
        }
    }

    var contentType by remember { mutableStateOf(ContentType.LIVE) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    var currentTime by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val pattern = if (prefs.is24Hour) "HH:mm:ss" else "hh:mm:ss a"
            currentTime = SimpleDateFormat(pattern, Locale.getDefault()).format(Date())
            delay(1000)
        }
    }

    val categories: List<String> = remember(contentType, uiState, favoritesVersion) {
        when (contentType) {
            ContentType.LIVE -> CategoriesManager.extractChannelCategories(uiState.channels)
            ContentType.VOD -> CategoriesManager.extractMovieCategories(uiState.movies)
            ContentType.SERIES -> CategoriesManager.extractSeriesCategories(uiState.series)
            ContentType.FAVORITES -> emptyList()
        }
    }

    LaunchedEffect(contentType) {
        selectedCategory = null
    }

    Row(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        Column(
            modifier = Modifier
                .width(300.dp)
                .fillMaxHeight()
                .background(Color(0xFF161B22))
                .padding(16.dp)
        ) {
            Text(
                "STTITEN IP TV",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(currentTime, color = Color.White, fontSize = 16.sp)
            Spacer(Modifier.height(16.dp))

            SidebarButton("📺 بث حي", contentType == ContentType.LIVE) {
                contentType = ContentType.LIVE
            }
            SidebarButton("🎬 أفلام", contentType == ContentType.VOD) {
                contentType = ContentType.VOD
            }
            SidebarButton("📼 مسلسلات", contentType == ContentType.SERIES) {
                contentType = ContentType.SERIES
            }
            SidebarButton("❤️ المفضلة", contentType == ContentType.FAVORITES) {
                contentType = ContentType.FAVORITES
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("بحث", fontSize = 14.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            if (categories.isNotEmpty()) {
                Text(
                    "التصنيفات",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))

                LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                    item {
                        CategoryChip(
                            name = "الكل",
                            selected = selectedCategory == null
                        ) { selectedCategory = null }
                    }
                    items(categories, key = { it }) { cat ->
                        CategoryChip(
                            name = cat,
                            selected = selectedCategory == cat
                        ) { selectedCategory = cat }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            TextButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text("الإعدادات", color = Color.White, fontSize = 16.sp)
            }
        }

        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            when (contentType) {

                ContentType.LIVE -> {
                    val list = uiState.channels
                        .filter { ch ->
                            (selectedCategory == null || ch.group == selectedCategory) &&
                            (search.isBlank() || ch.name.contains(search, true))
                        }
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(list, key = { it.id }) { ch ->
                            ChannelRow(
                                channel = ch,
                                isFavorite = viewModel.favorites.isChannelFavorite(ch.id),
                                onToggleFavorite = {
                                    viewModel.toggleChannelFavorite(ch.id)
                                },
                                onClick = {
                                    navController.navigate(Routes.player(ch.url, ch.name))
                                }
                            )
                        }
                    }
                }

                ContentType.VOD -> {
                    val list = uiState.movies
                        .filter { mv ->
                            (selectedCategory == null || mv.category == selectedCategory) &&
                            (search.isBlank() || mv.name.contains(search, true))
                        }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        items(list, key = { it.id }) { mv ->
                            MediaCard(
                                name = mv.name,
                                poster = mv.poster,
                                isFavorite = viewModel.favorites.isMovieFavorite(mv.id),
                                onToggleFavorite = {
                                    viewModel.toggleMovieFavorite(mv.id)
                                },
                                onClick = {
                                    navController.navigate(Routes.player(mv.url, mv.name))
                                }
                            )
                        }
                    }
                }

                ContentType.SERIES -> {
                    val list = uiState.series
                        .filter { sr ->
                            (selectedCategory == null || sr.category == selectedCategory) &&
                            (search.isBlank() || sr.name.contains(search, true))
                        }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        items(list, key = { it.id }) { sr ->
                            MediaCard(
                                name = sr.name,
                                poster = sr.poster,
                                isFavorite = viewModel.favorites.isSeriesFavorite(sr.id),
                                onToggleFavorite = {
                                    viewModel.toggleSeriesFavorite(sr.id)
                                },
                                onClick = {
                                    navController.navigate(Routes.seriesDetails(sr.id))
                                }
                            )
                        }
                    }
                }

                ContentType.FAVORITES -> {
                    FavoritesContent(
                        viewModel = viewModel,
                        search = search,
                        favoritesVersion = favoritesVersion,
                        onChannelClick = { ch ->
                            navController.navigate(Routes.player(ch.url, ch.name))
                        },
                        onMovieClick = { mv ->
                            navController.navigate(Routes.player(mv.url, mv.name))
                        },
                        onSeriesClick = { sr ->
                            navController.navigate(Routes.seriesDetails(sr.id))
                        }
                    )
                }
            }

            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun SidebarButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary
            else Color(0xFF21262D),
            contentColor = Color.White
        )
    ) { Text(text, fontSize = 16.sp) }
}

@Composable
private fun CategoryChip(name: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .tvFocusable(onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primary
        else Color(0xFF21262D)
    ) {
        Text(
            name,
            color = Color.White,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun ChannelRow(
    channel: Channel,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .tvFocusable(onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = channel.logo,
                contentDescription = null,
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    channel.name,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (channel.group.isNotEmpty()) {
                    Text(channel.group, color = Color.Gray, fontSize = 14.sp)
                }
            }
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "مفضلة",
                    tint = if (isFavorite) Color(0xFFDA3633) else Color.Gray,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun MediaCard(
    name: String,
    poster: String,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .tvFocusable(onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
    ) {
        Column {
            Box {
                AsyncImage(
                    model = poster,
                    contentDescription = name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.7f)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
                    contentScale = ContentScale.Crop
                )
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(40.dp)
                        .background(Color(0x99000000), RoundedCornerShape(50))
                ) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "مفضلة",
                        tint = if (isFavorite) Color(0xFFDA3633) else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Text(
                name,
                color = Color.White,
                fontSize = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(10.dp)
            )
        }
    }
}

@Composable
private fun FavoritesContent(
    viewModel: MainViewModel,
    search: String,
    favoritesVersion: Int,
    onChannelClick: (Channel) -> Unit,
    onMovieClick: (Movie) -> Unit,
    onSeriesClick: (Series) -> Unit
) {
    val favChannels = remember(favoritesVersion) {
        viewModel.getFavoriteChannels().filter {
            search.isBlank() || it.name.contains(search, true)
        }
    }
    val favMovies = remember(favoritesVersion) {
        viewModel.getFavoriteMovies().filter {
            search.isBlank() || it.name.contains(search, true)
        }
    }
    val favSeries = remember(favoritesVersion) {
        viewModel.getFavoriteSeries().filter {
            search.isBlank() || it.name.contains(search, true)
        }
    }

    if (favChannels.isEmpty() && favMovies.isEmpty() && favSeries.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("⭐", fontSize = 72.sp, color = Color.Gray)
                Spacer(Modifier.height(16.dp))
                Text(
                    "لا توجد عناصر في المفضلة بعد",
                    color = Color.White,
                    fontSize = 22.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "اضغط على ⭐ بجانب أي قناة أو فيلم أو مسلسل لإضافته",
                    color = Color.Gray,
                    fontSize = 16.sp
                )
            }
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {

        if (favChannels.isNotEmpty()) {
            item {
                Text(
                    "📺 قنوات مفضلة",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
            items(favChannels, key = { "fav_ch_${it.id}" }) { ch ->
                ChannelRow(
                    channel = ch,
                    isFavorite = true,
                    onToggleFavorite = { viewModel.toggleChannelFavorite(ch.id) },
                    onClick = { onChannelClick(ch) }
                )
            }
        }

        if (favMovies.isNotEmpty()) {
            item {
                Text(
                    "🎬 أفلام مفضلة",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
            item {
                LazyRow {
                    items(favMovies, key = { "fav_mv_${it.id}" }) { mv ->
                        Box(modifier = Modifier.width(200.dp)) {
                            MediaCard(
                                name = mv.name,
                                poster = mv.poster,
                                isFavorite = true,
                                onToggleFavorite = { viewModel.toggleMovieFavorite(mv.id) },
                                onClick = { onMovieClick(mv) }
                            )
                        }
                    }
                }
            }
        }

        if (favSeries.isNotEmpty()) {
            item {
                Text(
                    "📼 مسلسلات مفضلة",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
            item {
                LazyRow {
                    items(favSeries, key = { "fav_sr_${it.id}" }) { sr ->
                        Box(modifier = Modifier.width(200.dp)) {
                            MediaCard(
                                name = sr.name,
                                poster = sr.poster,
                                isFavorite = true,
                                onToggleFavorite = { viewModel.toggleSeriesFavorite(sr.id) },
                                onClick = { onSeriesClick(sr) }
                            )
                        }
                    }
                }
            }
        }
    }
}
