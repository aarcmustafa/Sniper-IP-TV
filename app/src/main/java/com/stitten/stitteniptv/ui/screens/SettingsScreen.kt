package com.stitten.stitteniptv.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
    var useInternal by remember { mutableStateOf(prefs.useInternalPlayer) }
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
                "الإعدادات",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(24.dp))

            FocusableSettingRow("صيغة 24 ساعة") {
                Switch(checked = is24h, onCheckedChange = {
                    is24h = it
                    prefs.is24Hour = it
                })
            }

            FocusableSettingRow("استخدام المشغل الداخلي") {
                Switch(checked = useInternal, onCheckedChange = {
                    useInternal = it
                    prefs.useInternalPlayer = it
                })
            }
            Text(
                if (useInternal) "✅ المشغل الداخلي مفعّل"
                else "🎬 المشغل الخارجي سيعمل تلقائياً",
                color = Color.LightGray,
                fontSize = 13.sp,
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
                if (liteModeState) "✅ يقلل استهلاك الذاكرة (للأجهزة الضعيفة)"
                else "ℹ️ الوضع العادي (جودة كاملة)",
                color = Color.LightGray,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
            )

            FocusableSettingRow("مزامنة المفضلة") {
                Switch(checked = favSyncEnabled, onCheckedChange = {
                    favSyncEnabled = it
                    prefs.favoritesSyncEnabled = it
                })
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    val count = favoritesSync.syncChannels(
                        com.stitten.stitteniptv.data.ContentRepository.channels,
                        com.stitten.stitteniptv.data.FavoritesManager(ctx)
                    )
                    syncMessage = "تمت مزامنة $count قناة"
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("🔄 مزامنة المفضلة الآن", fontSize = 16.sp) }

            if (syncMessage.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(syncMessage, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = Color(0xFF30363D))
            Spacer(Modifier.height(20.dp))

            FocusableNavigationRow(
                title = "⚙️ الإعدادات المتقدمة",
                subtitle = "فك التشفير، البروتوكول، صيغة البث، تنظيف Cache",
                onClick = { navController.navigate(Routes.ADVANCED_SETTINGS) }
            )

            Spacer(Modifier.height(12.dp))

            FocusableNavigationRow(
                title = "📡 إدارة المصادر",
                subtitle = "إضافة، تعديل، حذف (Xtream + M3U)",
                onClick = { navController.navigate(Routes.SOURCES) }
            )

            Spacer(Modifier.height(12.dp))

            FocusableNavigationRow(
                title = "🎬 اختيار المشغل الخارجي",
                subtitle = "VLC، MX Player، MPV، وغيرها",
                onClick = { navController.navigate(Routes.EXTERNAL_PLAYERS) }
            )

            Spacer(Modifier.height(12.dp))

            FocusableNavigationRow(
                title = "🔒 الرقابة الأبوية",
                subtitle = "PIN وإدارة الحجب",
                onClick = { navController.navigate(Routes.PARENTAL) }
            )

            Spacer(Modifier.height(12.dp))

            FocusableNavigationRow(
                title = "📋 سجل الأخطاء",
                subtitle = "عرض أخطاء التشغيل",
                onClick = { navController.navigate(Routes.ERRORS) }
            )
            
            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = Color(0xFF30363D))
            Spacer(Modifier.height(20.dp))

            Text(
                "📊 معلومات الجهاز",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF161B22).copy(alpha = 0.9f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    val capability = remember {
                        com.stitten.stitteniptv.data.DeviceCapabilityDetector.detect(ctx)
                    }

                    InfoRow("ذاكرة RAM", "${capability.totalRamMb} MB")
                    InfoRow("RAM متاح", "${capability.availableRamMb} MB")
                    InfoRow("التخزين", "${capability.totalStorageMb / 1024} GB")
                    InfoRow("متاح", "${capability.availableStorageMb / 1024} GB")
                    InfoRow("الأنوية", "${capability.cpuCores}")
                    InfoRow("Android", "${capability.androidVersion}")

                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (capability.isLowEnd) "⚠️ جهاز محدود الموارد"
                        else "✅ جهاز قوي",
                        color = if (capability.isLowEnd) Color(0xFFFFA726)
                        else Color(0xFF66BB6A),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = Color(0xFF30363D))
            Spacer(Modifier.height(20.dp))

            Text(
                "حول التطبيق",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(8.dp))
            Text("اسم التطبيق: STTITEN IP TV", color = Color.White, fontSize = 16.sp)
            Text("المطور: جلولي مصطفى", color = Color.White, fontSize = 16.sp)
            Text("الإصدار: v1.0.0", color = Color.White, fontSize = 16.sp)

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    prefs.clear()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.fillMaxWidth()
            ) { Text("تسجيل الخروج", fontSize = 16.sp) }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ============== مكونات مع تركيز واضح ==============

@Composable
private fun FocusableSettingRow(
    label: String,
    content: @Composable () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(
                width = if (isFocused) 3.dp else 1.dp,
                color = if (isFocused) Color(0xFF58A6FF) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .background(
                color = if (isFocused) Color(0xFF21262D).copy(alpha = 0.8f)
                else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
            .onFocusChanged { isFocused = it.isFocused },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            color = if (isFocused) Color(0xFF58A6FF) else Color.White,
            fontSize = 18.sp,
            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Normal
        )
        content()
    }
}

@Composable
private fun FocusableNavigationRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isFocused) 3.dp else 0.dp,
                color = if (isFocused) Color(0xFF58A6FF) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .onFocusChanged { isFocused = it.isFocused },
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) Color(0xFF1F6FEB)
            else Color(0xFF161B22).copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )
            }
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.LightGray, fontSize = 13.sp)
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
