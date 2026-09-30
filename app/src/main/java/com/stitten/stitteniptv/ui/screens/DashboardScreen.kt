package com.stitten.stitteniptv.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
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
import coil.request.ImageRequest
import com.stitten.stitteniptv.R
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

        // ============== الشريط الجانبي ==============
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
            Spacer(Modifier.height(20.dp))

            SidebarButton("📺 القنوات", !showFavorites) {
                showFavorites = false
            }
            SidebarButton("❤️ المفضلة", showFavorites) {
                showFavorites = true
            }

            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("بحث", fontSize = 14.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.weight(1f))

            // ============== الأيقونات المخصصة ==============
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CustomIconButton(
                    iconRes = R.drawable.ic_sources_custom,
                    label = "المصادر"
                ) {
                    navController.navigate(Routes.SOURCES)
                }

                CustomIconButton(
                    iconRes = R.drawable.ic_settings_custom,
                    label = "الإعدادات"
                ) {
                    navController.navigate(Routes.SETTINGS)
                }
            }
        }
        
            // ============== منطقة العرض ==============
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {

                if (showFavorites) {
                    FavoritesContent(viewModel, search, navController)
                } else {
                    ChannelsContent(channelRepo, viewModel, search, navController)
                }

                if (uiState.isLoading) {
                    LoadingOverlay(uiState.loadingProgress, uiState.loadingMessage)
                }
            }
        }
    }
}

// ============== عرض القنوات (مرتبة) ==============
@Composable
private fun ChannelsContent(
    repo: ChannelRepository,
    viewModel: MainViewModel,
    search: String,
    navController: NavHostController
) {
    val pagingItems = remember(search) {
        if (search.isNotBlank()) repo.searchChannelsPaged(search)
        else repo.getAllChannelsPaged()
    }.collectAsLazyPagingItems()

    var lastGroup by remember { mutableStateOf("") }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(pagingItems.itemCount) { index ->
            val entity = pagingItems[index]
            if (entity != null) {
                if (entity.groupName != lastGroup && entity.groupName.isNotEmpty()) {
                    lastGroup = entity.groupName
                    PackageHeader(entity.groupName)
                }
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
private fun PackageHeader(name: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(24.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                name,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ✅ الحل: RowScope.CustomIconButton لتفعيل weight
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
            .clickable(onClick = onClick)
            .onFocusChanged { isFocused = it.isFocused }
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
                Text(
                    "اضغط على ⭐ بجانب أي قناة لإضافتها",
                    color = Color.Gray,
                    fontSize = 16.sp
                )
            }
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(favChannels, key = { it.id }) { ch ->
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
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
                    CircularProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.size(180.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 12.dp,
                        trackColor = Color(0xFF21262D)
                    )
                    Text(
                        "$progress%",
                        color = Color.White,
                        fontSize = 44.sp,
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
                    modifier = Modifier.fillMaxWidth().height(8.dp),
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
            containerColor = if (selected) MaterialTheme.colorScheme.primary
            else Color(0xFF21262D),
            contentColor = Color.White
        )
    ) { Text(text, fontSize = 16.sp) }
}
