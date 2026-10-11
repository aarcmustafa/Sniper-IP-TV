package com.stitten.stitteniptv.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.stitten.stitteniptv.R
import com.stitten.stitteniptv.ui.components.tvFocusable
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ErrorLogScreen(navController: NavHostController) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val entryPoint = remember {
        EntryPointAccessors.fromApplication(
            ctx.applicationContext,
            PlayerEntryPoint::class.java
        )
    }
    val errorLogger = remember { entryPoint.errorLogger() }
    val logs by errorLogger.getAll().collectAsState(initial = emptyList())

    var confirmClear by remember { mutableStateOf(false) }
    var copySuccess by remember { mutableStateOf(false) }

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
            ErrorsTopBar(
                totalCount = logs.size,
                onBack = { navController.popBackStack() },
            )

            Spacer(Modifier.height(20.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {

                // ═══════════════════════════════════════════════════
                //  🛠️ أدوات
                // ═══════════════════════════════════════════════════
                item(key = "tools_header") {
                    SectionHeader(
                        icon = Icons.Default.Info,
                        title = "أدوات",
                    )
                }

                item(key = "tools_buttons") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // ─── نسخ السجل ───
                        Button(
                            onClick = {
                                val text = logs.joinToString("\n\n") { log ->
                                    val fmt = SimpleDateFormat(
                                        "yyyy-MM-dd HH:mm:ss",
                                        Locale.getDefault()
                                    )
                                    "🕐 ${fmt.format(Date(log.timestamp))}\n" +
                                            "🔗 ${log.url}\n" +
                                            "⚠️ ${log.errorMessage}"
                                }
                                val clipboard = ctx.getSystemService(
                                    Context.CLIPBOARD_SERVICE
                                ) as ClipboardManager
                                clipboard.setPrimaryClip(
                                    ClipData.newPlainText("STTITEN Error Logs", text)
                                )
                                copySuccess = true
                            },
                            enabled = logs.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1F6FEB),
                                disabledContainerColor = Color(0xFF30363D),
                            ),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("نسخ كل السجل", fontSize = 16.sp, color = Color.White)
                        }

                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "مفيد لمشاركة المشاكل مع الدعم",
                            color = Color(0xFF8B949E),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 8.dp),
                        )

                        if (copySuccess) {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF66BB6A),
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "تم النسخ إلى الحافظة",
                                    color = Color(0xFF66BB6A),
                                    fontSize = 12.sp,
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // ─── مسح السجل ───
                        Button(
                            onClick = { confirmClear = true },
                            enabled = logs.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDA3633).copy(alpha = 0.7f),
                                disabledContainerColor = Color(0xFF30363D),
                            ),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("مسح كل السجل", fontSize = 16.sp, color = Color.White)
                        }

                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "يحذف كل السجلات المسجّلة (لا يمكن التراجع)",
                            color = Color(0xFF8B949E),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }

                item { Spacer(Modifier.height(28.dp)) }

                // ═══════════════════════════════════════════════════
                //  📋 قائمة الأخطاء
                // ═══════════════════════════════════════════════════
                item(key = "logs_header") {
                    SectionHeader(
                        icon = Icons.Default.Lock,
                        title = "الأخطاء المسجّلة",
                        count = logs.size,
                    )
                }

                if (logs.isEmpty()) {
                    item(key = "empty") {
                        EmptyErrorsView()
                    }
                } else {
                    items(logs, key = { "log_${it.id}" }) { log ->
                        ErrorLogCard(log = log)
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    // ─── Dialog تأكيد المسح ───
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("تأكيد المسح") },
            text = { Text("هل تريد مسح جميع السجلات (${logs.size})؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                errorLogger.clear()
                            } catch (_: Exception) { }
                        }
                        confirmClear = false
                    }
                ) {
                    Text("مسح الكل", color = Color(0xFFDA3633))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  الشريط العلوي
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ErrorsTopBar(
    totalCount: Int,
    onBack: () -> Unit,
) {
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
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = Color(0xFFFFA726),
            modifier = Modifier.size(32.dp),
        )
        Spacer(Modifier.width(12.dp))

        Column {
            Text(
                text = "سجل الأخطاء",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (totalCount == 0)
                    "لا توجد أخطاء مسجّلة"
                else
                    "$totalCount خطأ مسجّل",
                color = Color(0xFF8B949E),
                fontSize = 13.sp,
            )
        }

        Spacer(Modifier.weight(1f))

        if (totalCount > 0) {
            Box(
                modifier = Modifier
                    .background(
                        Color(0xFFDA3633).copy(alpha = 0.2f),
                        RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "$totalCount",
                    color = Color(0xFFFF7B72),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  عنوان قسم
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
//  بطاقة خطأ
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ErrorLogCard(
    log: com.stitten.stitteniptv.database.entity.ErrorLogEntity,
) {
    val fmt = remember {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .border(
                width = 1.dp,
                color = Color(0xFFDA3633).copy(alpha = 0.35f),
                shape = RoundedCornerShape(12.dp),
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF161B22).copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // ─── الرأس: التاريخ + أيقونة ───
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = Color(0xFFDA3633),
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = fmt.format(Date(log.timestamp)),
                    color = Color(0xFF8B949E),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(Modifier.height(10.dp))

            // ─── URL ───
            if (log.url.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = Color(0xFF1F6FEB),
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = log.url,
                        color = Color(0xFFD0D7DE),
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            // ─── رسالة الخطأ ───
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color(0xFFDA3633).copy(alpha = 0.12f),
                        RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text = log.errorMessage,
                    color = Color(0xFFFF7B72),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  حالة فارغة
// ═══════════════════════════════════════════════════════════════

@Composable
private fun EmptyErrorsView() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF66BB6A),
                modifier = Modifier.size(72.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "لا توجد أخطاء مسجّلة",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "التطبيق يعمل بشكل جيد ✨",
                color = Color(0xFF8B949E),
                fontSize = 14.sp,
            )
        }
    }
}
