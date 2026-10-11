package com.stitten.stitteniptv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.stitten.stitteniptv.data.EpgParser
import com.stitten.stitteniptv.data.EpgProgram
import com.stitten.stitteniptv.ui.components.tvFocusable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EpgScreen(
    navController: NavHostController,
    channelId: String,
    channelName: String,
    epgUrl: String
) {
    var programs by remember { mutableStateOf<List<EpgProgram>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()

    LaunchedEffect(channelId) {
        loading = true
        error = null

        try {
            if (epgUrl.isBlank()) {
                programs = emptyList()
            } else {
                // ✅ التحميل على خيط IO — لا يجمّد الواجهة
                val all = withContext(Dispatchers.IO) {
                    EpgParser.loadFromUrl(epgUrl)
                }
                programs = all
                    .filter { it.channelId == channelId }
                    .sortedBy { it.startTime }

                // ✅ التمرير التلقائي إلى البرنامج الحالي
                val nowIndex = programs.indexOfFirst { it.isNow() }
                if (nowIndex > 0) {
                    try {
                        listState.scrollToItem(nowIndex)
                    } catch (_: Exception) { }
                }
            }
        } catch (e: Exception) {
            error = e.message ?: "تعذّر تحميل دليل البرامج"
        }

        loading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        // ─── الشريط العلوي ───
        EpgTopBar(
            channelName = channelName,
            count = programs.size,
            onBack = { navController.popBackStack() },
        )

        when {
            loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF1F6FEB))
                        Spacer(Modifier.height(16.dp))
                        Text("جارٍ تحميل دليل البرامج...", color = Color.White, fontSize = 16.sp)
                    }
                }
            }

            error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⚠️", fontSize = 64.sp)
                        Spacer(Modifier.height(16.dp))
                        Text(error!!, color = Color(0xFFF85149), fontSize = 16.sp)
                    }
                }
            }

            programs.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📅", fontSize = 64.sp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "لا تتوفر بيانات EPG لهذه القناة",
                            color = Color(0xFF8B949E),
                            fontSize = 18.sp,
                        )
                    }
                }
            }

            else -> {
                val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(programs, key = { "${it.channelId}_${it.startTime}" }) { p ->
                        ProgramCard(program = p, fmt = fmt)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  الشريط العلوي
// ═══════════════════════════════════════════════════════════════

@Composable
private fun EpgTopBar(
    channelName: String,
    count: Int,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // زر الرجوع
        Box(
            modifier = Modifier
                .tvFocusable(onClick = onBack)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF30363D))
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Text("← رجوع", color = Color.White, fontSize = 15.sp)
        }

        Spacer(Modifier.width(20.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "📅 دليل البرامج",
                color = Color(0xFF1F6FEB),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = channelName,
                color = Color.White,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (count > 0) {
            Text(
                text = "$count برنامج",
                color = Color(0xFF8B949E),
                fontSize = 14.sp,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  بطاقة برنامج
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ProgramCard(
    program: EpgProgram,
    fmt: SimpleDateFormat,
) {
    val isNow = program.isNow()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusable(onClick = { }),
        colors = CardDefaults.cardColors(
            containerColor = if (isNow) Color(0xFF1F6FEB) else Color(0xFF161B22),
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // ─── العمود الأيسر: الوقت ───
            Column(
                modifier = Modifier.width(80.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = fmt.format(Date(program.startTime)),
                    color = if (isNow) Color.White else Color(0xFF1F6FEB),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = fmt.format(Date(program.endTime)),
                    color = if (isNow) Color.White.copy(alpha = 0.8f) else Color(0xFF8B949E),
                    fontSize = 14.sp,
                )
            }

            Spacer(Modifier.width(16.dp))

            // ─── العمود الأيمن: العنوان والوصف ───
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = program.title,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )

                    if (isNow) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = Color(0xFFDA3633),
                            shape = RoundedCornerShape(6.dp),
                        ) {
                            Text(
                                "الآن",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                }

                if (program.description.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = program.description,
                        color = if (isNow) Color.White.copy(alpha = 0.9f)
                        else Color(0xFFA8B3C1),
                        fontSize = 13.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
