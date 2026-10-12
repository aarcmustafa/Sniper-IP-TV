# ═══════════════════════════════════════════════════════════
#  LibVLC - قواعد ضرورية لمنع تشويش وحذف أصناف VLC
# ═══════════════════════════════════════════════════════════
-keep class org.videolan.** { *; }
-keep class org.videolan.libvlc.** { *; }
-keep class org.videolan.libvlc.util.** { *; }
-keep class org.videolan.libvlc.interfaces.** { *; }
-dontwarn org.videolan.**

# الحفاظ على الدوال الأصلية (Native Methods) - مهمة جدًا لـ JNI
-keepclasseswithmembernames class * {
    native <methods>;
}

# ═══════════════════════════════════════════════════════════
#  Hilt
# ═══════════════════════════════════════════════════════════
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper
-keepclasseswithmembers class * { @dagger.hilt.* <methods>; }

# ═══════════════════════════════════════════════════════════
#  Room
# ═══════════════════════════════════════════════════════════
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }

# ═══════════════════════════════════════════════════════════
#  Media3
# ═══════════════════════════════════════════════════════════
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ═══════════════════════════════════════════════════════════
#  الشبكة
# ═══════════════════════════════════════════════════════════
-dontwarn okhttp3.**
-dontwarn okio.**

# ═══════════════════════════════════════════════════════════
#  Compose
# ═══════════════════════════════════════════════════════════
-keep class androidx.compose.** { *; }

# ═══════════════════════════════════════════════════════════
#  أصناف التطبيق
# ═══════════════════════════════════════════════════════════
-keep class com.stitten.stitteniptv.data.** { *; }
-keep class com.stitten.stitteniptv.database.entity.** { *; }

# ═══════════════════════════════════════════════════════════
#  Kotlin / Coroutines
# ═══════════════════════════════════════════════════════════
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }

# ═══════════════════════════════════════════════════════════
#  Kotlin Metadata (مهم لمنع أخطاء Reflection)
# ═══════════════════════════════════════════════════════════
-keep class kotlin.Metadata { *; }
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
