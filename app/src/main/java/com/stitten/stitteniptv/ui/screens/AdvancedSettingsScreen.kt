package com.stitten.stitteniptv.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.navigation.NavHostController
import com.stitten.stitteniptv.R
import com.stitten.stitteniptv.data.HttpClientProvider
import com.stitten.stitteniptv.data.PrefsManager
import com.stitten.stitteniptv.data.XtreamApi
import com.stitten.stitteniptv.ui.components.tvFocusable
import com.stitten.stitteniptv.ui.theme.AppIcons

@Composable
fun AdvancedSettingsScreen(navController: NavHostController) {
    val ctx = LocalContext.current
    val prefs = remember { PrefsManager(ctx) }

    var useHw by remember { mutableStateOf(prefs.useHardwareDecoder) }
    var useHttpsState by remember { mutableStateOf(prefs.useHttps) }
    var streamFormatState by remember { mutableStateOf(prefs.streamFormat) }

    // رسائل التأكيد
    var cacheClearedMsg by remember { mutableStateOf("") }
    var dnsClearedMsg by remember { mutableStateOf("") }
    var cookiesClearedMsg by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {

        if (!prefs.liteModeEnabled) {
            Image(
                painter = painterResource(id = R.drawable.bg_main),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.88f))
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

            // ═══════════════════════════════════════════════════════
            //  الشريط العلوي
            // ═══════════════════════════════════════════════════════
            AdvancedTopBar(
                onBack = { navController.popBackStack() },
            )

            Spacer(Modifier.height(24.dp))

            // ═══════════════════════════════════════════════════════
            //  🎬 المشغل والترميز
            // ═══════════════════════════════════════════════════════
            SectionHeader(
                icon = AppIcons.Movies,
                title = "المشغل والترميز",
            )

            FocusableAdvancedRow(
                label = "فك الترميز العتادي (HW)",
                onClick = {
                    useHw = !useHw
                    prefs.useHardwareDecoder = useHw
                }
            ) {
                Switch(checked = useHw, onCheckedChange = {
                    useHw = it
                    prefs.useHardwareDecoder = it
                })
            }
            HintText(
                if (useHw) "✅ HW مفعّل — أداء أفضل"
                else "ℹ️ SW مفعّل — توافق أعلى"
            )

            Spacer(Modifier.height(28.dp))

            // ═══════════════════════════════════════════════════════
            //  🌐 البروتوكول والشبكة
            // ═══════════════════════════════════════════════════════
            SectionHeader(
                icon = AppIcons.Wifi,
                title = "البروتوكول والشبكة",
            )

            FocusableAdvancedRow(
                label = "استخدام HTTPS/TLS",
                onClick = {
                    useHttpsState = !useHttpsState
                    prefs.useHttps = useHttpsState
                    XtreamApi.setPreferredProtocol(useHttpsState)
                }
            ) {
                Switch(checked = useHttpsState, onCheckedChange = {
                    useHttpsState = it
                    prefs.useHttps = it
                    XtreamApi.setPreferredProtocol(it)
                })
            }
            HintText(
                if (useHttpsState) "🔒 HTTPS — اتصال مشفّر"
                else "🌐 HTTP — أسرع"
            )

            Spacer(Modifier.height(20.dp))

            // ─── صيغة البث ───
            Text(
                text = "صيغة البث للقنوات المباشرة",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StreamFormatCard(
                    label = "TS",
                    subLabel = "تأخير أقل",
                    techLabel = "MPEG-TS",
                    isSelected = streamFormatState == "ts",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        streamFormatState = "ts"
                        prefs.streamFormat = "ts"
                        XtreamApi.setPreferredFormat("ts")
                    },
                )

                StreamFormatCard(
                    label = "HLS",
                    subLabel = "تكيّف أفضل",
                    techLabel = "M3U8",
                    isSelected = streamFormatState == "hls",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        streamFormatState = "hls"
                        prefs.streamFormat = "hls"
                        XtreamApi.setPreferredFormat("hls")
                    },
                )
            }

            Spacer(Modifier.height(28.dp))

            // ═══════════════════════════════════════════════════════
            //  🎛️ الإعدادات الحالية
            // ═══════════════════════════════════════════════════════
            SectionHeader(
                icon = AppIcons.Storage,
                title = "الإعدادات الحالية",
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF161B22).copy(alpha = 0.9f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    InfoRow(
                        label = "Buffer",
                        value = if (prefs.liteModeEnabled) "15 ثانية" else "30 ثانية"
                    )
                    InfoRow(
                        label = "Cache فيديو",
                        value = if (prefs.liteModeEnabled) "30 MB" else "200 MB"
                    )
                    InfoRow(
                        label = "Cache صور",
                        value = if (prefs.liteModeEnabled) "30 MB" else "250 MB"
                    )
                    InfoRow(
                        label = "وضع Lite",
                        value = if (prefs.liteModeEnabled) "مفعّل" else "معطّل"
                    )
                }
            }
            
            Spacer(Modifier.height(28.dp))

            // ═══════════════════════════════════════════════════════
            //  🧹 الصيانة والتنظيف
            // ═══════════════════════════════════════════════════════
            SectionHeader(
                icon = Icons.Default.Delete,
                title = "الصيانة والتنظيف",
            )

            // ─── مسح Cache الفيديو ───
            CleanupButton(
                label = "مسح Cache الفيديو",
                hint = "سيُعاد تحميل الفيديوهات من السيرفر",
                onClick = {
                    try {
                        com.stitten.stitteniptv.player.CacheManager.clear()
                        cacheClearedMsg = "✅ تم مسح Cache الفيديو"
                    } catch (e: Exception) {
                        cacheClearedMsg = "⚠️ فشل المسح: ${e.message}"
                    }
                },
            )
            if (cacheClearedMsg.isNotEmpty()) {
                SuccessMessage(cacheClearedMsg)
                Spacer(Modifier.height(8.dp))
            }

            // ─── مسح DNS ───
            CleanupButton(
                label = "مسح ذاكرة DNS",
                hint = "يُستخدم عند تعذّر الاتصال ببعض السيرفرات",
                onClick = {
                    try {
                        HttpClientProvider.clearDnsCache()
                        dnsClearedMsg = "✅ تم مسح ذاكرة DNS"
                    } catch (e: Exception) {
                        dnsClearedMsg = "⚠️ فشل المسح: ${e.message}"
                    }
                },
            )
            if (dnsClearedMsg.isNotEmpty()) {
                SuccessMessage(dnsClearedMsg)
                Spacer(Modifier.height(8.dp))
            }

            // ─── مسح الكوكيز ───
            CleanupButton(
                label = "مسح الكوكيز (Cookies)",
                hint = "سيُعاد تسجيل الدخول للمصادر",
                onClick = {
                    try {
                        HttpClientProvider.clearCookies()
                        cookiesClearedMsg = "✅ تم مسح الكوكيز"
                    } catch (e: Exception) {
                        cookiesClearedMsg = "⚠️ فشل المسح: ${e.message}"
                    }
                },
            )
            if (cookiesClearedMsg.isNotEmpty()) {
                SuccessMessage(cookiesClearedMsg)
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  الشريط العلوي
// ═══════════════════════════════════════════════════════════════

@Composable
private fun AdvancedTopBar(onBack: () -> Unit) {
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

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = AppIcons.Settings,
                    contentDescription = null,
                    tint = Color(0xFF1F6FEB),
                    modifier = Modifier.size(30.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "الإعدادات المتقدمة",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "إعدادات للأجهزة والمستخدمين المتقدمين",
                color = Color(0xFF8B949E),
                fontSize = 13.sp,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  عنوان قسم
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
//  صف قابل للتركيز
// ═══════════════════════════════════════════════════════════════

@Composable
private fun FocusableAdvancedRow(
    label: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .tvFocusable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
        )
        content()
    }
}

// ═══════════════════════════════════════════════════════════════
//  نص توضيحي
// ═══════════════════════════════════════════════════════════════

@Composable
private fun HintText(text: String) {
    Text(
        text = text,
        color = Color(0xFF8B949E),
        fontSize = 12.sp,
        modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 8.dp),
    )
}

// ═══════════════════════════════════════════════════════════════
//  بطاقة صيغة البث
// ═══════════════════════════════════════════════════════════════

@Composable
private fun StreamFormatCard(
    label: String,
    subLabel: String,
    techLabel: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) Color(0xFF58A6FF) else Color(0xFF30363D),
                shape = RoundedCornerShape(12.dp)
            )
            .tvFocusable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                Color(0xFF1F6FEB).copy(alpha = 0.35f)
            else Color(0xFF161B22)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                text = subLabel,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.8f),
            )
            Text(
                text = techLabel,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.6f),
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  زر تنظيف
// ═══════════════════════════════════════════════════════════════

@Composable
private fun CleanupButton(
    label: String,
    hint: String,
    onClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF30363D)
            ),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = Color(0xFFFFA726),
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(label, fontSize = 16.sp, color = Color.White)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = hint,
            color = Color(0xFF8B949E),
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  رسالة نجاح
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SuccessMessage(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF66BB6A),
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            color = Color(0xFF66BB6A),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  صف معلومات
// ═══════════════════════════════════════════════════════════════

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            color = Color(0xFF8B949E),
            fontSize = 14.sp,
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
