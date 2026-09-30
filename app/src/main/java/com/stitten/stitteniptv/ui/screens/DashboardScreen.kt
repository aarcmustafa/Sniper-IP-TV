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
