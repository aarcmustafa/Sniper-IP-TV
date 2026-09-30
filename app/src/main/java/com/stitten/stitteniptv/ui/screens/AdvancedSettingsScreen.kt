package com.stitten.stitteniptv.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.navigation.NavHostController
import com.stitten.stitteniptv.R
import com.stitten.stitteniptv.data.PrefsManager

@Composable
fun AdvancedSettingsScreen(navController: NavHostController) {
    val ctx = LocalContext.current
    val prefs = remember { PrefsManager(ctx) }

    var useHw by remember { mutableStateOf(prefs.useHardwareDecoder) }
    var useHttpsState by remember { mutableStateOf(prefs.useHttps) }
    var streamFormatState by remember { mutableStateOf(prefs.streamFormat) }
    var cacheCleared by remember { mutableStateOf(false) }

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
                    .background(Color.Black.copy(alpha = 0.85f))
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
                "⚙️ الإعدادات المتقدمة",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "إعدادات للأجهزة والمستخدمين المتقدمين",
                color = Color.LightGray,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(24.dp))

            // ============== فك التشفير ==============
            Text(
                "🎬 فك التشفير",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))

            FocusableAdvancedRow("فك التشفير العتادي (HW)") {
                Switch(checked = useHw, onCheckedChange = {
                    useHw = it
                    prefs.useHardwareDecoder = it
                })
            }
            Text(
                if (useHw) "✅ HW مفعّل — أداء أفضل"
                else "ℹ️ SW مفعّل — توافق أعلى",
                color = Color.LightGray,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 16.dp)
            )

            HorizontalDivider(color = Color(0xFF30363D))
            Spacer(Modifier.height(16.dp))

            // ============== بروتوكول الاتصال ==============
            Text(
                "🌐 بروتوكول الاتصال",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))

            FocusableAdvancedRow("استخدام HTTPS/TLS") {
                Switch(checked = useHttpsState, onCheckedChange = {
                    useHttpsState = it
                    prefs.useHttps = it
                    com.stitten.stitteniptv.data.XtreamApi.setPreferredProtocol(it)
                })
            }
            Text(
                if (useHttpsState) "🔒 HTTPS — اتصال مشفّر"
                else "🌐 HTTP — أسرع",
                color = Color.LightGray,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 16.dp)
            )

            HorizontalDivider(color = Color(0xFF30363D))
            Spacer(Modifier.height(16.dp))

            // ============== صيغة البث ==============
            Text(
                "🎬 صيغة البث",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "اختر صيغة تشغيل القنوات المباشرة",
                color = Color.LightGray,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // TS Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            width = if (streamFormatState == "ts") 3.dp else 1.dp,
                            color = if (streamFormatState == "ts")
                                Color(0xFF58A6FF) else Color(0xFF30363D),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            streamFormatState = "ts"
                            prefs.streamFormat = "ts"
                            com.stitten.stitteniptv.data.XtreamApi.setPreferredFormat("ts")
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (streamFormatState == "ts")
                            Color(0xFF1F6FEB).copy(alpha = 0.4f)
                        else Color(0xFF161B22)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("TS", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("تأخير أقل", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                        Text("MPEG-TS", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                    }
                }

                // HLS Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            width = if (streamFormatState == "hls") 3.dp else 1.dp,
                            color = if (streamFormatState == "hls")
                                Color(0xFF58A6FF) else Color(0xFF30363D),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            streamFormatState = "hls"
                            prefs.streamFormat = "hls"
                            com.stitten.stitteniptv.data.XtreamApi.setPreferredFormat("hls")
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (streamFormatState == "hls")
                            Color(0xFF1F6FEB).copy(alpha = 0.4f)
                        else Color(0xFF161B22)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("HLS", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("تكيّف أفضل", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                        Text("M3U8", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                    }
                }
            }
            
            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = Color(0xFF30363D))
            Spacer(Modifier.height(16.dp))

            // ============== إعدادات المشغل الحالية ==============
            Text(
                "🎛️ إعدادات المشغل الحالية",
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
                    Text(
                        "• Buffer: ${if (prefs.liteModeEnabled) "15 ثانية" else "30 ثانية"}",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "• Cache فيديو: ${if (prefs.liteModeEnabled) "30 MB" else "200 MB"}",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "• Cache صور: ${if (prefs.liteModeEnabled) "30 MB" else "250 MB"}",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = Color(0xFF30363D))
            Spacer(Modifier.height(16.dp))

            // ============== تنظيف البيانات ==============
            Text(
                "🧹 تنظيف البيانات",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    com.stitten.stitteniptv.player.CacheManager.clear()
                    cacheCleared = true
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("🗑️ مسح Cache الفيديو", fontSize = 16.sp) }

            if (cacheCleared) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "✅ تم مسح الـ Cache",
                    color = Color(0xFF66BB6A),
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "عند المسح، سيُعاد تحميل الفيديوهات من السيرفر.",
                color = Color.LightGray,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun FocusableAdvancedRow(
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
