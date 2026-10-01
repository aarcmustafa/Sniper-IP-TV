package com.stitten.stitteniptv.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.stitten.stitteniptv.R
import com.stitten.stitteniptv.data.PrefsManager
import com.stitten.stitteniptv.ui.navigation.Routes
import com.stitten.stitteniptv.viewmodel.MainViewModel
import dagger.hilt.android.EntryPointAccessors

@Composable
fun SettingsScreen(
    navController: NavHostController,
    viewModel: MainViewModel = viewModel()
) {
    val ctx = LocalContext.current
    val prefs = remember { PrefsManager(ctx) }

    val entryPoint = remember {
        EntryPointAccessors.fromApplication(
            ctx.applicationContext,
            PlayerEntryPoint::class.java
        )
    }
    val favoritesSync = entryPoint.favoritesSyncManager()

    var is24h by remember { mutableStateOf(prefs.is24Hour) }
    var liteModeState by remember { mutableStateOf(prefs.liteModeEnabled) }
    var favSyncEnabled by remember { mutableStateOf(prefs.favoritesSyncEnabled) }
    var syncMessage by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {

        if (!liteModeState) {
            Image(
                painter = painterResource(id = R.drawable.bg_main),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.80f))
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0D1117))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "⚙️ الإعدادات",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(24.dp))

            // ============ الإعدادات الأساسية ============
            Text(
                "📌 الإعدادات الأساسية",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            FocusableSettingRow("صيغة 24 ساعة") {
                Switch(checked = is24h, onCheckedChange = {
                    is24h = it
                    prefs.is24Hour = it
                })
            }
            Text(
                if (is24h) "🕐 14:30:25"
                else "🕐 02:30:25 PM",
                color = Color.LightGray,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
            )

            FocusableSettingRow("⚡ وضع Lite") {
                Switch(
                    checked = liteModeState,
                    onCheckedChange = {
                        liteModeState = it
                        prefs.liteModeEnabled = it
                        viewModel.channelRepo.liteMode = it
                        prefs.userDisabledLiteMode = !it
                    }
                )
            }
            Text(
                if (liteModeState)
                    "✅ يقلل استهلاك الذاكرة للأجهزة الضعيفة"
                else
                    "ℹ️ الوضع العادي (جودة كاملة)",
                color = Color.LightGray,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
            )

            FocusableSettingRow("مزامنة المفضلة") {
                Switch(checked = favSyncEnabled, onCheckedChange = {
                    favSyncEnabled = it
                    prefs.favoritesSyncEnabled = it
                })
            }
            Text(
                "🔗 مزامنة المفضلة بين المصادر المتعددة",
                color = Color.LightGray,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    val count = favoritesSync.syncChannels(
                        com.stitten.stitteniptv.data.ContentRepository.channels,
                        com.stitten.stitteniptv.data.FavoritesManager(ctx)
                    )
                    syncMessage = "✅ تمت مزامنة $count قناة"
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) { Text("🔄 مزامنة المفضلة الآن", fontSize = 16.sp) }

            if (syncMessage.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    syncMessage,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = Color(0xFF30363D))
            Spacer(Modifier.height(20.dp))
            
