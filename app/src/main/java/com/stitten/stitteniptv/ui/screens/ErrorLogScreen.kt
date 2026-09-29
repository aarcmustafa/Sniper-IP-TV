package com.stitten.stitteniptv.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dagger.hilt.android.EntryPointAccessors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ErrorLogScreen() {
    val ctx = LocalContext.current
    val entryPoint = remember {
        EntryPointAccessors.fromApplication(
            ctx.applicationContext,
            PlayerEntryPoint::class.java
        )
    }
    val logs by entryPoint.errorLogger().getAll().collectAsState(initial = emptyList())

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text(
            "📋 سجل الأخطاء",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))

        if (logs.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا توجد أخطاء مسجلة", color = Color.Gray, fontSize = 18.sp)
            }
        } else {
            val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            LazyColumn {
                items(logs) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF161B22)
                        )
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                fmt.format(Date(log.timestamp)),
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                            Text(
                                "URL: ${log.url}",
                                color = Color.White,
                                fontSize = 14.sp,
                                maxLines = 1
                            )
                            Text(
                                "Error: ${log.errorMessage}",
                                color = Color(0xFFDA3633),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
