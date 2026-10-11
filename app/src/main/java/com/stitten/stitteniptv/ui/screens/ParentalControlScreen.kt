package com.stitten.stitteniptv.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.stitten.stitteniptv.R
import com.stitten.stitteniptv.data.CategoriesManager
import com.stitten.stitteniptv.data.ContentRepository
import com.stitten.stitteniptv.data.PrefsManager
import com.stitten.stitteniptv.ui.components.tvFocusable
import com.stitten.stitteniptv.ui.theme.AppIcons
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

@Composable
fun ParentalControlScreen(navController: NavHostController) {
    val ctx = LocalContext.current
    val prefs = remember { PrefsManager(ctx) }

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

    // ─── شاشة القفل ───
    if (!unlocked) {
        PinUnlockScreen(
            enteredPin = enteredPin,
            onPinChange = { enteredPin = it },
            error = error,
            onUnlock = {
                if (enteredPin == prefs.parentalPin) {
                    unlocked = true
                    error = ""
                } else {
                    error = "PIN غير صحيح"
                }
            },
            onBack = { navController.popBackStack() },
        )
        return
    }

    // ─── الشاشة الرئيسية ───
    ParentalMainContent(
        prefs = prefs,
        manager = manager,
        onBack = { navController.popBackStack() },
    )
}

// ═══════════════════════════════════════════════════════════════
//  شاشة إدخال PIN للفتح
// ═══════════════════════════════════════════════════════════════

@Composable
private fun PinUnlockScreen(
    enteredPin: String,
    onPinChange: (String) -> Unit,
    error: String,
    onUnlock: () -> Unit,
    onBack: () -> Unit,
) {
    var showPin by remember { mutableStateOf(false) }

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

        // زر الرجوع
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(24.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "رجوع",
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // أيقونة القفل
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color(0xFF1F6FEB).copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFF1F6FEB),
                    modifier = Modifier.size(56.dp),
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "الرقابة الأبوية",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "أدخل PIN لفتح الشاشة",
                fontSize = 16.sp,
                color = Color(0xFF8B949E)
            )

            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value = enteredPin,
                onValueChange = {
                    onPinChange(it.filter { c -> c.isDigit() }.take(6))
                },
                label = { Text("PIN") },
                visualTransformation = if (showPin)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
                trailingIcon = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .tvFocusable(onClick = { showPin = !showPin }),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (showPin) Icons.Default.VisibilityOff
                            else Icons.Default.Visibility,
                            contentDescription = if (showPin) "إخفاء" else "إظهار",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                },
                modifier = Modifier.width(400.dp),
                singleLine = true
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = onUnlock,
                modifier = Modifier
                    .width(400.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1F6FEB)
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text("فتح", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            if (error.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            Color(0xFFDA3633).copy(alpha = 0.2f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "⚠️ $error",
                        color = Color(0xFFFF7B72),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  الشاشة الرئيسية — بعد الفتح
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ParentalMainContent(
    prefs: PrefsManager,
    manager: com.stitten.stitteniptv.data.ParentalControlManager,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var pinField by remember { mutableStateOf(prefs.parentalPin) }

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
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFF1F6FEB),
                    modifier = Modifier.size(32.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "الرقابة الأبوية",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                // عدد المحجوبات
                if (blocks.isNotEmpty()) {
                    Text(
                        text = "${blocks.size} محجوب",
                        color = Color(0xFFDA3633),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {

                // ═══════════════════════════════════════════════════
                //  🔐 إعداد PIN
                // ═══════════════════════════════════════════════════
                item(key = "pin_header") {
                    SectionHeader(
                        icon = Icons.Default.Lock,
                        title = "إعداد PIN",
                    )
                }

                item(key = "pin_field") {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF161B22).copy(alpha = 0.9f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "PIN الحالي (4-6 أرقام)",
                                color = Color(0xFF8B949E),
                                fontSize = 13.sp,
                            )
                            Spacer(Modifier.height(8.dp))

                            OutlinedTextField(
                                value = pinField,
                                onValueChange = { v ->
                                    pinField = v.filter { c -> c.isDigit() }.take(6)
                                    prefs.parentalPin = pinField
                                },
                                label = { Text("PIN") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                            )

                            Spacer(Modifier.height(8.dp))

                            val isPinValid = pinField.length in 4..6
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isPinValid)
                                        Icons.Default.CheckCircle
                                    else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (isPinValid) Color(0xFF66BB6A)
                                    else Color(0xFFFFA726),
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (isPinValid)
                                        "✅ PIN صالح — سيُطلب عند فتح التطبيق"
                                    else if (pinField.isBlank())
                                        "⚠️ لم يُعيَّن PIN بعد — الشاشة غير محمية"
                                    else
                                        "⚠️ PIN قصير جدًا (4 أرقام على الأقل)",
                                    color = if (isPinValid) Color(0xFF66BB6A)
                                    else Color(0xFFFFA726),
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(28.dp)) }

                // ═══════════════════════════════════════════════════
                //  📺 حجب التصنيفات
                // ═══════════════════════════════════════════════════
                item(key = "categories_header") {
                    SectionHeader(
                        icon = AppIcons.LiveTv,
                        title = "حجب التصنيفات",
                        count = allCategories.size,
                    )
                }

                if (allCategories.isEmpty()) {
                    item(key = "no_categories") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لا توجد تصنيفات متاحة للحجب",
                                color = Color(0xFF8B949E),
                                fontSize = 15.sp,
                            )
                        }
                    }
                } else {
                    items(allCategories, key = { "cat_$it" }) { cat ->
                        val blockId = "GROUP:$cat"
                        val isBlocked = blocks.any { it.blockId == blockId }

                        CategoryBlockRow(
                            categoryName = cat,
                            isBlocked = isBlocked,
                            onToggle = { checked ->
                                scope.launch {
                                    if (checked) {
                                        manager.addBlock(blockId, "GROUP", cat)
                                    } else {
                                        val item = blocks.firstOrNull { it.blockId == blockId }
                                        if (item != null) manager.removeBlock(item)
                                    }
                                }
                            },
                        )
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  عنوان قسم
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SectionHeader(
    icon: ImageVector,
    title: String,
    count: Int? = null,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
            if (count != null) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "($count)",
                    color = Color(0xFF8B949E),
                    fontSize = 14.sp,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = Color(0xFF30363D))
        Spacer(Modifier.height(12.dp))
    }
}

// ═══════════════════════════════════════════════════════════════
//  صف تصنيف
// ═══════════════════════════════════════════════════════════════

@Composable
private fun CategoryBlockRow(
    categoryName: String,
    isBlocked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .border(
                width = if (isBlocked) 2.dp else 0.dp,
                color = if (isBlocked) Color(0xFFDA3633).copy(alpha = 0.5f)
                else Color.Transparent,
                shape = RoundedCornerShape(10.dp),
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isBlocked) Color(0xFFDA3633).copy(alpha = 0.15f)
            else Color(0xFF161B22)
        ),
        shape = RoundedCornerShape(10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .tvFocusable { onToggle(!isBlocked) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // أيقونة حالة القفل
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = if (isBlocked) Color(0xFFDA3633) else Color(0xFF6E7681),
                modifier = Modifier.size(20.dp),
            )

            Spacer(Modifier.width(12.dp))

            Text(
                text = categoryName,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = if (isBlocked) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f),
            )

            if (isBlocked) {
                Text(
                    text = "محجوب",
                    color = Color(0xFFDA3633),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.width(12.dp))
            }

            Switch(
                checked = isBlocked,
                onCheckedChange = onToggle,
            )
        }
    }
}
