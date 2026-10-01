package com.stitten.stitteniptv.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.stitten.stitteniptv.R
import com.stitten.stitteniptv.data.CategoriesManager
import com.stitten.stitteniptv.data.ChannelRepository
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

    var showFavorites by remember { mutableStateOf(false) }
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
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0D1117)))
        }

        Row(modifier = Modifier.fillMaxSize()) {
            SidebarContent(
                currentTime = currentTime,
                sourceName = uiState.currentSourceName,
                showFavorites = showFavorites,
                onShowChannels = { showFavorites = false },
                onShowFavorites = { showFavorites = true },
                search = search,
                onSearchChange = { search = it },
                onSourcesClick = { navController.navigate(Routes.SOURCES) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) }
            )

            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // ✅ key لتجنب اختلاط النصوص عند التنقل السريع
                key(showFavorites, search) {
                    if (showFavorites) {
                        FavoritesContent(viewModel, search, navController)
                    } else {
                        ChannelsContent(channelRepo, viewModel, search, navController)
                    }
                }

                // ✅ دائرة النسبة المئوية
                if (uiState.isLoading) {
                    CircularProgressOverlay(
                        progress = uiState.loadingProgress,
                        message = uiState.loadingMessage
                    )
                }
            }
        }
    }
}

// ============== الشريط الجانبي ==============
@Composable
private fun SidebarContent(
    currentTime: String,
    sourceName: String,
    showFavorites: Boolean,
    onShowChannels: () -> Unit,
    onShowFavorites: () -> Unit,
    search: String,
    onSearchChange: (String) -> Unit,
    onSourcesClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(280.dp)
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
        if (sourceName.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                "📡 $sourceName",
                color = Color.LightGray,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(20.dp))

        SidebarButton("📺 القنوات", !showFavorites, onShowChannels)
        SidebarButton("❤️ المفضلة", showFavorites, onShowFavorites)

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = search,
            onValueChange = onSearchChange,
            label = { Text("بحث", fontSize = 14.sp) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CustomIconButton(
                iconRes = R.drawable.ic_sources_custom,
                label = "المصادر",
                onClick = onSourcesClick
            )
            CustomIconButton(
                iconRes = R.drawable.ic_settings_custom,
                label = "الإعدادات",
                onClick = onSettingsClick
            )
        }
    }
}

// ============== عرض الباقات والقنوات ==============
@Composable
private fun ChannelsContent(
    repo: ChannelRepository,
    viewModel: MainViewModel,
    search: String,
    navController: NavHostController
) {
    var selectedPackage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(search) {
        if (search.isNotBlank()) selectedPackage = null
    }

    when {
        search.isNotBlank() -> {
            // البحث - عرض القنوات مباشرة
            SearchResults(repo, viewModel, search, navController)
        }
        selectedPackage == null -> {
            // عرض الباقات
            PackagesList(repo) { pkg ->
                selectedPackage = pkg
            }
        }
        else -> {
            // قنوات الباقة المختارة
            key(selectedPackage) { // ✅ key لتجنب اختلاط النصوص
                PackageChannels(
                    repo = repo,
                    viewModel = viewModel,
                    packageName = selectedPackage!!,
                    navController = navController,
                    onBack = { selectedPackage = null }
                )
            }
        }
    }
}

// ============== عرض نتائج البحث ==============
@Composable
private fun SearchResults(
    repo: ChannelRepository,
    viewModel: MainViewModel,
    search: String,
    navController: NavHostController
) {
    val pagingItems = remember(search) {
        repo.searchChannelsPaged(search)
    }.collectAsLazyPagingItems()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(
            count = pagingItems.itemCount,
            key = { index ->
                // ✅ مفتاح فريد لكل عنصر
                val entity = pagingItems.peek(index)
                "search_${entity?.id ?: "item_$index"}"
            }
        ) { index ->
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
