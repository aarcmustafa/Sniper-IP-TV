package com.stitten.stitteniptv.data

import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object XtreamLogger {

    private const val FOLDER_NAME = "STTITEN IP TV"
    private const val FILE_NAME = "connection_log.txt"
    private const val MAX_LOG_SIZE = 5 * 1024 * 1024L // 5 MB

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var enabled: Boolean = false

    @Volatile
    private var logFile: File? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun setEnabled(context: Context, value: Boolean) {
        enabled = value
        if (value) initFile(context)
    }

    fun isEnabled(): Boolean = enabled

    private fun initFile(context: Context) {
        if (logFile != null && logFile?.exists() == true) return
        try {
            val dir = getLogDirectory(context)
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, FILE_NAME)
            if (file.exists() && file.length() > MAX_LOG_SIZE) {
                file.delete()
            }
            logFile = file
        } catch (e: Exception) {
            logFile = null
        }
    }

    private fun getLogDirectory(context: Context): File {
        val external = context.getExternalFilesDir(null)
        return if (external != null) {
            File(external, FOLDER_NAME)
        } else {
            File(context.filesDir, FOLDER_NAME)
        }
    }

    fun getLogFilePath(): String {
        val ctx = appContext ?: return ""
        val dir = getLogDirectory(ctx)
        return File(dir, FILE_NAME).absolutePath
    }

    suspend fun logHeader(appVersion: String) {
        val ctx = appContext ?: return
        if (!enabled) return
        withContext(Dispatchers.IO) {
            try {
                initFile(ctx)
                val file = logFile ?: return@withContext
                val header = buildString {
                    appendLine("=".repeat(60))
                    appendLine("STTITEN IP TV - Connection Log")
                    appendLine("App Version: $appVersion")
                    appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
                    appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                    appendLine("Session Started: ${timestamp()}")
                    appendLine("=".repeat(60))
                    appendLine()
                }
                FileOutputStream(file, true).use {
                    it.write(header.toByteArray(Charsets.UTF_8))
                }
            } catch (e: Exception) { }
        }
    }

    suspend fun log(tag: String, message: String) {
        val ctx = appContext ?: return
        if (!enabled) return
        withContext(Dispatchers.IO) {
            try {
                initFile(ctx)
                val file = logFile ?: return@withContext
                val line = "[${timestamp()}] [$tag] $message\n"
                FileOutputStream(file, true).use {
                    it.write(line.toByteArray(Charsets.UTF_8))
                }
            } catch (e: Exception) { }
        }
    }

    suspend fun logRequestStart(url: String, tag: String) {
        log(tag, "→ REQUEST: ${sanitize(url)}")
    }

    suspend fun logRequestSuccess(url: String, tag: String, code: Int, elapsedMs: Long, bodyLength: Int) {
        log(tag, "✅ SUCCESS [$code] time=${elapsedMs}ms size=${bodyLength}B url=${sanitize(url)}")
    }

    suspend fun logRequestFailure(url: String, tag: String, error: String, elapsedMs: Long) {
        log(tag, "❌ FAILED after ${elapsedMs}ms url=${sanitize(url)} error=$error")
    }

    suspend fun logException(tag: String, operation: String, exception: Throwable) {
        log(tag, "❌ EXCEPTION during $operation: ${exception.javaClass.simpleName}: ${exception.message ?: "no message"}")
    }

    suspend fun clearLog() {
        val ctx = appContext ?: return
        withContext(Dispatchers.IO) {
            try {
                val dir = getLogDirectory(ctx)
                val file = File(dir, FILE_NAME)
                if (file.exists()) file.delete()
                logFile = null
            } catch (e: Exception) { }
        }
    }

    private fun timestamp(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())

    private fun sanitize(url: String): String {
        // إخفاء كلمة المرور من الرابط
        return url
            .replace(Regex("password=[^&]*"), "password=***")
            .replace(Regex("/live/[^/]+/[^/]+/"), "/live/USER/PASS/")
            .replace(Regex("/movie/[^/]+/[^/]+/"), "/movie/USER/PASS/")
            .replace(Regex("/series/[^/]+/[^/]+/"), "/series/USER/PASS/")
    }
}
