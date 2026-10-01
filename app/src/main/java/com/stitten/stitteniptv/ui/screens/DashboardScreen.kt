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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
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
import com.stitten.stitteniptv.data.NetworkMonitor
import com.stitten.stitteniptv.data.PrefsManager
import com.stitten.stitteniptv.database.entity.ChannelEntity
import com.stitten.stitteniptv.database.entity.SourceEntity
import com.stitten.stitteniptv.ui.components.ContinueWatchingSection
import com.stitten.stitteniptv.ui.components.NoInternetBanner
import com.stitten.stitteniptv.ui.components.SlowInternetBanner
import com.stitten.stitteniptv.ui.components.SourcesSwitcherDialog
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
    val sourceMgr = entryPoint.sourceManager()
    val historyMgr = entryPoint.watchHistoryManager()

    val sources by sourceMgr.getAll().collectAsState(initial = emptyList())
    val recent by historyMgr.getRecent().collectAsState(initial = emptyList())

    // ✅ مراقبة الشبكة
    val networkState by NetworkMonitor.observe(ctx)
        .collectAsState(initial = NetworkMonitor.NetworkState(true, false, false, 0))

    // ✅ قائمة الحسابات
    var showSourcesDialog by remember { mutableStateOf(false) }
    var showFavorites by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var currentTime by remember { mutableStateOf("") }
    var refreshMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (uiState.channelsCount == 0) {
            viewModel.loadCachedContent()
        }
    }

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
                onSwitchSource = { showSourcesDialog = true },
                onRefresh = {
                    viewModel.refreshM3uSource()
                    refreshMessage = "🔄 جاري التحديث..."
                },
                onSourcesClick = { navController.navigate(Routes.SOURCES) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) }
            )

            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

                // ✅ إشعارات الشبكة
                if (!networkState.isConnected) {
                    NoInternetBanner()
                    Spacer(Modifier.height(12.dp))
                } else if (networkState.downKbps > 0 && networkState.downKbps < 2000) {
                    SlowInternetBanner(speedKbps = networkState.downKbps)
                    Spacer(Modifier.height(12.dp))
                }

                // ✅ رسالة التحديث
                if (refreshMessage.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1F6FEB).copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            refreshMessage,
                            color = Color.White,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // ✅ متابعة المشاهدة (فقط عند عرض القنوات)
                if (!showFavorites && search.isBlank() && recent.isNotEmpty()) {
                    key("continue_watching") {
                        ContinueWatchingSection(
                            items = recent.take(10),
                            onItemClick = { item ->
                                navController.navigate(
                                    Routes.player(item.url, item.title)
                                )
                            },
                            onRemoveItem = { item ->
                                viewModel.removeFromHistory(item.contentId)
                            }
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    key(showFavorites, search) {
                        if (showFavorites) {
                            FavoritesContent(viewModel, search, navController)
                        } else {
                            ChannelsContent(channelRepo, viewModel, search, navController)
                        }
                    }

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

    // ✅ Dialog تبديل المصادر
    if (showSourcesDialog) {
        SourcesSwitcherDialog(
            sources = sources,
            currentSourceId = sources.firstOrNull { it.isActive }?.id ?: -1L,
            onSelect = { source ->
                showSourcesDialog = false
                viewModel.switchSource(source)
            },
            onDismiss = { showSourcesDialog = false }
        )
    }
}
