package com.stitten.stitteniptv.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.stitten.stitteniptv.ui.settings.UserAgentSection
import com.stitten.stitteniptv.ui.theme.AppIcons
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

        // ─── خلفية ───
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

            // ═══════════════════════════════════════════════════
            //  الشريط العلوي (رجوع + عنوان)
            // ═══════════════════════════════════════════════════
            SettingsTopBar(
                onBack = { navController.popBackStack() },
            )

            Spacer(Modifier.height(24.dp))

            // ═══════════════════════════════════════════════════
            //  📺 العرض والواجهة
            // ═══════════════════════════════════════════════════
            SectionHeader(
                icon = AppIcons.Movies,
                title = "العرض والواجهة",
            )

            FocusableSettingRow(
                label = "صيغة 24 ساعة",
                onClick = {
                    is24h = !is24h
                    prefs.is24Hour = is24h
                }
            ) {
                Switch(checked = is24h, onCheckedChange = {
                    is24h = it
                    prefs.is24Hour = it
                })
            }
            HintText(
                if (is24h) "🕐 14:30:25" else "🕐 02:30:25 PM"
            )

            FocusableSettingRow(
                label = "وضع Lite (للأجهزة الضعيفة)",
                onClick = {
                    liteModeState = !liteModeState
                    prefs.liteModeEnabled = liteModeState
                    viewModel.channelRepo.liteMode = liteModeState
                    prefs.userDisabledLiteMode = !liteModeState
                }
            ) {
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
            HintText(
                if (liteModeState) "✅ يقلل استهلاك الذاكرة"
                else "ℹ️ الوضع العادي (جودة كاملة)"
            )

            Spacer(Modifier.height(28.dp))

            // ═══════════════════════════════════════════════════
            //  🌐 الشبكة والاتصال
            // ═══════════════════════════════════════════════════
            SectionHeader(
                icon = AppIcons.Wifi,
                title = "الشبكة والاتصال",
            )

            UserAgentSection(prefs = prefs)

            Spacer(Modifier.height(28.dp))

            // ═══════════════════════════════════════════════════
            //  💾 المحتوى والمفضلة
            // ═══════════════════════════════════════════════════
            SectionHeader(
                icon = AppIcons.Favorites,
                title = "المحتوى والمفضلة",
            )

            FocusableSettingRow(
                label = "مزامنة المفضلة",
                onClick = {
                    favSyncEnabled = !favSyncEnabled
                    prefs.favoritesSyncEnabled = favSyncEnabled
                }
            ) {
                Switch(checked = favSyncEnabled, onCheckedChange = {
                    favSyncEnabled = it
                    prefs.favoritesSyncEnabled = it
                })
            }
            HintText("🔗 مزامنة المفضلة بين المصادر")

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    val count = favoritesSync.syncChannels(
                        com.stitten.stitteniptv.data.ContentRepository.channels,
                        com.stitten.stitteniptv.data.FavoritesManager(ctx)
                    )
                    syncMessage = "✅ تمت مزامنة $count قناة"
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF238636)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("مزامنة المفضلة الآن", fontSize = 16.sp)
            }

            if (syncMessage.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    syncMessage,
                    color = Color(0xFF66BB6A),
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.height(28.dp))

            // ═══════════════════════════════════════════════════
            //  📡 المصادر
            // ═══════════════════════════════════════════════════
            SectionHeader(
                icon = AppIcons.Source,
                title = "المصادر",
            )

            FocusableNavigationRow(
                title = "إدارة المصادر",
                subtitle = "إضافة، تعديل، حذف (Xtream + M3U)",
                onClick = { navController.navigate(Routes.SOURCES) }
            )

            Spacer(Modifier.height(12.dp))

            FocusableNavigationRow(
                title = "اختيار المشغل الخارجي",
                subtitle = "VLC، MX Player، MPV",
                onClick = { navController.navigate(Routes.EXTERNAL_PLAYERS) }
            )

            Spacer(Modifier.height(28.dp))

            // ═══════════════════════════════════════════════════
            //  🔧 الأدوات
            // ═══════════════════════════════════════════════════
            SectionHeader(
                icon = AppIcons.Settings,
                title = "الأدوات",
            )

            FocusableNavigationRow(
                title = "الإعدادات المتقدمة",
                subtitle = "فك الترميز، صيغة البث، تنظيف Cache",
                onClick = { navController.navigate(Routes.ADVANCED_SETTINGS) }
            )

            Spacer(Modifier.height(12.dp))

            FocusableNavigationRow(
                title = "الرقابة الأبوية",
                subtitle = "PIN وإدارة الحجب",
                onClick = { navController.navigate(Routes.PARENTAL) }
            )

            Spacer(Modifier.height(12.dp))

            FocusableNavigationRow(
                title = "سجل الأخطاء",
                subtitle = "عرض أخطاء التشغيل",
                onClick = { navController.navigate(Routes.ERRORS) }
            )

            Spacer(Modifier.height(28.dp))

            // ═══════════════════════════════════════════════════
            //  💻 معلومات الجهاز
            // ═══════════════════════════════════════════════════
            SectionHeader(
                icon = AppIcons.Storage,
                title = "معلومات الجهاز",
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF161B22).copy(alpha = 0.9f)
                ),
                shape = RoundedCornerShape(12.dp),
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

                    Spacer(Modifier.height(10.dp))
                    Surface(
                        color = if (capability.isLowEnd)
                            Color(0xFFFFA726).copy(alpha = 0.2f)
                        else
                            Color(0xFF66BB6A).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            if (capability.isLowEnd)
                                "⚠️ جهاز محدود الموارد"
                            else
                                "✅ جهاز قوي",
                            color = if (capability.isLowEnd)
                                Color(0xFFFFA726)
                            else
                                Color(0xFF66BB6A),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 8.dp
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // ═══════════════════════════════════════════════════
            //  ℹ️ حول التطبيق
            // ═══════════════════════════════════════════════════
            SectionHeader(
                icon = AppIcons.Info,
                title = "حول التطبيق",
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF161B22).copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = AppIcons.Movies,
                            contentDescription = null,
                            tint = Color(0xFF1F6FEB),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "STTITEN IP TV",
                            color = Color(0xFF1F6FEB),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    InfoRow("المطور", "جلولي مصطفى")
                    InfoRow("الإصدار", "v1.0.0")
                    InfoRow("النوع", "IPTV لـ Android TV")
                }
            }

            Spacer(Modifier.height(32.dp))

            // ═══════════════════════════════════════════════════
            //  تسجيل الخروج
            // ═══════════════════════════════════════════════════
            Button(
                onClick = {
                    prefs.clear()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDA3633)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(60.dp)
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text("تسجيل الخروج", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  الشريط العلوي
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "رجوع",
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Icon(
            imageVector = AppIcons.Settings,
            contentDescription = null,
            tint = Color(0xFF1F6FEB),
            modifier = Modifier.size(32.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            "الإعدادات",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  عنوان قسم (مع أيقونة + خط فاصل)
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SectionHeader(
    icon: ImageVector,
    title: String,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF1F6FEB),
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = Color(0xFF30363D))
        Spacer(Modifier.height(12.dp))
    }
}

// ═══════════════════════════════════════════════════════════════
//  نص توضيحي أسفل الإعداد
// ═══════════════════════════════════════════════════════════════

@Composable
private fun HintText(text: String) {
    Text(
        text = text,
        color = Color(0xFF8B949E),
        fontSize = 12.sp,
        modifier = Modifier.padding(start = 8.dp, bottom = 12.dp, top = 4.dp),
    )
}
