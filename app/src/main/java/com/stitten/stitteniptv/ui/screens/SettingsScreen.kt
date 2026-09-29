package com.stitten.stitteniptv.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.stitten.stitteniptv.data.PrefsManager
import com.stitten.stitteniptv.ui.navigation.Routes

@Composable
fun SettingsScreen(navController: NavHostController) {
    val ctx = LocalContext.current
    val prefs = remember { PrefsManager(ctx) }

    var is24h by remember { mutableStateOf(prefs.is24Hour) }
    var useInternal by remember { mutableStateOf(prefs.useInternalPlayer) }
    var useHw by remember { mutableStateOf(prefs.useHardwareDecoder) }

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
        SettingRow("فك التشفير العتادي") {
            Switch(checked = useHw, onCheckedChange = {
                useHw = it
                prefs.useHardwareDecoder = it
            })
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

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
