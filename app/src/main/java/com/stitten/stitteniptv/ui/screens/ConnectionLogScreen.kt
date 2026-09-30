package com.stitten.stitteniptv.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.stitten.stitteniptv.data.XtreamLogger
import kotlinx.coroutines.launch

@Composable
fun ConnectionLogScreen(navController: NavHostController) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var logContent by remember { mutableStateOf("") }
    var logPath by remember { mutableStateOf("") }
    var logSize by remember { mutableStateOf(0L) }
    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }

    fun reload() {
        scope.launch {
            loading = true
            logContent = XtreamLogger.readLogContent()
            logPath = XtreamLogger.getLogFilePath()
            logSize = XtreamLogger.getLogFileSize()
            loading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let {
            scope.launch {
                val ok = XtreamLogger.exportTo(ctx, it)
                message = if (ok) "✅ تم حفظ السجل بنجاح" else "❌ فشل الحفظ"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .padding(20.dp)
    ) {
        Text(
            "📋 سجل الاتصال",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    "📁 المسار: ${logPath.takeLast(50)}",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
                Text(
                    "📊 الحجم: ${"%.2f".format(logSize / 1024.0)} KB",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { XtreamLogger.shareLog(ctx) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) { Text("📤 مشاركة", fontSize = 14.sp) }

            Button(
                onClick = {
                    exportLauncher.launch("STTITEN_log.txt")
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF238636)
                )
            ) { Text("💾 حفظ USB", fontSize = 14.sp) }

            Button(
                onClick = { reload() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF21262D)
                )
            ) { Text("🔄 تحديث", fontSize = 14.sp) }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                scope.launch {
                    XtreamLogger.clearLog()
                    reload()
                    message = "✅ تم مسح السجل"
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFDA3633)
            )
        ) { Text("🗑️ مسح السجل", fontSize = 14.sp) }

        if (message.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(message, color = Color(0xFF58A6FF), fontSize = 12.sp)
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = Color(0xFF30363D))
        Spacer(Modifier.height(12.dp))
        
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (logContent.isBlank()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📭", fontSize = 72.sp, color = Color.Gray)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "لا يوجد سجل بعد",
                        color = Color.White,
                        fontSize = 20.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "فعّل التسجيل وجرّب تسجيل الدخول",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0E14)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp)
                ) {
                    Text(
                        text = logContent,
                        color = Color(0xFF8B949E),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}
