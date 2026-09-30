package com.stitten.stitteniptv.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.stitten.stitteniptv.data.CategoriesManager
import com.stitten.stitteniptv.data.ContentRepository
import com.stitten.stitteniptv.data.PrefsManager
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

@Composable
fun ParentalControlScreen(navController: NavHostController) {
    val ctx = LocalContext.current
    val prefs = remember { PrefsManager(ctx) }
    val scope = rememberCoroutineScope()

    val entryPoint = remember {
        EntryPointAccessors.fromApplication(
            ctx.applicationContext,
            PlayerEntryPoint::class.java
        )
    }
    val manager = entryPoint.parentalControlManager()

    var enteredPin by remember { mutableStateOf("") }
    var unlocked by remember { mutableStateOf(prefs.parentalPin.isBlank()) }
    var error by remember { mutableStateOf("") }
    var pinField by remember { mutableStateOf(prefs.parentalPin) }

    if (!unlocked) {
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
                    .background(Color.Black.copy(alpha = 0.8f))
            )

            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "🔒 الرقابة الأبوية",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(24.dp))
                OutlinedTextField(
                    value = enteredPin,
                    onValueChange = {
                        enteredPin = it.filter { c -> c.isDigit() }.take(6)
                    },
                    label = { Text("أدخل PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.width(300.dp),
                    singleLine = true
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    if (enteredPin == prefs.parentalPin) {
                        unlocked = true
                        error = ""
                    } else error = "PIN غير صحيح"
                }) { Text("فتح", fontSize = 18.sp) }
                if (error.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
        }
        return
    }
    
    val allCategories = remember {
        (CategoriesManager.extractChannelCategories(ContentRepository.channels) +
            CategoriesManager.extractMovieCategories(ContentRepository.movies)).distinct()
    }

    val blocks by manager.getAllBlocks().collectAsState(initial = emptyList())

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
            modifier = Modifier.fillMaxSize().padding(32.dp)
        ) {
            Text(
                "🔒 الرقابة الأبوية",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = pinField,
                onValueChange = { v ->
                    pinField = v.filter { c -> c.isDigit() }.take(6)
                    prefs.parentalPin = pinField
                },
                label = { Text("تعيين PIN (4-6 أرقام)") },
                modifier = Modifier.width(400.dp),
                singleLine = true
            )

            Spacer(Modifier.height(24.dp))
            Text(
                "حجب التصنيفات:",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(allCategories) { cat ->
                    val blockId = "GROUP:$cat"
                    val isBlocked = blocks.any { it.blockId == blockId }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = isBlocked,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    if (checked) {
                                        manager.addBlock(blockId, "GROUP", cat)
                                    } else {
                                        val item = blocks.firstOrNull { it.blockId == blockId }
                                        if (item != null) manager.removeBlock(item)
                                    }
                                }
                            }
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(cat, color = Color.White, fontSize = 18.sp)
                    }
                }
            }
        }
    }
}
