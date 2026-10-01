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
    onSwitchSource: () -> Unit,
    onRefresh: () -> Unit,
    onSourcesClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(300.dp)
            .fillMaxHeight()
            .background(Color(0xFF161B22).copy(alpha = 0.9f))
            .padding(16.dp)
    ) {
        Text(
            "STTITEN IP TV",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(currentTime, color = Color.White, fontSize = 15.sp)

        if (sourceName.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .tvFocusable(onSwitchSource),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1F6FEB).copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.SwapHoriz,
                        contentDescription = "تبديل",
                        tint = Color(0xFF58A6FF),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            sourceName,
                            color = Color.White,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "اضغط للتبديل",
                            color = Color.LightGray,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        SidebarButton("📺 القنوات", !showFavorites, onShowChannels)
        SidebarButton("❤️ المفضلة", showFavorites, onShowFavorites)

        Spacer(Modifier.height(12.dp))

        SidebarButton("🔄 تحديث القنوات", false, onRefresh)

        Spacer(Modifier.height(16.dp))

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

// ============== عرض القنوات ==============
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
        search.isNotBlank() -> SearchResults(repo, viewModel, search, navController)
        selectedPackage == null -> PackagesList(repo) { pkg ->
            selectedPackage = pkg
        }
        else -> {
            key(selectedPackage) {
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

// ============== البحث ==============
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

// ============== قائمة الباقات ==============
@Composable
private fun PackagesList(
    repo: ChannelRepository,
    onPackageClick: (String) -> Unit
) {
    val packages by repo.getChannelGroups().collectAsState(initial = emptyList())

    if (packages.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Text("جاري تحميل الباقات...", color = Color.White, fontSize = 18.sp)
            }
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item(key = "packages_header") {
            Text(
                "📦 الباقات (${packages.size})",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
        items(
            items = packages,
            key = { pkg -> "package_$pkg" }
        ) { pkg ->
            PackageCard(pkg, onPackageClick)
        }
    }
}

@Composable
private fun PackageCard(
    name: String,
    onClick: (String) -> Unit
) {
    val priority = CategoriesManager.getPriorityOrder(name)
    val (badge, badgeColor) = when (priority) {
        1 -> "⭐" to Color(0xFF1F6FEB)
        2 -> "🌍" to Color(0xFF238636)
        3 -> "🏅" to Color(0xFF8B5CF6)
        else -> "📺" to Color(0xFF21262D)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .tvFocusable { onClick(name) },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(badgeColor, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(badge, fontSize = 26.sp)
            }
            Spacer(Modifier.width(16.dp))
            Text(
                name,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ============== قنوات باقة ==============
@Composable
private fun PackageChannels(
    repo: ChannelRepository,
    viewModel: MainViewModel,
    packageName: String,
    navController: NavHostController,
    onBack: () -> Unit
) {
    val pagingItems = remember(packageName) {
        repo.getChannelsByGroupPaged(packageName)
    }.collectAsLazyPagingItems()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item(key = "back_button_$packageName") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .tvFocusable(onBack),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1F6FEB).copy(alpha = 0.25f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = null,
                        tint = Color(0xFF58A6FF),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("← رجوع إلى الباقات", color = Color(0xFF58A6FF), fontSize = 13.sp)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            packageName,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        items(
            count = pagingItems.itemCount,
            key = { index ->
                val entity = pagingItems.peek(index)
                "pkg_${packageName}_${entity?.id ?: "item_$index"}"
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

// ============== المفضلة ==============
@Composable
private fun FavoritesContent(
    viewModel: MainViewModel,
    search: String,
    navController: NavHostController
) {
    val favoritesVersion by viewModel.favoritesVersion.collectAsState()
    val favChannels = remember(favoritesVersion, search) {
        viewModel.getFavoriteChannels().filter {
            search.isBlank() || it.name.contains(search, true)
        }
    }

    if (favChannels.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("⭐", fontSize = 72.sp, color = Color.Gray)
                Spacer(Modifier.height(16.dp))
                Text("لا توجد قنوات مفضلة", color = Color.White, fontSize = 22.sp)
                Spacer(Modifier.height(8.dp))
                Text("اضغط على ⭐ بجانب أي قناة لإضافتها", color = Color.Gray, fontSize = 16.sp)
            }
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(
            items = favChannels,
            key = { ch -> "fav_${ch.id}" }
        ) { ch ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .tvFocusable {
                        navController.navigate(Routes.player(ch.url, ch.name))
                    },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(ch.logo).crossfade(true).build(),
                        placeholder = painterResource(R.drawable.ic_placeholder),
                        error = painterResource(R.drawable.ic_placeholder),
                        fallback = painterResource(R.drawable.ic_placeholder),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            ch.name, color = Color.White, fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        if (ch.group.isNotEmpty()) {
                            Text(ch.group, color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                    IconButton(
                        onClick = { viewModel.toggleChannelFavorite(ch.id, ch.name) }
                    ) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color(0xFFDA3633),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

// ============== Channel Row ==============
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
                model = ImageRequest.Builder(LocalContext.current)
                    .data(entity.logo).crossfade(true).build(),
                placeholder = painterResource(R.drawable.ic_placeholder),
                error = painterResource(R.drawable.ic_placeholder),
                fallback = painterResource(R.drawable.ic_placeholder),
                contentDescription = null,
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entity.name, color = Color.White, fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                if (entity.groupName.isNotEmpty()) {
                    Text(
                        entity.groupName, color = Color.Gray, fontSize = 14.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
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

// ============== دائرة النسبة ==============
@Composable
private fun CircularProgressOverlay(progress: Int, message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(40.dp)
                    .width(400.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(200.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.size(200.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 14.dp,
                        trackColor = Color(0xFF21262D)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$progress%",
                            color = Color.White,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("مكتمل", color = Color.LightGray, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(28.dp))
                Text(
                    message.ifBlank { "جاري التحميل..." },
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(20.dp))
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color(0xFF21262D)
                )
            }
        }
    }
}

// ============== مكونات مساعدة ==============
@Composable
private fun RowScope.CustomIconButton(
    iconRes: Int,
    label: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) Color(0xFF58A6FF) else Color(0xFF30363D),
                shape = RoundedCornerShape(12.dp)
            )
            .background(
                color = if (isFocused) Color(0xFF1F6FEB).copy(alpha = 0.3f)
                else Color(0xFF161B22)
            )
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter)
                ) {
                    onClick()
                    true
                } else false
            }
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            tint = if (isFocused) Color(0xFF58A6FF) else Color.White,
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            color = if (isFocused) Color(0xFF58A6FF) else Color.White,
            fontSize = 12.sp,
            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Normal
        )
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
    ) { Text(text, fontSize = 15.sp) }
}
