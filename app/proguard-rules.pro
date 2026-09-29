-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper
-keepclasseswithmembers class * { @dagger.hilt.* <methods>; }

-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }

-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

-dontwarn okhttp3.**
-dontwarn okio.**

-keep class androidx.compose.** { *; }

-keep class com.stitten.stitteniptv.data.** { *; }
-keep class com.stitten.stitteniptv.database.entity.** { *; }

-dontwarn kotlinx.coroutines.**
