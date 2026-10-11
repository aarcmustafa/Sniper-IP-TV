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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.stitten.stitteniptv.R
import com.stitten.stitteniptv.database.entity.SourceEntity
import com.stitten.stitteniptv.ui.components.tvFocusable
import com.stitten.stitteniptv.ui.navigation.Routes
import com.stitten.stitteniptv.ui.theme.AppIcons
import com.stitten.stitteniptv.viewmodel.MainViewModel
import dagger.hilt.android.EntryPointAccessors

@Composable
fun SourcesManagerScreen(
    navController: NavHostController,
    viewModel: MainViewModel = viewModel()
) {
    val ctx = LocalContext.current

    val entryPoint = remember {
        EntryPointAccessors.fromApplication(
            ctx.applicationContext,
            PlayerEntryPoint::class.java
        )
    }
    val sourceMgr = entryPoint.sourceManager()

    val allSources by sourceMgr.getAll().collectAsState(initial = emptyList())

    val xtreamSources = allSources.filter { it.type == "XTREAM" }
    val m3uSources = allSources.filter { it.type == "M3U" }
    val m3u8Sources = allSources.filter { it.type == "M3U8" }

    var confirmDelete by remember { mutableStateOf<SourceEntity?>(null) }

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
            SourcesTopBar(
                totalCount = allSources.size,
                onBack = { navController.popBackStack() },
            )

            Spacer(Modifier.height(20.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {

                // ═══════════════════════════════════════════════════════
                //  قسم Xtream
                // ═══════════════════════════════════════════════════════
                item(key = "xtream_header") {
                    SourcesSectionHeader(
                        icon = AppIcons.XtreamSource,
                        title = "حسابات Xtream",
                        count = xtreamSources.size,
                        maxCount = 10,
                        onAdd = {
                            if (xtreamSources.size < 10) {
                                navController.navigate(Routes.ADD_SOURCE)
                            }
                        },
                    )
                }

                if (xtreamSources.isEmpty()) {
                    item(key = "xtream_empty") {
                        EmptySourceRow(text = "لا توجد حسابات Xtream بعد")
                    }
                } else {
                    items(xtreamSources, key = { "xt_${it.id}" }) { src ->
                        SourceCard(
                            source = src,
                            onActivate = {
                                viewModel.switchSource(src)
                                navController.navigate(Routes.DASHBOARD) {
                                    popUpTo(Routes.SOURCES) { inclusive = true }
                                }
                            },
                            onEdit = {
                                navController.navigate(Routes.addSourceEdit(src.id))
                            },
                            onDelete = { confirmDelete = src }
                        )
                    }
                }

                item { Spacer(Modifier.height(28.dp)) }

                // ═══════════════════════════════════════════════════════
                //  قسم M3U
                // ═══════════════════════════════════════════════════════
                item(key = "m3u_header") {
                    SourcesSectionHeader(
                        icon = AppIcons.M3uSource,
                        title = "قوائم M3U",
                        count = m3uSources.size,
                        maxCount = null,
                        onAdd = {
                            navController.navigate(Routes.ADD_SOURCE)
                        },
                    )
                }

                if (m3uSources.isEmpty()) {
                    item(key = "m3u_empty") {
                        EmptySourceRow(text = "لا توجد قوائم M3U بعد")
                    }
                } else {
                    items(m3uSources, key = { "m3u_${it.id}" }) { src ->
                        SourceCard(
                            source = src,
                            onActivate = {
                                viewModel.switchSource(src)
                                navController.navigate(Routes.DASHBOARD) {
                                    popUpTo(Routes.SOURCES) { inclusive = true }
                                }
                            },
                            onEdit = {
                                navController.navigate(Routes.addSourceEdit(src.id))
                            },
                            onDelete = { confirmDelete = src }
                        )
                    }
                }

                item { Spacer(Modifier.height(28.dp)) }

                // ═══════════════════════════════════════════════════════
                //  قسم M3U8
                // ═══════════════════════════════════════════════════════
                item(key = "m3u8_header") {
                    SourcesSectionHeader(
                        icon = AppIcons.M3u8Source,
                        title = "قوائم M3U8 (HLS)",
                        count = m3u8Sources.size,
                        maxCount = null,
                        onAdd = {
                            navController.navigate(Routes.ADD_SOURCE)
                        },
                    )
                }

                if (m3u8Sources.isEmpty()) {
                    item(key = "m3u8_empty") {
                        EmptySourceRow(text = "لا توجد قوائم M3U8 بعد")
                    }
                } else {
                    items(m3u8Sources, key = { "m3u8_${it.id}" }) { src ->
                        SourceCard(
                            source = src,
                            onActivate = {
                                viewModel.switchSource(src)
                                navController.navigate(Routes.DASHBOARD) {
                                    popUpTo(Routes.SOURCES) { inclusive = true }
                                }
                            },
                            onEdit = {
                                navController.navigate(Routes.addSourceEdit(src.id))
                            },
                            onDelete = { confirmDelete = src }
                        )
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    confirmDelete?.let { src ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("تأكيد الحذف") },
            text = { Text("هل تريد حذف \"${src.name}\" نهائياً؟") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSource(src) {
                        confirmDelete = null
                    }
                }) { Text("حذف", color = Color(0xFFDA3633)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text("إلغاء") }
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  الشريط العلوي
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SourcesTopBar(
    totalCount: Int,
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
            imageVector = AppIcons.Source,
            contentDescription = null,
            tint = Color(0xFF1F6FEB),
            modifier = Modifier.size(32.dp),
        )

        Spacer(Modifier.width(12.dp))

        Text(
            text = "إدارة المصادر",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.weight(1f))

        // الإجمالي
        if (totalCount > 0) {
            Text(
                text = "$totalCount مصدر",
                color = Color(0xFF8B949E),
                fontSize = 14.sp,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  عنوان قسم المصادر
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SourcesSectionHeader(
    icon: ImageVector,
    title: String,
    count: Int,
    maxCount: Int?,
    onAdd: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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

            Spacer(Modifier.width(8.dp))

            // عدّاد
            val countText = if (maxCount != null) "($count/$maxCount)" else "($count)"
            val countColor = if (maxCount != null && count >= maxCount)
                Color(0xFFDA3633) else Color(0xFF8B949E)
            Text(
                text = countText,
                color = countColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )

            Spacer(Modifier.weight(1f))

            // زر إضافة
            val canAdd = maxCount == null || count < maxCount
            Button(
                onClick = onAdd,
                enabled = canAdd,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF238636),
                    disabledContainerColor = Color(0xFF30363D),
                ),
                shape = RoundedCornerShape(10.dp),
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("إضافة", fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = Color(0xFF30363D))
        Spacer(Modifier.height(8.dp))
    }
}

// ═══════════════════════════════════════════════════════════════
//  صف فارغ
// ═══════════════════════════════════════════════════════════════

@Composable
private fun EmptySourceRow(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color(0xFF8B949E),
            fontSize = 15.sp,
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  بطاقة مصدر — مع أيقونة ولون حسب النوع
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SourceCard(
    source: SourceEntity,
    onActivate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    // تحديد النوع
    val accentColor = when (source.type) {
        "XTREAM" -> Color(0xFF1F6FEB)   // أزرق
        "M3U8" -> Color(0xFF8B5CF6)     // بنفسجي
        else -> Color(0xFF238636)       // أخضر (M3U)
    }

    val iconVector = when (source.type) {
        "XTREAM" -> AppIcons.XtreamSource
        "M3U8" -> AppIcons.M3u8Source
        else -> AppIcons.M3uSource
    }

    val typeName = when (source.type) {
        "XTREAM" -> "Xtream Codes API"
        "M3U8" -> "قائمة M3U8 (HLS)"
        else -> "قائمة M3U"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (source.isActive) accentColor.copy(alpha = 0.25f)
            else Color(0xFF161B22).copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ─── زر التفعيل (البطاقة كاملة) ───
            Box(
                modifier = Modifier
                    .weight(1f)
                    .tvFocusable(onActivate)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // أيقونة النوع + علامة نشط
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                accentColor,
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = typeName,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp),
                        )

                        if (source.isActive) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(3.dp)
                                    .size(16.dp)
                                    .background(Color(0xFF66BB6A), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "نشط",
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = source.name,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (source.isActive) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "نشط",
                                    color = Color(0xFF66BB6A),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }

                        Spacer(Modifier.height(2.dp))

                        Text(
                            text = typeName,
                            color = accentColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )

                        if (source.url.isNotBlank()) {
                            Text(
                                text = source.url.take(60),
                                color = Color(0xFF6E7681),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            // ─── زر التعديل ───
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "تعديل",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }

            // ─── زر الحذف ───
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "حذف",
                    tint = Color(0xFFDA3633),
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
