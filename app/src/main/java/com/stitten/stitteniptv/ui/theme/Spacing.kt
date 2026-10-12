package com.stitten.stitteniptv.ui.theme

import androidx.compose.ui.unit.dp

/**
 * نظام مسافات موحّد — يضمن تناسق كل الشاشات.
 * استخدمه بدلاً من الأرقام العشوائية (12.dp, 14.dp, 18.dp...).
 */
object Spacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
    val huge = 48.dp
    val massive = 64.dp

    // ─── مسافات خاصة بـ TV (آمنة من حواف الشاشة) ───
    val screenPadding = 48.dp       // حافة الشاشة
    val screenPaddingCompact = 24.dp
    val rowSpacing = 16.dp          // بين صفوف المحتوى
    val cardSpacing = 12.dp         // بين البطاقات
}

/**
 * أنصاف الأقطار (Corner Radius) — موحّدة لكل التطبيق.
 */
object Radius {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val pill = 999.dp    // أزرار دائرية كاملة
}
