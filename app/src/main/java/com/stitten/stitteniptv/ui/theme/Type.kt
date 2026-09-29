package com.stitten.stitteniptv.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.unit.sp

val TvTypography = Typography().let {
    it.copy(
        displayLarge = it.displayLarge.copy(fontSize = 48.sp),
        headlineLarge = it.headlineLarge.copy(fontSize = 32.sp),
        headlineMedium = it.headlineMedium.copy(fontSize = 28.sp),
        titleLarge = it.titleLarge.copy(fontSize = 24.sp),
        bodyLarge = it.bodyLarge.copy(fontSize = 18.sp),
        bodyMedium = it.bodyMedium.copy(fontSize = 16.sp),
        labelLarge = it.labelLarge.copy(fontSize = 18.sp)
    )
}
