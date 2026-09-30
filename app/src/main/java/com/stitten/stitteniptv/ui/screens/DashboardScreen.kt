package com.stitten.stitteniptv.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.stitten.stitteniptv.R
import com.stitten.stitteniptv.data.Channel
import com.stitten.stitteniptv.data.ContentType
import com.stitten.stitteniptv.data.Movie
import com.stitten.stitteniptv.data.PrefsManager
import com.stitten.stitteniptv.data.Series
import com.stitten.stitteniptv.database.entity.ChannelEntity
import com.stitten.stitteniptv.database.entity.MovieEntity
import com.stitten.stitteniptv.database.entity.SeriesEntity
import com.stitten.stitteniptv.ui.components.tvFocusable
import com.stitten.stitteniptv.ui.navigation.Routes
import com.stitten.stitteniptv.viewmodel.MainViewModel
import dagger.hilt.android.EntryPointAccessors
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

    val entryPoint = remember {
        EntryPointAccessors.fromApplication(
            ctx.applicationContext,
            PlayerEntryPoint::class.java
        )
    }
    val channelRepo = entryPoint.channelRepository()
    val historyMgr = entryPoint.watchHistoryManager()
    val recent by historyMgr.getRecent().collectAsState(initial = emptyList())

    LaunchedEffect(Unit) {
        if (uiState.channelsCount == 0 &&
            uiState.moviesCount == 0 &&
            uiState.seriesCount == 0
        ) {
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

    // قائمة التصنيفات
    val channelGroups by channelRepo.getChannelGroups()
        .collectAsState(initial = emptyList())
    val movieCategories by channelRepo.getMovieCategories()
        .collectAsState(initial = emptyList())
    val seriesCategories by channelRepo.getSeriesCategories()
        .collectAsState(initial = emptyList())

    val categories: List<String> = when (contentType) {
        ContentType.LIVE -> channelGroups
        ContentType.VOD -> movieCategories
        ContentType.SERIES -> seriesCategories
        ContentType.FAVORITES -> emptyList()
    }

    LaunchedEffect(contentType) {
        selectedCategory = null
    }
    
            // منطقة العرض
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {

                when (contentType) {

                    ContentType.LIVE -> {
                        // استخدام Paging للقنوات
                        val pagingItems = remember(selectedCategory, search) {
                            when {
                                search.isNotBlank() -> channelRepo.searchChannels(search)
                                selectedCategory != null -> channelRepo.getChannelsByGroup(selectedCategory!!)
                                else -> channelRepo.getAllChannels()
                            }
                        }.collectAsLazyPagingItems()

                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(pagingItems.itemCount) { index ->
                                pagingItems[index]?.let { entity ->
                                    ChannelRowFromEntity(
                                        entity = entity,
                                        isFavorite = viewModel.favorites.isChannelFavorite(entity.id),
                                        onToggleFavorite = {
                                            viewModel.toggleChannelFavorite(entity.id, entity.name)
                                        },
                                        onClick = {
                                            navController.navigate(Routes.player(entity.url, entity.name))
                                        }
                                    )
                                }
                            }
                        }
                    }

                    ContentType.VOD -> {
                        val pagingItems = remember(selectedCategory, search) {
                            when {
                                search.isNotBlank() -> channelRepo.searchMovies(search)
                                selectedCategory != null -> channelRepo.getMoviesByCategory(selectedCategory!!)
                                else -> channelRepo.getAllMovies()
                            }
                        }.collectAsLazyPagingItems()

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(8.dp)
                        ) {
                            items(pagingItems.itemCount) { index ->
                                pagingItems[index]?.let { entity ->
                                    MediaCardFromEntity(
                                        name = entity.name,
                                        poster = entity.poster,
                                        isFavorite = viewModel.favorites.isMovieFavorite(entity.id),
                                        onToggleFavorite = {
                                            viewModel.toggleMovieFavorite(entity.id, entity.name)
                                        },
                                        onClick = {
                                            navController.navigate(Routes.player(entity.url, entity.name))
                                        }
                                    )
                                }
                            }
                        }
                    }

                    ContentType.SERIES -> {
                        val pagingItems = remember(selectedCategory, search) {
                            when {
                                search.isNotBlank() -> channelRepo.searchSeries(search)
                                selectedCategory != null -> channelRepo.getSeriesByCategory(selectedCategory!!)
                                else -> channelRepo.getAllSeries()
                            }
                        }.collectAsLazyPagingItems()

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(8.dp)
                        ) {
                            items(pagingItems.itemCount) { index ->
                                pagingItems[index]?.let { entity ->
                                    MediaCardFromEntity(
                                        name = entity.name,
                                        poster = entity.poster,
                                        isFavorite = viewModel.favorites.isSeriesFavorite(entity.id),
                                        onToggleFavorite = {
                                            viewModel.toggleSeriesFavorite(entity.id, entity.name)
                                        },
                                        onClick = {
                                            navController.navigate(Routes.seriesDetails(entity.id))
                                        }
                                    )
                                }
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

                // نسبة التحميل
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.9f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF161B22)
                            ),
                            modifier = Modifier.width(500.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(160.dp)
                                ) {
                                    CircularProgressIndicator(
                                        progress = { uiState.loadingProgress / 100f },
                                        modifier = Modifier.size(160.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 10.dp,
                                        trackColor = Color(0xFF21262D)
                                    )
                                    Text(
                                        "${uiState.loadingProgress}%",
                                        color = Color.White,
                                        fontSize = 40.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(Modifier.height(24.dp))

                                Text(
                                    uiState.loadingMessage.ifBlank { "جاري التحميل..." },
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(Modifier.height(16.dp))

                                LinearProgressIndicator(
                                    progress = { uiState.loadingProgress / 100f },
                                    modifier = Modifier.fillMaxWidth().height(6.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = Color(0xFF21262D)
                                )

                                Spacer(Modifier.height(16.dp))

                                Text(
                                    "قد تستغرق السيرفرات الكبيرة دقيقة",
                                    color = Color.Gray,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
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
private fun ChannelRowFromEntity(
    entity: ChannelEntity,
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
                model = entity.logo,
                contentDescription = null,
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entity.name,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (entity.groupName.isNotEmpty()) {
                    Text(entity.groupName, color = Color.Gray, fontSize = 14.sp)
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
private fun MediaCardFromEntity(
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
                    onToggleFavorite = { viewModel.toggleChannelFavorite(ch.id, ch.name) },
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
                                onToggleFavorite = {
                                    viewModel.toggleMovieFavorite(mv.id, mv.name)
                                },
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
                                onToggleFavorite = {
                                    viewModel.toggleSeriesFavorite(sr.id, sr.name)
                                },
                                onClick = { onSeriesClick(sr) }
                            )
                        }
                    }
                }
            }
        }
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
