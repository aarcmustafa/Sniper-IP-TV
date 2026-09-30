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
import androidx.navigation.NavHostController
import com.stitten.stitteniptv.R
import com.stitten.stitteniptv.ui.components.tvFocusable
import com.stitten.stitteniptv.ui.navigation.Routes
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

@Composable
fun SourcesManagerScreen(navController: NavHostController) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val entryPoint = remember {
        EntryPointAccessors.fromApplication(
            ctx.applicationContext,
            PlayerEntryPoint::class.java
        )
    }
    val sourceMgr = entryPoint.sourceManager()
    val sources by sourceMgr.getAll().collectAsState(initial = emptyList())

    var confirmDeleteId by remember { mutableStateOf<Long?>(null) }

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
                Button(onClick = { navController.navigate(Routes.ADD_SOURCE) }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("إضافة مصدر", fontSize = 16.sp)
                }
            }
            Spacer(Modifier.height(20.dp))

            if (sources.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📡", fontSize = 64.sp, color = Color.Gray)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "لا توجد مصادر بعد",
                            color = Color.White,
                            fontSize = 22.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "اضغط 'إضافة مصدر' لإضافة أول قائمة",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    
                    items(sources, key = { it.id }) { src ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .tvFocusable {
                                    scope.launch {
                                        sourceMgr.setActive(src.id)
                                        navController.navigate(Routes.DASHBOARD) {
                                            popUpTo(Routes.SOURCES) { inclusive = true }
                                        }
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (src.isActive) Color(0xFF1F6FEB)
                                else Color(0xFF161B22).copy(alpha = 0.9f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            src.name,
                                            color = Color.White,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (src.isActive) {
                                            Spacer(Modifier.width(8.dp))
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                "نشط",
                                                color = Color.White,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "النوع: ${src.type}",
                                        color = Color.LightGray,
                                        fontSize = 14.sp
                                    )
                                    if (src.url.isNotBlank()) {
                                        Text(
                                            src.url.take(60),
                                            color = Color.Gray,
                                            fontSize = 12.sp,
                                            maxLines = 1
                                        )
                                    }
                                }

                                IconButton(onClick = {
                                    navController.navigate(Routes.addSourceEdit(src.id))
                                }) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "تعديل",
                                        tint = Color.White
                                    )
                                }

                                IconButton(onClick = { confirmDeleteId = src.id }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "حذف",
                                        tint = Color(0xFFDA3633)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    confirmDeleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { confirmDeleteId = null },
            title = { Text("تأكيد الحذف") },
            text = { Text("هل تريد حذف هذا المصدر نهائياً؟") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val src = sources.firstOrNull { it.id == id }
                        if (src != null) sourceMgr.delete(src)
                        confirmDeleteId = null
                    }
                }) { Text("حذف", color = Color(0xFFDA3633)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteId = null }) { Text("إلغاء") }
            }
        )
    }
}
                    
