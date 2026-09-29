package com.stitten.stitteniptv.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat

data class ExternalPlayer(
    val name: String,
    val packageName: String,
    val icon: String
)

object ExternalPlayerManager {

    val KNOWN_PLAYERS = listOf(
        ExternalPlayer("VLC", "org.videolan.vlc", "🟠"),
        ExternalPlayer("MX Player", "com.mxtech.videoplayer.ad", "🔵"),
        ExternalPlayer("MX Player Pro", "com.mxtech.videoplayer.pro", "🔷"),
        ExternalPlayer("MPV", "is.xyz.mpv", "⚫"),
        ExternalPlayer("Kodi", "org.xbmc.kodi", "🎬"),
        ExternalPlayer("Just Player", "com.brouken.player", "▶️"),
        ExternalPlayer("Vimu Player", "net.gtvbox.videoplayer", "🟣"),
        ExternalPlayer("nPlayer", "com.newin.nplayer.pro", "🔴"),
        ExternalPlayer("PlayerX", "com.videoplayer.playerx", "🟢")
    )

    fun getInstalledPlayers(ctx: Context): List<ExternalPlayer> {
        val pm = ctx.packageManager
        return KNOWN_PLAYERS.filter { player ->
            try {
                pm.getPackageInfo(player.packageName, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    fun launchInPackage(ctx: Context, url: String, packageName: String): Boolean {
        return try {
            val uri = Uri.parse(url)
            val mimeType = guessMimeType(url)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                setPackage(packageName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("headers", arrayOf("User-Agent: ${HttpClientProvider.getUserAgent()}"))
            }
            ContextCompat.startActivity(ctx, intent, null)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun launchWithChooser(ctx: Context, url: String) {
        try {
            val uri = Uri.parse(url)
            val mimeType = guessMimeType(url)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "اختر مشغلاً").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(chooser)
        } catch (e: Exception) { }
    }

    private fun guessMimeType(url: String): String {
        val lower = url.lowercase()
        return when {
            lower.contains(".m3u8") -> "application/x-mpegURL"
            lower.contains(".mpd") -> "application/dash+xml"
            lower.contains(".mkv") -> "video/x-matroska"
            lower.contains(".mp4") -> "video/mp4"
            lower.contains(".ts") -> "video/mp2t"
            lower.contains(".avi") -> "video/x-msvideo"
            lower.contains(".mov") -> "video/quicktime"
            lower.contains(".webm") -> "video/webm"
            else -> "video/*"
        }
    }
}
