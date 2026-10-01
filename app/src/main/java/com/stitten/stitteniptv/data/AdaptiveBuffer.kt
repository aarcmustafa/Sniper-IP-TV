package com.stitten.stitteniptv.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object AdaptiveBuffer {

    /**
     * Buffer تكيفي حسب سرعة الشبكة والوضع
     */
    fun getMinBufferMs(context: Context, liteMode: Boolean): Int {
        if (liteMode) return 30_000

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? ConnectivityManager ?: return 45_000

        val network = cm.activeNetwork ?: return 45_000
        val caps = cm.getNetworkCapabilities(network) ?: return 45_000

        // Ethernet (TV سلكي) — أسرع
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
            return 30_000
        }

        // WiFi — حسب السرعة
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            val downKbps = caps.linkDownstreamBandwidthKbps
            return when {
                downKbps >= 20_000 -> 30_000  // 20+ Mbps
                downKbps >= 10_000 -> 45_000  // 10-20 Mbps
                downKbps >= 5_000 -> 60_000   // 5-10 Mbps
                else -> 90_000                 // أقل
            }
        }

        // Cellular أو VPN
        return 90_000
    }

    fun getMaxBufferMs(context: Context, liteMode: Boolean): Int {
        return getMinBufferMs(context, liteMode) * 3
    }

    /**
     * حجم Buffer الأدنى لبدء التشغيل (أصغر = بدء أسرع)
     */
    fun getPlaybackBufferMs(liteMode: Boolean): Int {
        return if (liteMode) 2500 else 1500
    }

    /**
     * Buffer بعد التقطع
     */
    fun getRebufferBufferMs(liteMode: Boolean): Int {
        return if (liteMode) 5000 else 3000
    }

    /**
     * الحد الأقصى لحجم Buffer بالبايت
     */
    fun getTargetBufferBytes(liteMode: Boolean): Int {
        return if (liteMode) {
            20 * 1024 * 1024  // 20 MB
        } else {
            80 * 1024 * 1024  // 80 MB
        }
    }

    /**
     * Back Buffer (لتحريك للخلف)
     */
    fun getBackBufferMs(liteMode: Boolean): Int {
        return if (liteMode) 15_000 else 60_000
    }
}
