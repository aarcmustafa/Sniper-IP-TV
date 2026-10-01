package com.stitten.stitteniptv.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.stitten.stitteniptv.R
import com.stitten.stitteniptv.database.entity.SourceEntity
import com.stitten.stitteniptv.ui.navigation.Routes
import com.stitten.stitteniptv.viewmodel.MainViewModel
import dagger.hilt.android.EntryPointAccessors

@Composable
fun LoginScreen(
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
    val sources by sourceMgr.getAll().collectAsState(initial = emptyList())
    val uiState by viewModel.uiState.collectAsState()

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

        // ✅ استخدام key لضمان إعادة بناء الواجهة عند تغيير المصادر
        key(sources.size) {
            if (sources.isEmpty()) {
                WelcomeScreen(
                    onAddXtream = { navController.navigate("add_source?type=XTREAM") },
                    onAddM3u = { navController.navigate("add_source?type=M3U") }
                )
            } else {
                AccountsListScreen(
                    sources = sources,
                    onPickSource = { source ->
                        viewModel.switchSource(source)
                        navController.navigate(Routes.DASHBOARD)
                    },
                    onAddNew = { navController.navigate(Routes.ADD_SOURCE) },
                    onEdit = { source ->
                        navController.navigate(Routes.addSourceEdit(source.id))
                    },
                    onDelete = { source -> confirmDelete = source }
                )
            }
        }

        // ✅ دائرة النسبة المئوية
        if (uiState.isLoading) {
            CircularProgressOverlay(
                progress = uiState.loadingProgress,
                message = uiState.loadingMessage
            )
        }
    }

    confirmDelete?.let { source ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("تأكيد الحذف") },
            text = { Text("هل تريد حذف \"${source.name}\" نهائياً؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSource(source) {
                            confirmDelete = null
                        }
                    }
                ) { Text("حذف", color = Color(0xFFDA3633)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

// ============== 🎯 دائرة النسبة المئوية ==============
@Composable
fun CircularProgressOverlay(
    progress: Int,
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(40.dp)
                    .width(400.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // الدائرة مع النسبة
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(200.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.size(200.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 14.dp,
                        trackColor = Color(0xFF21262D)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$progress%",
                            color = Color.White,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "مكتمل",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                // الرسالة
                Text(
                    message.ifBlank { "جاري التحميل..." },
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(20.dp))

                // شريط خطي
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color(0xFF21262D)
                )
            }
        }
    }
}
