package com.stitten.stitteniptv.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.stitten.stitteniptv.ui.navigation.Routes
import com.stitten.stitteniptv.viewmodel.MainViewModel

@Composable
fun LoginScreen(
    navController: NavHostController,
    viewModel: MainViewModel = viewModel()
) {
    val ctx = LocalContext.current
    var mode by remember { mutableStateOf(0) }
    var m3uUrl by remember { mutableStateOf("") }
    var server by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    val uiState by viewModel.uiState.collectAsState()

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val content = ctx.contentResolver.openInputStream(it)
                ?.bufferedReader()?.readText() ?: ""
            viewModel.loginM3u("", content) { ok ->
                if (ok) navController.navigate(Routes.DASHBOARD)
                else message = "فشل تحميل الملف"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "STTITEN IP TV",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text("اختر طريقة تسجيل الدخول", color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp)
        Spacer(Modifier.height(32.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            TabButton("M3U Online", mode == 0) { mode = 0 }
            Spacer(Modifier.width(12.dp))
            TabButton("M3U Local", mode == 1) { mode = 1 }
            Spacer(Modifier.width(12.dp))
            TabButton("Xtream API", mode == 2) { mode = 2 }
        }
        Spacer(Modifier.height(32.dp))

        when (mode) {
            0 -> {
                OutlinedTextField(
                    value = m3uUrl,
                    onValueChange = { m3uUrl = it },
                    label = { Text("رابط M3U / M3U8") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        if (m3uUrl.isBlank()) { message = "أدخل الرابط"; return@Button }
                        viewModel.loginM3u(m3uUrl) { ok ->
                            if (ok) navController.navigate(Routes.DASHBOARD)
                            else message = "فشل تحميل الرابط"
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("تحميل", fontSize = 18.sp) }
            }
            1 -> {
                Text("اختر ملف M3U من ذاكرة الجهاز أو USB",
                    color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp)
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { filePicker.launch("*/*") },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("فتح منتقي الملفات", fontSize = 18.sp) }
            }
            2 -> {
                OutlinedTextField(
                    value = server,
                    onValueChange = { server = it },
                    label = { Text("Server URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = user,
                    onValueChange = { user = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = pass,
                    onValueChange = { pass = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        if (server.isBlank() || user.isBlank() || pass.isBlank()) {
                            message = "املأ كل الحقول"; return@Button
                        }
                        viewModel.loginXtream(server, user, pass) { ok ->
                            if (ok) navController.navigate(Routes.DASHBOARD)
                            else message = "فشل الاتصال بالخادم"
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("تسجيل الدخول", fontSize = 18.sp) }
            }
        }

        if (message.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(message, color = MaterialTheme.colorScheme.error, fontSize = 16.sp)
        }
        if (uiState.isLoading) {
            Spacer(Modifier.height(20.dp))
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun TabButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surface,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface
        )
    ) { Text(text, fontSize = 14.sp) }
}
