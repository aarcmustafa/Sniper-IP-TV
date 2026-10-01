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

// ============== شاشة الترحيب ==============
@Composable
private fun WelcomeScreen(
    onAddXtream: () -> Unit,
    onAddM3u: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "STTITEN IP TV",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "أضف حسابك للبدء",
            color = Color.White,
            fontSize = 20.sp
        )
        Spacer(Modifier.height(48.dp))

        WelcomeCard(
            icon = "🎬",
            title = "حساب Xtream",
            subtitle = "استخدم بيانات Xtream API",
            color = Color(0xFF1F6FEB),
            onClick = onAddXtream
        )

        Spacer(Modifier.height(16.dp))

        WelcomeCard(
            icon = "📥",
            title = "قائمة M3U / M3U8",
            subtitle = "رابط أو ملف من الجهاز",
            color = Color(0xFF238636),
            onClick = onAddM3u
        )
    }
}

@Composable
private fun WelcomeCard(
    icon: String,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .width(500.dp)
            .height(100.dp)
            .border(
                width = if (isFocused) 3.dp else 0.dp,
                color = if (isFocused) Color(0xFF58A6FF) else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter)
                ) {
                    onClick()
                    true
                } else false
            }
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = if (isFocused) 1f else 0.9f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 40.sp)
            Spacer(Modifier.width(20.dp))
            Column {
                Text(
                    title,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    subtitle,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            }
        }
    }
}

// ============== قائمة الحسابات ==============
@Composable
private fun AccountsListScreen(
    sources: List<SourceEntity>,
    onPickSource: (SourceEntity) -> Unit,
    onAddNew: () -> Unit,
    onEdit: (SourceEntity) -> Unit,
    onDelete: (SourceEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "STTITEN IP TV",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            Text(
                "${sources.size} حساب",
                color = Color.LightGray,
                fontSize = 16.sp
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "اختر حساباً للدخول",
            color = Color.LightGray,
            fontSize = 16.sp
        )

        Spacer(Modifier.height(24.dp))

        // ✅ LazyColumn مع مفاتيح فريدة
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(
                items = sources,
                key = { "account_${it.id}_${it.type}" } // ✅ مفتاح فريد
            ) { source ->
                AccountCard(
                    source = source,
                    onPick = { onPickSource(source) },
                    onEdit = { onEdit(source) },
                    onDelete = { onDelete(source) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onAddNew,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF238636)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(8.dp))
            Text("إضافة حساب جديد", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ============== بطاقة حساب ==============
@Composable
private fun AccountCard(
    source: SourceEntity,
    onPick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val isXtream = source.type == "XTREAM"
    val icon = if (isXtream) "🎬" else "📥"
    val typeName = if (isXtream) "Xtream API" else "قائمة M3U"
    val accentColor = if (isXtream) Color(0xFF1F6FEB) else Color(0xFF238636)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .border(
                width = if (isFocused) 3.dp else 0.dp,
                color = if (isFocused) Color(0xFF58A6FF) else Color.Transparent,
                shape = RoundedCornerShape(14.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (source.isActive) accentColor.copy(alpha = 0.3f)
            else Color(0xFF161B22)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(accentColor, RoundedCornerShape(12.dp))
                    .focusable()
                    .onFocusChanged { isFocused = it.isFocused }
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyUp &&
                            (event.key == Key.DirectionCenter || event.key == Key.Enter)
                        ) {
                            onPick()
                            true
                        } else false
                    }
                    .clickable(onClick = onPick),
                contentAlignment = Alignment.Center
            ) {
                if (source.isActive) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    Text(icon, fontSize = 32.sp)
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .focusable()
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyUp &&
                            (event.key == Key.DirectionCenter || event.key == Key.Enter)
                        ) {
                            onPick()
                            true
                        } else false
                    }
                    .clickable(onClick = onPick)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        source.name,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (source.isActive) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "نشط",
                            color = Color(0xFF66BB6A),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(typeName, color = accentColor, fontSize = 13.sp)
                if (source.url.isNotBlank()) {
                    Text(
                        source.url.take(50),
                        color = Color.Gray,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

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

// ============== حقل كلمة السر مع زر الإظهار المُصلَّح ==============
@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    showPassword: Boolean,
    onToggleShow: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Password") },
        visualTransformation = if (showPassword)
            VisualTransformation.None
        else
            PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password
        ),
        trailingIcon = {
            // ✅ الحل: focusable + onKeyEvent + clickable
            var isFocused by remember { mutableStateOf(false) }
            val interaction = remember { MutableInteractionSource() }

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .focusable(interactionSource = interaction)
                    .onFocusChanged { isFocused = it.isFocused }
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyUp &&
                            (event.key == Key.DirectionCenter || event.key == Key.Enter)
                        ) {
                            onToggleShow()
                            true
                        } else false
                    }
                    .clickable(
                        interactionSource = interaction,
                        indication = null
                    ) { onToggleShow() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (showPassword)
                        Icons.Default.VisibilityOff
                    else
                        Icons.Default.Visibility,
                    contentDescription = if (showPassword) "إخفاء" else "إظهار",
                    tint = if (isFocused) Color(0xFF58A6FF) else Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}
