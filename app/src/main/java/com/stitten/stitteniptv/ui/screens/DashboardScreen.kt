package com.stitten.stitteniptv.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.stitten.stitteniptv.data.ChannelRepository
import com.stitten.stitteniptv.data.ContentType
import com.stitten.stitteniptv.data.PrefsManager
import com.stitten.stitteniptv.database.entity.ChannelEntity
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

    val entryPoint = remember {
        EntryPointAccessors.fromApplication(
            ctx.applicationContext,
            PlayerEntryPoint::class.java
        )
    }
    val channelRepo = entryPoint.channelRepository()

    LaunchedEffect(Unit) {
        if (uiState.channelsCount == 0) {
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

    val channelGroups by channelRepo.getChannelGroups().collectAsState(initial = emptyList())
    val movieCategories by channelRepo.getMovieCategories().collectAsState(initial = emptyList())
    val seriesCategories by channelRepo.getSeriesCategories().collectAsState(initial = emptyList())

    val categories: List<String> = when (contentType) {
        ContentType.LIVE -> channelGroups
        ContentType.VOD -> movieCategories
        ContentType.SERIES -> seriesCategories
        ContentType.FAVORITES -> emptyList()
    }
    
Box(modifier = Modifier.fillMaxSize()) {

    Image(
        painter = painterResource(id = R.drawable.bg_main),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
    )

    Row(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .width(300.dp)
                .fillMaxHeight()
                .background(Color(0xFF161B22).copy(alpha = 0.85f))
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
                selectedCategory = null
            }
            SidebarButton("🎬 أفلام", contentType == ContentType.VOD) {
                contentType = ContentType.VOD
                selectedCategory = null
            }
            SidebarButton("📼 مسلسلات", contentType == ContentType.SERIES) {
                contentType = ContentType.SERIES
                selectedCategory = null
            }
            SidebarButton("❤️ المفضلة", contentType == ContentType.FAVORITES) {
                contentType = ContentType.FAVORITES
                selectedCategory = null
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
                        CategoryChip("الكل", selectedCategory == null) {
                            selectedCategory = null
                        }
                    }
                    items(categories, key = { it }) { cat ->
                        CategoryChip(cat, selectedCategory == cat) {
                            selectedCategory = cat
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            TextButton(
                onClick = { navController.navigate(Routes.SOURCES) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("📡 إدارة المصادر", color = Color.White, fontSize = 16.sp)
            }

            TextButton(
                onClick = { navController.navigate(Routes.SETTINGS) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text("الإعدادات", color = Color.White, fontSize = 16.sp)
            }
        }
        
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {

                when (contentType) {
                    ContentType.LIVE -> LiveContent(
                        channelRepo, viewModel, selectedCategory, search, navController
                    )
                    ContentType.VOD -> VodContent(
                        channelRepo, viewModel, selectedCategory, search, navController
                    )
                    ContentType.SERIES -> SeriesContent(
                        channelRepo, viewModel, selectedCategory, search, navController
                    )
                    ContentType.FAVORITES -> FavoritesPlaceholder()
                }

                if (uiState.isLoading) {
                    LoadingOverlay(uiState.loadingProgress, uiState.loadingMessage)
                }
            }
        }
    }
}

@Composable
private fun LiveContent(
    repo: ChannelRepository,
    viewModel: MainViewModel,
    selectedCategory: String?,
    search: String,
    navController: NavHostController
) {
    val pagingItems = remember(selectedCategory, search) {
        when {
            search.isNotBlank() -> repo.searchChannelsPaged(search)
            selectedCategory != null -> repo.getChannelsByGroupPaged(selectedCategory)
            else -> repo.getAllChannelsPaged()
        }
    }.collectAsLazyPagingItems()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(pagingItems.itemCount) { index ->
            val entity = pagingItems[index]
            if (entity != null) {
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

@Composable
private fun VodContent(
    repo: ChannelRepository,
    viewModel: MainViewModel,
    selectedCategory: String?,
    search: String,
    navController: NavHostController
) {
    val pagingItems = remember(selectedCategory, search) {
        when {
            search.isNotBlank() -> repo.searchMoviesPaged(search)
            selectedCategory != null -> repo.getMoviesByCategoryPaged(selectedCategory)
            else -> repo.getAllMoviesPaged()
        }
    }.collectAsLazyPagingItems()

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(pagingItems.itemCount) { index ->
            val entity = pagingItems[index]
            if (entity != null) {
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
