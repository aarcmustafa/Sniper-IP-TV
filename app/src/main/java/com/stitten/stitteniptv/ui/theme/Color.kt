package com.stitten.stitteniptv.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════
//  🎨 STTITEN IP TV — لوحة الألوان الاحترافية
//  مستوحاة من Netflix / Shahid / OSN
// ═══════════════════════════════════════════════════════════

// ─────────────────────────────────────────────────────────────
//  الخلفيات (Layered Surfaces)
// ─────────────────────────────────────────────────────────────

/** الخلفية الأساسية — أسود عميق يبرز البوسترات */
val TvBackground = Color(0xFF08090C)

/** السطح الأول — للبطاقات العامة */
val TvSurface = Color(0xFF12141A)

/** السطح المرتفع — للبطاقات المهمة */
val TvSurfaceElevated = Color(0xFF1A1D26)

/** السطح العالي جدًا — للعناصر المنبثقة */
val TvSurfaceHigh = Color(0xFF242833)

// ─────────────────────────────────────────────────────────────
//  العلامة التجارية (Brand)
// ─────────────────────────────────────────────────────────────

/** اللون الأساسي — أزرق ملكي أنيق */
val TvPrimary = Color(0xFF3B82F6)

/** نسخة داكنة للأزرار المضغوطة */
val TvPrimaryDark = Color(0xFF2563EB)

/** نسخة فاتحة للتأثيرات */
val TvPrimaryLight = Color(0xFF60A5FA)

// ─────────────────────────────────────────────────────────────
//  الألوان المميزة (Accents)
// ─────────────────────────────────────────────────────────────

/** ذهبي فاخر — للعناصر المميزة */
val TvGold = Color(0xFFF5B301)

/** أخضر نجاح — للتأكيدات */
val TvSuccess = Color(0xFF22C55E)

/** أحمر خطأ — للأخطاء */
val TvError = Color(0xFFEF4444)

/** أحمر LIVE — لمؤشر البث المباشر */
val TvLive = Color(0xFFE50914)

/** برتقالي تحذير — للتنبيهات */
val TvWarning = Color(0xFFF59E0B)

// ─────────────────────────────────────────────────────────────
//  النصوص (Text)
// ─────────────────────────────────────────────────────────────

/** نص أساسي — أبيض نقي */
val TvText = Color(0xFFFFFFFF)

/** نص ثانوي — رمادي فاتح */
val TvTextSecondary = Color(0xFFA0A6B0)

/** نص باهت — للتفاصيل الصغيرة */
val TvTextTertiary = Color(0xFF6B7280)

/** نص معطّل */
val TvTextDisabled = Color(0xFF4B5563)

// ─────────────────────────────────────────────────────────────
//  التركيز (Focus) — مهم جدًا لـ Android TV
// ─────────────────────────────────────────────────────────────

/** إطار التركيز — أزرق سماوي يبرز على كل الخلفيات */
val TvFocus = Color(0xFF58A6FF)

/** توهج التركيز (Glow) */
val TvFocusGlow = Color(0x6658A6FF)

// ─────────────────────────────────────────────────────────────
//  الحدود والفواصل
// ─────────────────────────────────────────────────────────────

/** حدود خفيفة */
val TvBorder = Color(0xFF2A2E38)

/** حدود نشطة */
val TvBorderActive = Color(0xFF3B82F6)

// ─────────────────────────────────────────────────────────────
//  Gradients
// ─────────────────────────────────────────────────────────────

val TvGradientBottom = Color(0x00000000)
val TvGradientTop = Color(0xE6000000)
val TvScrim = Color(0x99000000)

// ─────────────────────────────────────────────────────────────
//  ألوان إضافية
// ─────────────────────────────────────────────────────────────

/** لون خلفية Skeleton loading */
val TvSkeleton = Color(0xFF1F2330)

/** لون تمييز Skeleton (Shimmer) */
val TvSkeletonHighlight = Color(0xFF2A2F3D)

/** ألوان التصنيفات */
val TvChipSelected = Color(0xFF3B82F6)
val TvChipUnselected = Color(0xFF1A1D26)

// ═══════════════════════════════════════════════════════════
//  🔄 Aliases للتوافق مع الكود القديم
//  (يمكن حذفها لاحقًا بعد تحديث كل الشاشات)
// ═══════════════════════════════════════════════════════════

val BackgroundDark = TvBackground
val SurfaceDark = TvSurface
val SurfaceElevated = TvSurfaceElevated
val SurfaceHighlight = TvSurfaceHigh

val PrimaryAccent = TvPrimary
val SecondaryAccent = TvPrimaryLight
val FocusBorder = TvFocus
val GreenAccent = TvSuccess
val RedAccent = TvError

val TextPrimary = TvText
val TextSecondary = TvTextSecondary
val TextMuted = TvTextTertiary
val Divider = TvBorder
