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
import com.stitten.stitteniptv.data.CategoriesManager
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
    var selectedMainCategory by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    var currentTime by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val pattern = if (prefs.is24Hour) "HH:mm:ss" else "hh:mm:ss a"
            currentTime = SimpleDateFormat(pattern, Locale.getDefault()).format(Date())
            delay(1000)
        }
    }
    
Box(modifier = Modifier.fillMaxSize()) {
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
            modifier = Modifier.fillMaxSize().background(Color(0xFF0D1117))
        )
    }

    Row(modifier = Modifier.fillMaxSize()) {

        // ============== الشريط الجانبي ==============
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
            if (uiState.currentSourceName.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "📡 ${uiState.currentSourceName}",
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(12.dp))

            // فئات رئيسية (3 فقط)
            MainCategoryButton(
                "📺 بث حي",
                contentType == ContentType.LIVE,
                null == selectedMainCategory
            ) {
                contentType = ContentType.LIVE
                selectedMainCategory = null
            }
            MainCategoryButton(
                "🎬 أفلام",
                contentType == ContentType.VOD,
                null == selectedMainCategory
            ) {
                contentType = ContentType.VOD
                selectedMainCategory = null
            }
            MainCategoryButton(
                "📼 مسلسلات",
                contentType == ContentType.SERIES,
                null == selectedMainCategory
            ) {
                contentType = ContentType.SERIES
                selectedMainCategory = null
            }
            MainCategoryButton(
                "❤️ المفضلة",
                contentType == ContentType.FAVORITES,
                null == selectedMainCategory
            ) {
                contentType = ContentType.FAVORITES
                selectedMainCategory = null
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFF30363D))
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("بحث", fontSize = 14.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            // الفئات الرئيسية الثلاث
            if (contentType != ContentType.FAVORITES) {
                Text(
                    "الفئات",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))

                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    item {
                        CategoryChip(
                            "الكل",
                            selectedMainCategory == null
                        ) { selectedMainCategory = null }
                    }
                    items(CategoriesManager.getMainCategories(), key = { it }) { cat ->
                        CategoryChip(
                            cat,
                            selectedMainCategory == cat
                        ) { selectedMainCategory = cat }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            TextButton(
                onClick = { navController.navigate(Routes.SOURCES) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("📡 إدارة المصادر", color = Color.White, fontSize = 15.sp)
            }

            TextButton(
                onClick = { navController.navigate(Routes.SETTINGS) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text("الإعدادات", color = Color.White, fontSize = 15.sp)
            }
        }
        
            // ============== منطقة العرض ==============
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {

                when (contentType) {
                    ContentType.LIVE -> LiveContent(
                        channelRepo, viewModel, selectedMainCategory, search, navController
                    )
                    ContentType.VOD -> VodContent(
                        channelRepo, viewModel, selectedMainCategory, search, navController
                    )
                    ContentType.SERIES -> SeriesContent(
                        channelRepo, viewModel, selectedMainCategory, search, navController
                    )
                    ContentType.FAVORITES -> FavoritesPlaceholder()
                }

                if (uiState.isLoading) {
                    LoadingOverlay(uiState.loadingProgress, uiState.loadingMessage)
                }

                // شريط تحميل في الخلفية
                if (uiState.backgroundLoading && !uiState.isLoading) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF161B22).copy(alpha = 0.95f)
                        ),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "جاري تحميل المحتوى الإضافي...",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============== LiveContent مع الفلترة بالتصنيف الرئيسي ==============
@Composable
private fun LiveContent(
    repo: ChannelRepository,
    viewModel: MainViewModel,
    mainCategory: String?,
    search: String,
    navController: NavHostController
) {
    val pagingItems = remember(mainCategory, search) {
        when {
            search.isNotBlank() -> repo.searchChannelsPaged(search)
            else -> repo.getAllChannelsPaged()
        }
    }.collectAsLazyPagingItems()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(pagingItems.itemCount) { index ->
            val entity = pagingItems[index]
            if (entity != null) {
                // فلترة حسب الفئة الرئيسية
                val entityMainCat = CategoriesManager.getMainCategory(entity.groupName)
                val matchesFilter = mainCategory == null || mainCategory == entityMainCat

                if (matchesFilter) {
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
}

@Composable
private fun VodContent(
    repo: ChannelRepository,
    viewModel: MainViewModel,
    mainCategory: String?,
    search: String,
    navController: NavHostController
) {
    val pagingItems = remember(search) {
        if (search.isNotBlank()) repo.searchMoviesPaged(search)
        else repo.getAllMoviesPaged()
    }.collectAsLazyPagingItems()

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(pagingItems.itemCount) { index ->
            val entity = pagingItems[index]
            if (entity != null) {
                val entityMainCat = CategoriesManager.getMainCategory(entity.category)
                val matchesFilter = mainCategory == null || mainCategory == entityMainCat

                if (matchesFilter) {
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
}

@Composable
private fun SeriesContent(
    repo: ChannelRepository,
    viewModel: MainViewModel,
    mainCategory: String?,
    search: String,
    navController: NavHostController
) {
    val pagingItems = remember(search) {
        if (search.isNotBlank()) repo.searchSeriesPaged(search)
        else repo.getAllSeriesPaged()
    }.collectAsLazyPagingItems()

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(pagingItems.itemCount) { index ->
            val entity = pagingItems[index]
            if (entity != null) {
                val entityMainCat = CategoriesManager.getMainCategory(entity.category)
                val matchesFilter = mainCategory == null || mainCategory == entityMainCat

                if (matchesFilter) {
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
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)),
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
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
                    CircularProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.size(180.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 12.dp,
                        trackColor = Color(0xFF21262D)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$progress%",
                            color = Color.White,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
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
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color(0xFF21262D)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "السيرفرات الكبيرة قد تستغرق دقائق",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun MainCategoryButton(
    text: String,
    isContentTypeSelected: Boolean,
    isMainCatSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isContentTypeSelected && isMainCatSelected)
                MaterialTheme.colorScheme.primary else Color(0xFF21262D),
            contentColor = Color.White
        )
    ) { Text(text, fontSize = 16.sp) }
}

@Composable
private fun CategoryChip(name: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .tvFocusable(onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else Color(0xFF21262D)
    ) {
        Text(
            name,
            color = Color.White,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
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
                    Text(entity.groupName, color = Color.Gray, fontSize = 14.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
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
