package com.stitten.stitteniptv.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.stitten.stitteniptv.database.entity.SourceEntity
import com.stitten.stitteniptv.ui.components.tvFocusable
import com.stitten.stitteniptv.ui.navigation.Routes
import com.stitten.stitteniptv.viewmodel.MainViewModel
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

@Composable
fun SourcesManagerScreen(
    navController: NavHostController,
    viewModel: MainViewModel = viewModel()
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

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
                .background(Color.Black.copy(alpha = 0.75f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // العنوان
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "📡 إدارة المصادر",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "Xtream: ${xtreamSources.size}/10",
                    color = if (xtreamSources.size >= 10) Color(0xFFDA3633) else Color.White,
                    fontSize = 14.sp
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "M3U: ${m3uSources.size}",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }

            Spacer(Modifier.height(20.dp))
            
            LazyColumn(modifier = Modifier.fillMaxSize()) {

                // ============== قسم Xtream ==============
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "🎬 حسابات Xtream",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (xtreamSources.size >= 10) return@Button
                                navController.navigate(Routes.ADD_SOURCE + "?type=XTREAM")
                            },
                            enabled = xtreamSources.size < 10
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("إضافة", fontSize = 14.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                if (xtreamSources.isEmpty()) {
                    item {
                        Text(
                            "لا توجد حسابات Xtream بعد",
                            color = Color.Gray,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
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

                item { Spacer(Modifier.height(24.dp)) }

                // ============== قسم M3U ==============
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "📥 قوائم M3U",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                navController.navigate(Routes.ADD_SOURCE + "?type=M3U")
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("إضافة", fontSize = 14.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                if (m3uSources.isEmpty()) {
                    item {
                        Text(
                            "لا توجد قوائم M3U بعد",
                            color = Color.Gray,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                } else {
                    items(m3uSources, key = { "m3_${it.id}" }) { src ->
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

@Composable
private fun SourceCard(
    source: SourceEntity,
    onActivate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (source.isActive) Color(0xFF1F6FEB)
            else Color(0xFF161B22).copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // زر التفعيل (الضغط على البطاقة)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .tvFocusable(onActivate)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            source.name,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (source.isActive) {
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("نشط", color = Color.White, fontSize = 14.sp)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (source.type == "XTREAM")
                            "Xtream: ${source.username}"
                        else
                            "M3U محلي",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                    if (source.url.isNotBlank()) {
                        Text(
                            source.url.take(60),
                            color = Color.Gray,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            // زر التعديل
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "تعديل",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // زر الحذف
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "حذف",
                    tint = Color(0xFFDA3633),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
