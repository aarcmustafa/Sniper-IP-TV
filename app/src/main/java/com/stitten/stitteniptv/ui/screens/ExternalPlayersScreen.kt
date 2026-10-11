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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.stitten.stitteniptv.R
import com.stitten.stitteniptv.data.ExternalPlayerManager
import com.stitten.stitteniptv.ui.components.tvFocusable
import com.stitten.stitteniptv.ui.theme.AppIcons
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

    Box(modifier = Modifier.fillMaxSize()) {

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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {

            // ═══════════════════════════════════════════════════════
            //  الشريط العلوي
            // ═══════════════════════════════════════════════════════
            PlayersTopBar(
                installedCount = installed.size,
                onBack = { navController.popBackStack() },
            )

            Spacer(Modifier.height(24.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {

                // ═══════════════════════════════════════════════════
                //  ⚙️ السلوك
                // ═══════════════════════════════════════════════════
                item(key = "behavior_header") {
                    SectionHeader(
                        icon = Icons.Default.Settings,
                        title = "السلوك الافتراضي",
                    )
                }

                item(key = "behavior_row") {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF161B22).copy(alpha = 0.9f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .tvFocusable {
                                        alwaysAsk = !alwaysAsk
                                        settings.alwaysAskExternalPlayer = alwaysAsk
                                    }
                                    .padding(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFF1F6FEB),
                                    modifier = Modifier.size(24.dp),
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "اسألني في كل مرة",
                                        color = Color.White,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        text = "عند الضغط على 'فتح بمشغل خارجي'",
                                        color = Color(0xFF8B949E),
                                        fontSize = 12.sp,
                                    )
                                }
                                Switch(
                                    checked = alwaysAsk,
                                    onCheckedChange = {
                                        alwaysAsk = it
                                        settings.alwaysAskExternalPlayer = it
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF1F6FEB),
                                    ),
                                )
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(28.dp)) }

                // ═══════════════════════════════════════════════════
                //  🎬 المشغلات المثبتة
                // ═══════════════════════════════════════════════════
                item(key = "players_header") {
                    SectionHeader(
                        icon = Icons.Default.PlayArrow,
                        title = "المشغلات المثبتة",
                        count = installed.size,
                    )
                }

                // ─── خيار "اسألني في كل مرة" ───
                item(key = "system_row") {
                    PlayerRow(
                        icon = AppIcons.LiveTv,
                        name = "اسألني في كل مرة (نظام)",
                        subtitle = "سيُظهر النظام قائمة اختيار",
                        isSelected = selectedPackage.isBlank(),
                        onClick = {
                            selectedPackage = ""
                            settings.externalPlayerPackage = ""
                        },
                    )
                }

                if (installed.isEmpty()) {
                    item(key = "empty") {
                        EmptyPlayersView()
                    }
                } else {
                    items(installed, key = { "pkg_${it.packageName}" }) { p ->
                        PlayerRow(
                            icon = AppIcons.Movies,
                            name = p.name,
                            subtitle = p.packageName,
                            isSelected = selectedPackage == p.packageName,
                            onClick = {
                                selectedPackage = p.packageName
                                settings.externalPlayerPackage = p.packageName
                            },
                        )
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  الشريط العلوي
// ═══════════════════════════════════════════════════════════════

@Composable
private fun PlayersTopBar(
    installedCount: Int,
    onBack: () -> Unit,
) {
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
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = Color(0xFF1F6FEB),
            modifier = Modifier.size(32.dp),
        )
        Spacer(Modifier.width(12.dp))

        Column {
            Text(
                text = "المشغل الخارجي",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (installedCount == 0)
                    "لا توجد مشغلات مثبتة"
                else
                    "$installedCount مشغل مثبت",
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
    count: Int? = null,
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
            if (count != null) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "($count)",
                    color = Color(0xFF8B949E),
                    fontSize = 14.sp,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = Color(0xFF30363D))
        Spacer(Modifier.height(12.dp))
    }
}

// ═══════════════════════════════════════════════════════════════
//  صف مشغل
// ═══════════════════════════════════════════════════════════════

@Composable
private fun PlayerRow(
    icon: ImageVector,
    name: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .tvFocusable(onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                Color(0xFF1F6FEB).copy(alpha = 0.3f)
            else Color(0xFF161B22).copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // أيقونة المشغل
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (isSelected) Color(0xFF1F6FEB)
                        else Color(0xFF21262D),
                        RoundedCornerShape(10.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp),
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        color = Color(0xFF8B949E),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "مختار",
                    tint = Color(0xFF66BB6A),
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  حالة فارغة
// ═══════════════════════════════════════════════════════════════

@Composable
private fun EmptyPlayersView() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color(0xFF30363D),
                modifier = Modifier.size(72.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "لم يُعثر على مشغلات خارجية",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "ثبّت VLC أو MX Player أو أي مشغل فيديو",
                color = Color(0xFF8B949E),
                fontSize = 14.sp,
            )
        }
    }
}
