package com.stitten.stitteniptv.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.stitten.stitteniptv.R
import com.stitten.stitteniptv.data.ContentRepository
import com.stitten.stitteniptv.data.M3uParser
import com.stitten.stitteniptv.data.XtreamApi
import com.stitten.stitteniptv.database.entity.SourceEntity
import com.stitten.stitteniptv.ui.components.tvFocusable
import com.stitten.stitteniptv.ui.navigation.Routes
import com.stitten.stitteniptv.viewmodel.MainViewModel
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

@Composable
fun AddSourceScreen(
    navController: NavHostController,
    editSourceId: Long = -1L
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

    val mainViewModel: MainViewModel = viewModel()
    val uiState by mainViewModel.uiState.collectAsState()

    var mode by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var m3uUrl by remember { mutableStateOf("") }
    var server by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var epgUrl by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    var localLoading by remember { mutableStateOf(false) }
    var pendingNavigate by remember { mutableStateOf(false) }

    val isBusy = localLoading || uiState.isLoading

    val progressPercent = when {
        uiState.isLoading -> uiState.loadingProgress
        localLoading -> 15
        else -> 0
    }

    val progressMessage = when {
        uiState.isLoading -> uiState.loadingMessage
        localLoading -> "⏳ جاري التحقق..."
        else -> ""
    }

    LaunchedEffect(editSourceId) {
        if (editSourceId > 0) {
            val existing = sourceMgr.getById(editSourceId)
            if (existing != null) {
                name = existing.name
                mode = if (existing.type == "XTREAM") 2 else 0
                m3uUrl = existing.url
                server = existing.url
                user = existing.username
                pass = existing.password
                epgUrl = existing.epgUrl
            }
        }
    }

    LaunchedEffect(pendingNavigate, uiState.isLoading) {
        if (pendingNavigate && !uiState.isLoading) {
            navController.navigate(Routes.DASHBOARD) {
                popUpTo(Routes.ADD_SOURCE) { inclusive = true }
            }
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            localLoading = true
            message = ""
            try {
                val content = ctx.contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.use { it.readText() } ?: ""
                if (content.isBlank()) {
                    message = "الملف فارغ أو غير قابل للقراءة"
                    localLoading = false
                    return@launch
                }
                val channels = M3uParser.loadFromContent(content)
                if (channels.isEmpty()) {
                    message = "لا توجد قنوات في هذا الملف"
                    localLoading = false
                    return@launch
                }
                ContentRepository.channels = channels
                val finalName = name.ifBlank {
                    "M3U محلي: ${uri.lastPathSegment?.takeLast(20) ?: "مصدر"}"
                }

                // ✅ تمييز M3U8 عن M3U للملفات
                val detectedType = if (
                    uri.toString().lowercase().contains(".m3u8") ||
                    finalName.lowercase().contains(".m3u8")
                ) "M3U8" else "M3U"

                val newId = sourceMgr.add(
                    SourceEntity(
                        name = finalName,
                        type = detectedType,
                        url = uri.toString(),
                        isActive = false
                    )
                )
                sourceMgr.setActive(newId)
                localLoading = false
                navController.navigate(Routes.DASHBOARD) {
                    popUpTo(Routes.ADD_SOURCE) { inclusive = true }
                }
            } catch (e: Exception) {
                message = "خطأ: ${e.message}"
                localLoading = false
            }
        }
    }
    
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
                .padding(32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                if (editSourceId > 0) "تعديل مصدر" else "إضافة مصدر جديد",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("اسم المصدر (اختياري)") },
                modifier = Modifier.width(600.dp),
                singleLine = true,
                enabled = !isBusy
            )
            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.Center) {
                TabButton("M3U / M3U8", mode == 0, !isBusy) { mode = 0 }
                Spacer(Modifier.width(8.dp))
                TabButton("ملف من الجهاز / USB", mode == 1, !isBusy) { mode = 1 }
                Spacer(Modifier.width(8.dp))
                TabButton("Xtream API", mode == 2, !isBusy) { mode = 2 }
            }
            Spacer(Modifier.height(24.dp))

            when (mode) {

                // ═══════════════════════════════════════════════════
                //  M3U من رابط
                // ═══════════════════════════════════════════════════
                0 -> {
                    OutlinedTextField(
                        value = m3uUrl,
                        onValueChange = { m3uUrl = it },
                        label = { Text("رابط M3U / M3U8") },
                        placeholder = { Text("http://example.com/list.m3u") },
                        modifier = Modifier.width(600.dp),
                        singleLine = true,
                        enabled = !isBusy
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = epgUrl,
                        onValueChange = { epgUrl = it },
                        label = { Text("رابط EPG (اختياري)") },
                        placeholder = { Text("http://example.com/epg.xml") },
                        modifier = Modifier.width(600.dp),
                        singleLine = true,
                        enabled = !isBusy
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = {
                            if (m3uUrl.isBlank()) {
                                message = "أدخل الرابط"
                                return@Button
                            }
                            message = ""
                            scope.launch {
                                localLoading = true
                                try {
                                    val channels = M3uParser.loadFromUrl(m3uUrl)
                                    if (channels.isEmpty()) {
                                        message = "فشل تحميل القنوات — تحقق من الرابط"
                                        localLoading = false
                                        return@launch
                                    }
                                    ContentRepository.channels = channels

                                    // ✅ تمييز M3U8 عن M3U
                                    val detectedType = if (m3uUrl.lowercase().contains(".m3u8"))
                                        "M3U8" else "M3U"

                                    val newId = sourceMgr.add(
                                        SourceEntity(
                                            name = name.ifBlank {
                                                "$detectedType: ${m3uUrl.takeLast(30)}"
                                            },
                                            type = detectedType,
                                            url = m3uUrl,
                                            epgUrl = epgUrl,
                                            isActive = false
                                        )
                                    )
                                    sourceMgr.setActive(newId)
                                    localLoading = false

                                    val newSource = sourceMgr.getById(newId)
                                    if (newSource != null) {
                                        pendingNavigate = true
                                        mainViewModel.switchSource(newSource)
                                    } else {
                                        navController.navigate(Routes.DASHBOARD) {
                                            popUpTo(Routes.ADD_SOURCE) { inclusive = true }
                                        }
                                    }
                                } catch (e: Exception) {
                                    message = "خطأ: ${e.message}"
                                    localLoading = false
                                }
                            }
                        },
                        modifier = Modifier.width(300.dp).height(56.dp),
                        enabled = !isBusy
                    ) { Text("حفظ وتحميل", fontSize = 18.sp) }
                }

                // ═══════════════════════════════════════════════════
                //  ملف من الجهاز
                // ═══════════════════════════════════════════════════
                1 -> {
                    Text(
                        "اختر ملف M3U من ذاكرة التلفاز الداخلية أو USB",
                        color = Color.White,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { filePicker.launch(arrayOf("*/*")) },
                        modifier = Modifier.width(400.dp).height(56.dp),
                        enabled = !isBusy
                    ) { Text("📂 فتح منتقي الملفات", fontSize = 18.sp) }
                }

                // ═══════════════════════════════════════════════════
                //  Xtream API
                // ═══════════════════════════════════════════════════
                2 -> {
                    OutlinedTextField(
                        value = server,
                        onValueChange = { server = it },
                        label = { Text("Server URL") },
                        placeholder = { Text("http://example.com:8080") },
                        modifier = Modifier.width(600.dp),
                        singleLine = true,
                        enabled = !isBusy
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "أدخل اسم السيرفر كاملاً: http://اسم-السيرفر:المنفذ",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        modifier = Modifier.width(600.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = user,
                        onValueChange = { user = it },
                        label = { Text("Username") },
                        modifier = Modifier.width(600.dp),
                        singleLine = true,
                        enabled = !isBusy
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pass,
                        onValueChange = { pass = it },
                        label = { Text("Password") },
                        visualTransformation = if (showPassword)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password
                        ),
                        trailingIcon = {
                            // ✅ Box + tvFocusable بدل IconButton
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .tvFocusable(onClick = { showPassword = !showPassword }),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = if (showPassword)
                                        Icons.Default.VisibilityOff
                                    else
                                        Icons.Default.Visibility,
                                    contentDescription = if (showPassword)
                                        "إخفاء كلمة المرور"
                                    else
                                        "إظهار كلمة المرور",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        },
                        modifier = Modifier.width(600.dp),
                        singleLine = true,
                        enabled = !isBusy
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = {
                            if (server.isBlank() || user.isBlank() || pass.isBlank()) {
                                message = "املأ كل الحقول"
                                return@Button
                            }
                            if (!server.contains(".")) {
                                message = "أدخل اسم سيرفر صحيح"
                                return@Button
                            }
                            message = ""
                            scope.launch {
                                localLoading = true
                                val valid = XtreamApi.validate(server, user, pass)
                                if (!valid) {
                                    message = "بيانات Xtream غير صحيحة أو السيرفر غير متاح"
                                    localLoading = false
                                    return@launch
                                }
                                val newId = sourceMgr.add(
                                    SourceEntity(
                                        name = name.ifBlank { "Xtream: $user" },
                                        type = "XTREAM",
                                        url = server,
                                        username = user,
                                        password = pass,
                                        isActive = false
                                    )
                                )
                                sourceMgr.setActive(newId)
                                localLoading = false

                                val newSource = sourceMgr.getById(newId)
                                if (newSource != null) {
                                    pendingNavigate = true
                                    mainViewModel.switchSource(newSource)
                                } else {
                                    navController.navigate(Routes.DASHBOARD) {
                                        popUpTo(Routes.ADD_SOURCE) { inclusive = true }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.width(300.dp).height(56.dp),
                        enabled = !isBusy
                    ) { Text("اختبار وحفظ", fontSize = 18.sp) }
                }
            }

            // ─── رسالة الخطأ ───
            if (message.isNotEmpty() && !isBusy) {
                Spacer(Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFDA3633).copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.width(600.dp)
                ) {
                    Text(
                        text = "⚠️ $message",
                        color = Color(0xFFFF7B72),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }

        // ═══════════════════════════════════════════════════════════
        //  Progress Overlay
        // ═══════════════════════════════════════════════════════════
        if (isBusy) {
            ProgressOverlay(
                progress = progressPercent,
                message = progressMessage,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  Progress Overlay
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ProgressOverlay(
    progress: Int,
    message: String,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.width(420.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(40.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(200.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { (progress / 100f).coerceIn(0f, 1f) },
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

                Text(
                    message.ifBlank { "جاري التحميل..." },
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(20.dp))

                LinearProgressIndicator(
                    progress = { (progress / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color(0xFF21262D)
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    "لا تغلق التطبيق",
                    color = Color(0xFF8B949E),
                    fontSize = 12.sp,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  زر تبويب
// ═══════════════════════════════════════════════════════════════

@Composable
private fun TabButton(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface
        )
    ) { Text(text, fontSize = 14.sp) }
}
