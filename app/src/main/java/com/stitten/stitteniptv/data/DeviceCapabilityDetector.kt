package com.stitten.stitteniptv.data

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs

data class DeviceCapability(
    val totalRamMb: Long,
    val availableRamMb: Long,
    val totalStorageMb: Long,
    val availableStorageMb: Long,
    val cpuCores: Int,
    val androidVersion: Int,
    val isLowEnd: Boolean,
    val reason: String
)

object DeviceCapabilityDetector {

    private const val LOW_RAM_THRESHOLD_MB = 1800L
    private const val LOW_STORAGE_THRESHOLD_MB = 2500L
    private const val MIN_CPU_CORES = 4
    private const val MIN_ANDROID_VERSION = 24

    fun detect(context: Context): DeviceCapability {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)

        val totalRamMb = memInfo.totalMem / (1024 * 1024)
        val availableRamMb = memInfo.availMem / (1024 * 1024)

        val stat = StatFs(Environment.getDataDirectory().path)
        val totalStorageMb = (stat.blockCountLong * stat.blockSizeLong) / (1024 * 1024)
        val availableStorageMb = (stat.availableBlocksLong * stat.blockSizeLong) / (1024 * 1024)

        val cpuCores = Runtime.getRuntime().availableProcessors()
        val androidVersion = Build.VERSION.SDK_INT

        val reasons = mutableListOf<String>()

        if (totalRamMb < LOW_RAM_THRESHOLD_MB) {
            reasons.add("ذاكرة منخفضة ($totalRamMb MB)")
        }
        if (availableStorageMb < LOW_STORAGE_THRESHOLD_MB) {
            reasons.add("تخزين محدود ($availableStorageMb MB متاح)")
        }
        if (cpuCores < MIN_CPU_CORES) {
            reasons.add("معالج بـ $cpuCores أنوية فقط")
        }
        if (androidVersion < MIN_ANDROID_VERSION) {
            reasons.add("Android ${androidVersion} قديم")
        }

        val isLowEnd = reasons.isNotEmpty()

        return DeviceCapability(
            totalRamMb = totalRamMb,
            availableRamMb = availableRamMb,
            totalStorageMb = totalStorageMb,
            availableStorageMb = availableStorageMb,
            cpuCores = cpuCores,
            androidVersion = androidVersion,
            isLowEnd = isLowEnd,
            reason = if (isLowEnd) reasons.joinToString(" • ") else "جهاز قوي"
        )
    }
}
