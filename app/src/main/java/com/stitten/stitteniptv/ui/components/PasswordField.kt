package com.stitten.stitteniptv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.stitten.stitteniptv.ui.theme.Radius
import com.stitten.stitteniptv.ui.theme.Spacing
import com.stitten.stitteniptv.ui.theme.TvPrimary
import com.stitten.stitteniptv.ui.theme.TvSurfaceHigh
import com.stitten.stitteniptv.ui.theme.TvText
import com.stitten.stitteniptv.ui.theme.TvTextSecondary
import com.stitten.stitteniptv.ui.theme.TvTextTertiary

/**
 * حقل كلمة سر مع زر كشف **مستقل** (خارج الإطار).
 *
 * ✅ الزر خارج مربع كلمة السر (في Row منفصل).
 * ✅ الريموت لا يتوقف عند الزر (focusProperties canFocus = false).
 * ✅ الزر يعمل باللمس على الأجهزة اللمسية.
 */
@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "كلمة السر",
    placeholder: String = "أدخل كلمة السر",
    enabled: Boolean = true,
) {
    var isVisible by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        // ═══════════════════════════════════════════════════════
        //  حقل كلمة السر
        // ═══════════════════════════════════════════════════════
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            label = {
                Text(
                    text = label,
                    color = TvTextSecondary,
                )
            },
            placeholder = {
                Text(
                    text = placeholder,
                    color = TvTextTertiary,
                )
            },
            singleLine = true,
            enabled = enabled,
            visualTransformation = if (isVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            shape = RoundedCornerShape(Radius.md),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TvText,
                unfocusedTextColor = TvText,
                disabledTextColor = TvTextTertiary,
                focusedBorderColor = TvPrimary,
                unfocusedBorderColor = TvSurfaceHigh,
                disabledBorderColor = TvSurfaceHigh,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                cursorColor = TvPrimary,
            ),
        )

        // ═══════════════════════════════════════════════════════
        //  زر الكشف — مستقل بصريًا
        //  ⚠️ غير قابل للتركيز بالريموت (focusProperties)
        //  ✅ يعمل باللمس
        // ═══════════════════════════════════════════════════════
        Box(
            modifier = Modifier
                // 🔑 السر: الريموت لا يتوقف هنا
                .focusProperties { canFocus = false }
                .size(56.dp)
                .clip(RoundedCornerShape(Radius.md))
                .background(
                    if (isVisible) TvPrimary.copy(alpha = 0.2f)
                    else TvSurfaceHigh
                )
                .clickable(
                    enabled = enabled,
                    onClick = { isVisible = !isVisible },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isVisible) Icons.Default.VisibilityOff
                else Icons.Default.Visibility,
                contentDescription = if (isVisible) "إخفاء كلمة السر" else "كشف كلمة السر",
                tint = if (isVisible) TvPrimary else TvTextSecondary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
