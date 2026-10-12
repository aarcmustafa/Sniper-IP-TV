package com.stitten.stitteniptv.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.stitten.stitteniptv.R
import com.stitten.stitteniptv.data.ContentRepository
import com.stitten.stitteniptv.data.M3uParser
import com.stitten.stitteniptv.database.entity.SourceEntity
import com.stitten.stitteniptv.ui.components.tvFocusable
import com.stitten.stitteniptv.ui.navigation.Routes
import com.stitten.stitteniptv.ui.theme.Radius
import com.stitten.stitteniptv.ui.theme.Spacing
import com.stitten.stitteniptv.ui.theme.TvSurfaceHigh
import com.stitten.stitteniptv.viewmodel.MainViewModel
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

@Composable
fun AddSourceScreen(
    navController: NavHostController,
    editSourceId: Long = -1L,
    initialMode: Int = 0,
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

    var mode by remember { mutableIntStateOf(initialMode) }
    var name by remember { mutableStateOf("") }
    var m3uUrl by remember { mutableStateOf("") }
    var server by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var epgUrl by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    var localLoading by remember { mutableStateOf(false) }
    var pendingNavigate by remember { mutableStateOf(false) }
    var loadingStarted by remember { mutableStateOf(false) }

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
                mode = when (existing.type) {
                    "XTREAM" -> 2
                    else -> 0
                }
                m3uUrl = existing.url
                server = existing.url
                user = existing.username
                pass = existing.password
                epgUrl = existing.epgUrl
            }
        }
    }

    // الانتقال إلى Dashboard بعد اكتمال التحميل الفعلي
    LaunchedEffect(pendingNavigate, uiState.isLoading) {
        if (pendingNavigate) {
            if (uiState.isLoading) {
                loadingStarted = true
            } else if (loadingStarted && uiState.error == null) {
                navController.navigate(Routes.DASHBOARD) {
                    popUpTo(Routes.ADD_SOURCE) { inclusive = true }
                }
            }
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult

        try {
            ctx.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) { }

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
                AddSourceTabButton("M3U / M3U8", mode == 0, !isBusy) { mode = 0 }
                Spacer(Modifier.width(8.dp))
                AddSourceTabButton("ملف من الجهاز / USB", mode == 1, !isBusy) { mode = 1 }
                Spacer(Modifier.width(8.dp))
                AddSourceTabButton("Xtream API", mode == 2, !isBusy) { mode = 2 }
            }
            Spacer(Modifier.height(24.dp))

            when (mode) {

                // ═══════════════════════════════════════════════════
                //  M3U من رابط
                // ═══════════════════════════════════════════════════
                0 -> {
                    M3uForm(
                        m3uUrl = m3uUrl,
                        onM3uUrlChange = { m3uUrl = it },
                        epgUrl = epgUrl,
                        onEpgUrlChange = { epgUrl = it },
                        name = name,
                        isBusy = isBusy,
                        message = message,
                        onMessageChange = { message = it },
                        onSourceReady = { newSource ->
                            pendingNavigate = true
                            mainViewModel.switchSource(newSource)
                        },
                        onNavigateDirect = {
                            navController.navigate(Routes.DASHBOARD) {
                                popUpTo(Routes.ADD_SOURCE) { inclusive = true }
                            }
                        },
                        sourceMgr = sourceMgr,
                        scope = scope,
                        onLocalLoadingChange = { localLoading = it }
                    )
                }

                // ═══════════════════════════════════════════════════
                //  ملف من الجهاز
                // ═══════════════════════════════════════════════════
                1 -> {
                    FilePickerForm(
                        isBusy = isBusy,
                        onPickFile = { filePicker.launch(arrayOf("*/*")) }
                    )
                }

                // ═══════════════════════════════════════════════════
                //  Xtream API
                // ═══════════════════════════════════════════════════
                2 -> {
                    XtreamForm(
                        server = server,
                        onServerChange = { server = it },
                        user = user,
                        onUserChange = { user = it },
                        pass = pass,
                        onPassChange = { pass = it },
                        name = name,
                        isBusy = isBusy,
                        message = message,
                        onMessageChange = { message = it },
                        onSourceReady = { newSource ->
                            pendingNavigate = true
                            mainViewModel.switchSource(newSource)
                        },
                        onNavigateDirect = {
                            navController.navigate(Routes.DASHBOARD) {
                                popUpTo(Routes.ADD_SOURCE) { inclusive = true }
                            }
                        },
                        sourceMgr = sourceMgr,
                        scope = scope,
                        onLocalLoadingChange = { localLoading = it }
                    )
                }
            }

            if (message.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    message,
                    color = if (message.startsWith("خطأ") || message.contains("فشل") ||
                        message.contains("غير صحيحة") || message.contains("املأ"))
                        Color(0xFFDA3633)
                    else
                        Color(0xFF66BB6A),
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(600.dp)
                )
            }

            // ─── شريط التقدم ───
            if (isBusy) {
                Spacer(Modifier.height(24.dp))
                AddSourceProgressCard(
                    progress = progressPercent,
                    message = progressMessage
                )
            }

            Spacer(Modifier.height(24.dp))

            // ─── زر الرجوع ───
            Text(
                "← رجوع",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .tvFocusable(onClick = { navController.popBackStack() })
                    .background(TvSurfaceHigh, RoundedCornerShape(Radius.md))
                    .padding(horizontal = Spacing.xxl, vertical = Spacing.md)
            )
        }
    }
}
