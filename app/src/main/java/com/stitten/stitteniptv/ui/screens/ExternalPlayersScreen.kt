package com.stitten.stitteniptv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import com.stitten.stitteniptv.data.ExternalPlayerManager
import com.stitten.stitteniptv.ui.components.tvFocusable
import dagger.hilt.android.EntryPointAccessors

@Composable
fun ExternalPlayersScreen(navController: NavHostController) {
    val ctx = LocalContext.current
    val entryPoint = remember {
        EntryPointAccessors.fromApplication(
            ctx.applicationContext,
            PlayerEntryPoint::class.java
        )
    }
    val settings = entryPoint.playerSettingsManager()

    val installed = remember { ExternalPlayerManager.getInstalledPlayers(ctx) }
    var selectedPackage by remember { mutableStateOf(settings.externalPlayerPackage) }
    var alwaysAsk by remember { mutableStateOf(settings.alwaysAskExternalPlayer) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Text(
            "🎬 المشغل الخارجي",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "اختر المشغل الذي سيُفتح عند اختيار 'فتح بمشغل خارجي'",
            color = Color.LightGray,
            fontSize = 16.sp
        )
        Spacer(Modifier.height(16.dp))

        SettingRow("اسألني في كل مرة") {
            Switch(checked = alwaysAsk, onCheckedChange = {
                alwaysAsk = it
                settings.alwaysAskExternalPlayer = it
            })
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        if (installed.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚠️", fontSize = 64.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "لم يُعثر على مشغلات خارجية مثبتة",
                        color = Color.White,
                        fontSize = 20.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "ثبّت VLC أو MX Player أو أي مشغل فيديو",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    PlayerRow(
                        icon = "🌐",
                        name = "اسألني في كل مرة (نظام)",
                        isSelected = selectedPackage.isBlank(),
                        onClick = {
                            selectedPackage = ""
                            settings.externalPlayerPackage = ""
                        }
                    )
                }
                items(installed, key = { it.packageName }) { p ->
                    PlayerRow(
                        icon = p.icon,
                        name = p.name,
                        isSelected = selectedPackage == p.packageName,
                        onClick = {
                            selectedPackage = p.packageName
                            settings.externalPlayerPackage = p.packageName
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerRow(
    icon: String,
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .tvFocusable(onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1F6FEB) else Color(0xFF161B22)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 32.sp)
            Spacer(Modifier.width(16.dp))
            Text(
                name,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingRow(label: String, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White, fontSize = 18.sp)
        content()
    }
}
