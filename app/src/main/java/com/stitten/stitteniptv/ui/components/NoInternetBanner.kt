package com.stitten.stitteniptv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NoInternetBanner(
    message: String = "⚠️ لا يوجد اتصال بالإنترنت"
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(0xFFDA3633).copy(alpha = 0.95f),
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("📡", fontSize = 20.sp)
            Spacer(Modifier.width(10.dp))
            Text(
                message,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SlowInternetBanner(
    speedKbps: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(0xFFFFA726).copy(alpha = 0.95f),
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🐢", fontSize = 20.sp)
            Spacer(Modifier.width(10.dp))
            Text(
                "اتصال بطيء ($speedKbps kbps) — قد تتأثر جودة البث",
                color = Color.Black,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
