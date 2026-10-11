package com.stitten.stitteniptv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stitten.stitteniptv.data.HttpClientProvider
import com.stitten.stitteniptv.data.PrefsManager
import com.stitten.stitteniptv.ui.components.tvFocusable

@Composable
fun UserAgentSection(prefs: PrefsManager) {
    var selected by remember { mutableStateOf(prefs.getUserAgent()) }

    LaunchedEffect(Unit) {
        selected = prefs.getUserAgent()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = "🌐 User-Agent",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "غيّر القيمة إذا فشل الاتصال بالمزود أو توقّف البث.",
            color = Color(0xFF8B949E),
            fontSize = 13.sp,
        )

        Spacer(Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            HttpClientProvider.USER_AGENT_PRESETS.forEach { ua ->
                UserAgentOption(
                    label = ua,
                    selected = (selected == ua),
                    onClick = {
                        selected = ua
                        prefs.setUserAgent(ua)
                    },
                )
            }
        }
    }
}

@Composable
private fun UserAgentOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusable(onClick = onClick)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) Color(0xFF1F6FEB).copy(alpha = 0.15f)
                else Color.Transparent
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = Color(0xFF1F6FEB),
                unselectedColor = Color(0xFF8B949E),
            ),
        )

        Spacer(Modifier.padding(horizontal = 6.dp))

        Text(
            text = label,
            color = if (selected) Color.White else Color(0xFFD0D7DE),
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
