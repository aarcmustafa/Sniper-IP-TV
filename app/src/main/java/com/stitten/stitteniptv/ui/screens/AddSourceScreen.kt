package com.stitten.stitteniptv.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.stitten.stitteniptv.R
import com.stitten.stitteniptv.data.ContentRepository
import com.stitten.stitteniptv.data.M3uParser
import com.stitten.stitteniptv.data.XtreamApi
import com.stitten.stitteniptv.database.entity.SourceEntity
import com.stitten.stitteniptv.ui.navigation.Routes
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

    var mode by remember { mutableStateOf(0) }
    var name by remember { mutableStateOf("") }
    var m3uUrl by remember { mutableStateOf("") }
    var server by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var epgUrl by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

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

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            loading = true
            try {
                val content = ctx.contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.use { it.readText() } ?: ""
                if (content.isBlank()) {
                    message = "الملف فارغ أو غير قابل للقراءة"
                    loading = false
                    return@launch
                }
                val channels = M3uParser.loadFromContent(content)
                if (channels.isEmpty()) {
                    message = "لا توجد قنوات في هذا الملف"
                    loading = false
                    return@launch
                }
                ContentRepository.channels = channels
                val finalName = name.ifBlank {
                    "M3U محلي: ${uri.lastPathSegment?.takeLast(20) ?: "مصدر"}"
                }
                val newId = sourceMgr.add(
                    SourceEntity(
                        name = finalName,
                        type = "M3U",
                        url = uri.toString(),
                        isActive = false
                    )
                )
                sourceMgr.setActive(newId)
                loading = false
                navController.navigate(Routes.DASHBOARD) {
                    popUpTo(Routes.ADD_SOURCE) { inclusive = true }
                }
            } catch (e: Exception) {
                message = "خطأ: ${e.message}"
                loading = false
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
            singleLine = true
        )
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.Center) {
            TabButton("M3U / M3U8", mode == 0) { mode = 0 }
            Spacer(Modifier.width(8.dp))
            TabButton("ملف من الجهاز / USB", mode == 1) { mode = 1 }
            Spacer(Modifier.width(8.dp))
            TabButton("Xtream API", mode == 2) { mode = 2 }
        }
        Spacer(Modifier.height(24.dp))

        when (mode) {
            0 -> {
                OutlinedTextField(
                    value = m3uUrl,
                    onValueChange = { m3uUrl = it },
                    label = { Text("رابط M3U / M3U8") },
                    modifier = Modifier.width(600.dp),
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = epgUrl,
                    onValueChange = { epgUrl = it },
                    label = { Text("رابط EPG (اختياري)") },
                    modifier = Modifier.width(600.dp),
                    singleLine = true
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        if (m3uUrl.isBlank()) {
                            message = "أدخل الرابط"; return@Button
                        }
                        scope.launch {
                            loading = true
                            try {
                                val channels = M3uParser.loadFromUrl(m3uUrl)
                                if (channels.isEmpty()) {
                                    message = "فشل تحميل القنوات"
                                    loading = false
                                    return@launch
                                }
                                ContentRepository.channels = channels
                                val newId = sourceMgr.add(
                                    SourceEntity(
                                        name = name.ifBlank { "M3U: ${m3uUrl.takeLast(30)}" },
                                        type = "M3U",
                                        url = m3uUrl,
                                        epgUrl = epgUrl,
                                        isActive = false
                                    )
                                )
                                sourceMgr.setActive(newId)
                                loading = false
                                navController.navigate(Routes.DASHBOARD) {
                                    popUpTo(Routes.ADD_SOURCE) { inclusive = true }
                                }
                            } catch (e: Exception) {
                                message = "خطأ: ${e.message}"
                                loading = false
                            }
                        }
                    },
                    modifier = Modifier.width(300.dp)
                ) { Text("حفظ وتحميل", fontSize = 18.sp) }
            }
            1 -> {
                Text(
                    "اختر ملف M3U من ذاكرة التلفاز الداخلية أو USB",
                    color = Color.White,
                    fontSize = 18.sp
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { filePicker.launch(arrayOf("*/*")) },
                    modifier = Modifier.width(400.dp)
                ) { Text("📂 فتح منتقي الملفات", fontSize = 18.sp) }
            }
            
                2 -> {
                    OutlinedTextField(
                        value = server,
                        onValueChange = { server = it },
                        label = { Text("Server URL") },
                        modifier = Modifier.width(600.dp),
                        singleLine = true
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = user,
                        onValueChange = { user = it },
                        label = { Text("Username") },
                        modifier = Modifier.width(600.dp),
                        singleLine = true
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pass,
                        onValueChange = { pass = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.width(600.dp),
                        singleLine = true
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = {
                            if (server.isBlank() || user.isBlank() || pass.isBlank()) {
                                message = "املأ كل الحقول"; return@Button
                            }
                            scope.launch {
                                loading = true
                                val valid = XtreamApi.validate(server, user, pass)
                                if (!valid) {
                                    message = "بيانات Xtream غير صحيحة أو السيرفر غير متاح"
                                    loading = false
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
                                loading = false
                                navController.navigate(Routes.DASHBOARD) {
                                    popUpTo(Routes.ADD_SOURCE) { inclusive = true }
                                }
                            }
                        },
                        modifier = Modifier.width(300.dp)
                    ) { Text("اختبار وحفظ", fontSize = 18.sp) }
                }
            }

            if (message.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(message, color = MaterialTheme.colorScheme.error, fontSize = 16.sp)
            }
            if (loading) {
                Spacer(Modifier.height(20.dp))
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun TabButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface
        )
    ) { Text(text, fontSize = 14.sp) }
}
