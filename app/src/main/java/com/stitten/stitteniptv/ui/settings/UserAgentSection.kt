package com.stitten.stitteniptv.ui.settings

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stitten.stitteniptv.data.network.UserAgentProvider
import kotlinx.coroutines.launch

@Composable
fun UserAgentSection(context: Context) {
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf(UserAgentProvider.DEFAULT_UA) }

    LaunchedEffect(Unit) {
        selected = UserAgentProvider.get(context)
    }

    Column(Modifier.padding(16.dp)) {
        Text("User-Agent", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        UserAgentProvider.PRESETS.forEach { ua ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        selected = ua
                        scope.launch { UserAgentProvider.set(context, ua) }
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = (selected == ua),
                    onClick = {
                        selected = ua
                        scope.launch { UserAgentProvider.set(context, ua) }
                    }
                )
                Text(ua, modifier = Modifier.padding(start = 8.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "غيّر القيمة إذا فشل الاتصال بالمزود.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
