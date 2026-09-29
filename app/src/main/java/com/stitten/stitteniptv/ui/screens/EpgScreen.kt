package com.stitten.stitteniptv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.stitten.stitteniptv.data.EpgParser
import com.stitten.stitteniptv.data.EpgProgram
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

    LaunchedEffect(channelId) {
        loading = true
        if (epgUrl.isNotBlank()) {
            val all = EpgParser.loadFromUrl(epgUrl)
            programs = all.filter { it.channelId == channelId }
                .sortedBy { it.startTime }
        }
        loading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Text(
            "دليل البرامج - $channelName",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (programs.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "لا تتوفر بيانات EPG لهذه القناة",
                    color = Color.Gray,
                    fontSize = 18.sp
                )
            }
        } else {
            val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(programs) { p ->
                    val isNow = p.isNow()
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isNow) Color(0xFF1F6FEB)
                            else Color(0xFF161B22)
                        )
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    fmt.format(Date(p.startTime)) + " - " +
                                        fmt.format(Date(p.endTime)),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isNow) {
                                    Spacer(Modifier.width(8.dp))
                                    Surface(
                                        color = Color(0xFFDA3633),
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Text(
                                            "الآن",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(
                                                horizontal = 6.dp, vertical = 2.dp
                                            )
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                p.title,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (p.description.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    p.description,
                                    color = Color.LightGray,
                                    fontSize = 14.sp,
                                    maxLines = 3
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
