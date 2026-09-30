package com.stitten.stitteniptv.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.stitten.stitteniptv.data.DeviceCapabilityDetector
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
    val playerSettings = entryPoint.playerSettingsManager()
    val favoritesSync = entryPoint.favoritesSyncManager()

    var is24h by remember { mutableStateOf(prefs.is24Hour) }
    var useInternal by remember { mutableStateOf(prefs.useInternalPlayer) }
    var useHw by remember { mutableStateOf(prefs.useHardwareDecoder) }
    var favSyncEnabled by remember { mutableStateOf(playerSettings.favoritesSyncEnabled) }
    var useHttpsState by remember { mutableStateOf(prefs.useHttps) }
    var streamFormatState by remember { mutableStateOf(prefs.streamFormat) }
    var liteModeState by remember { mutableStateOf(prefs.liteModeEnabled) }
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
                    .background(Color.Black.copy(alpha = 0.75f))
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

            SettingRow("صيغة 24 ساعة") {
                Switch(checked = is24h, onCheckedChange = {
                    is24h = it
                    prefs.is24Hour = it
                })
            }

            SettingRow("استخدام المشغل الداخلي") {
                Switch(checked = useInternal, onCheckedChange = {
                    useInternal = it
                    prefs.useInternalPlayer = it
                })
            }
            Text(
                if (useInternal)
                    "✅ المشغل الداخلي مفعّل"
                else
                    "🎬 المشغل الخارجي مفعّل",
                color = Color.LightGray,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            SettingRow("فك التشفير العتادي") {
                Switch(checked = useHw, onCheckedChange = {
                    useHw = it
                    prefs.useHardwareDecoder = it
                })
            }

            SettingRow("مزامنة المفضلة بين المصادر") {
                Switch(checked = favSyncEnabled, onCheckedChange = {
                    favSyncEnabled = it
                    playerSettings.favoritesSyncEnabled = it
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
            ) { Text("🔄 مزامنة المفضلة الآن", fontSize = 18.sp) }

            if (syncMessage.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(syncMessage, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            
SettingRow("استخدام HTTPS/TLS") {
    Switch(checked = useHttpsState, onCheckedChange = {
        useHttpsState = it
        prefs.useHttps = it
        com.stitten.stitteniptv.data.XtreamApi.setPreferredProtocol(it)
    })
}
Text(
    if (useHttpsState) "🔒 HTTPS — اتصال آمن" else "🌐 HTTP — توافق أعلى",
    color = Color.LightGray,
    fontSize = 13.sp,
    modifier = Modifier.padding(bottom = 8.dp)
)

Text(
    "🎬 صيغة البث",
    color = Color.White,
    fontSize = 20.sp,
    fontWeight = FontWeight.Bold
)
Spacer(Modifier.height(8.dp))

Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(12.dp)
) {
    OutlinedButton(
        onClick = {
            streamFormatState = "ts"
            prefs.streamFormat = "ts"
            com.stitten.stitteniptv.data.XtreamApi.setPreferredFormat("ts")
        },
        modifier = Modifier.weight(1f),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (streamFormatState == "ts")
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent
        )
    ) { Text("TS (تأخير أقل)") }

    OutlinedButton(
        onClick = {
            streamFormatState = "hls"
            prefs.streamFormat = "hls"
            com.stitten.stitteniptv.data.XtreamApi.setPreferredFormat("hls")
        },
        modifier = Modifier.weight(1f),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (streamFormatState == "hls")
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent
        )
    ) { Text("HLS (تكيّف)") }
}

Spacer(Modifier.height(16.dp))
HorizontalDivider()
Spacer(Modifier.height(16.dp))

// ============== Lite Mode ==============
SettingRow("⚡ وضع Lite") {
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
        "✅ يقلل استهلاك الذاكرة (مناسب للأجهزة الضعيفة)"
    else
        "ℹ️ الوضع العادي (جودة كاملة)",
    color = Color.LightGray,
    fontSize = 13.sp
)

Spacer(Modifier.height(16.dp))
HorizontalDivider()
Spacer(Modifier.height(16.dp))

// ============== معلومات الجهاز ==============
Text(
    "📊 معلومات الجهاز",
    color = Color.White,
    fontSize = 20.sp,
    fontWeight = FontWeight.Bold
)
Spacer(Modifier.height(8.dp))

Card(
    colors = CardDefaults.cardColors(
        containerColor = Color(0xFF161B22).copy(alpha = 0.9f)
    ),
    modifier = Modifier.fillMaxWidth()
) {
    Column(Modifier.padding(16.dp)) {
        val capability = remember {
            DeviceCapabilityDetector.detect(ctx)
        }

        Text(
            "ذاكرة RAM: ${capability.totalRamMb} MB",
            color = Color.White,
            fontSize = 14.sp
        )
        Text(
            "متاح: ${capability.availableRamMb} MB",
            color = Color.LightGray,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "التخزين: ${capability.totalStorageMb / 1024} GB",
            color = Color.White,
            fontSize = 14.sp
        )
        Text(
            "متاح: ${capability.availableStorageMb / 1024} GB",
            color = Color.LightGray,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "الأنوية: ${capability.cpuCores}",
            color = Color.White,
            fontSize = 14.sp
        )
        Text(
            "Android: ${capability.androidVersion}",
            color = Color.White,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (capability.isLowEnd)
                "⚠️ جهاز محدود الموارد"
            else
                "✅ جهاز قوي",
            color = if (capability.isLowEnd) Color(0xFFFFA726) else Color(0xFF66BB6A),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

Spacer(Modifier.height(16.dp))
HorizontalDivider()
Spacer(Modifier.height(16.dp))

            Button(
                onClick = { navController.navigate(Routes.EXTERNAL_PLAYERS) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("🎬 اختيار المشغل الخارجي", fontSize = 18.sp) }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { navController.navigate(Routes.SOURCES) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("📡 إدارة المصادر", fontSize = 18.sp) }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { navController.navigate(Routes.PARENTAL) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("🔒 الرقابة الأبوية", fontSize = 18.sp) }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { navController.navigate(Routes.ERRORS) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("📋 سجل الأخطاء", fontSize = 18.sp) }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            Text(
                "حول التطبيق",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(12.dp))
            Text("اسم التطبيق: STTITEN IP TV", color = Color.White, fontSize = 18.sp)
            Text("المطور: جلولي مصطفى", color = Color.White, fontSize = 18.sp)
            Text("الإصدار: v1.0.0", color = Color.White, fontSize = 18.sp)

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
                )
            ) { Text("تسجيل الخروج", fontSize = 18.sp) }
        }
    }
}

@Composable
private fun SettingRow(label: String, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White, fontSize = 20.sp)
        content()
    }
}
