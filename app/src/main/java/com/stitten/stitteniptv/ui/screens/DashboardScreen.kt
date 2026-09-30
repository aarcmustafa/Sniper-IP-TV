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
    val liteMode = prefs.liteModeEnabled

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

    // الخلفية: صورة في الوضع العادي، لون في Lite
    if (!liteMode) {
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
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0D1117))
        )
    }

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
                Text("التصنيفات", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
            search.isNotBlank() && selectedCategory != null ->
                repo.searchChannelsInGroupPaged(selectedCategory, search)
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
            search.isNotBlank() && selectedCategory != null ->
                repo.searchMoviesInCategoryPaged(selectedCategory, search)
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

@Composable
private fun SeriesContent(
    repo: ChannelRepository,
    viewModel: MainViewModel,
    selectedCategory: String?,
    search: String,
    navController: NavHostController
) {
    val pagingItems = remember(selectedCategory, search) {
        when {
            search.isNotBlank() && selectedCategory != null ->
                repo.searchSeriesInCategoryPaged(selectedCategory, search)
            search.isNotBlank() -> repo.searchSeriesPaged(search)
            selectedCategory != null -> repo.getSeriesByCategoryPaged(selectedCategory)
            else -> repo.getAllSeriesPaged()
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

@Composable
private fun FavoritesPlaceholder() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("⭐", fontSize = 72.sp, color = Color.Gray)
            Spacer(Modifier.height(16.dp))
            Text("لا توجد عناصر في المفضلة", color = Color.White, fontSize = 22.sp)
        }
    }
}

@Composable
private fun LoadingOverlay(progress: Int, message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            modifier = Modifier.width(500.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                    CircularProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.size(160.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 10.dp,
                        trackColor = Color(0xFF21262D)
                    )
                    Text(
                        "$progress%",
                        color = Color.White,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    message.ifBlank { "جاري التحميل..." },
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color(0xFF21262D)
                )
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
            containerColor = if (selected) MaterialTheme.colorScheme.primary else Color(0xFF21262D),
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
        color = if (selected) MaterialTheme.colorScheme.primary else Color(0xFF21262D)
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
            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(48.dp)) {
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
