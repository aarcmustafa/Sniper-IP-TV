package com.stitten.stitteniptv.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
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
    private const val MAX_LOG_SIZE = 5 * 1024 * 1024L

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var enabled: Boolean = false

    @Volatile
    private var logFile: File? = null

    @Volatile
    private var customUri: Uri? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        initFile(context)
    }

    fun setEnabled(context: Context, value: Boolean) {
        enabled = value
        if (value) initFile(context)
    }

    fun isEnabled(): Boolean = enabled

    fun setCustomFolder(uri: Uri?) {
        customUri = uri
    }

    private fun initFile(context: Context) {
        if (logFile != null && logFile?.exists() == true) return
        try {
            val dir = getDefaultDirectory(context)
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

    // ============== المجلد الافتراضي (بدون إذن) ==============
    private fun getDefaultDirectory(context: Context): File {
        val external = context.getExternalFilesDir(null)
        return if (external != null) {
            File(external, FOLDER_NAME)
        } else {
            File(context.filesDir, FOLDER_NAME)
        }
    }

    fun getLogFile(): File? {
        val ctx = appContext ?: return null
        initFile(ctx)
        return logFile
    }

    fun getLogFilePath(): String = getLogFile()?.absolutePath ?: ""

    fun getLogFileSize(): Long = getLogFile()?.length() ?: 0L

    // ============== الكتابة ==============
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
                val dir = getDefaultDirectory(ctx)
                val file = File(dir, FILE_NAME)
                if (file.exists()) file.delete()
                logFile = null
                initFile(ctx)
            } catch (e: Exception) { }
        }
    }

    // ============== مشاركة السجل ==============
    fun shareLog(context: Context): Boolean {
        val file = getLogFile() ?: return false
        if (!file.exists()) return false
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "STTITEN IP TV - Connection Log")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(intent, "مشاركة السجل").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    // ============== حفظ نسخة إلى URI (USB عبر SAF) ==============
    suspend fun exportTo(context: Context, targetUri: Uri): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val file = getLogFile() ?: return@withContext false
                if (!file.exists()) return@withContext false

                context.contentResolver.openOutputStream(targetUri)?.use { output ->
                    file.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }
                true
            } catch (e: Exception) {
                false
            }
        }

    // ============== قراءة محتوى السجل (للعرض) ==============
    suspend fun readLogContent(): String = withContext(Dispatchers.IO) {
        try {
            val file = getLogFile() ?: return@withContext ""
            if (!file.exists()) return@withContext ""
            file.readText(Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }

    private fun timestamp(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())

    private fun sanitize(url: String): String {
        return url
            .replace(Regex("password=[^&]*"), "password=***")
            .replace(Regex("/live/[^/]+/[^/]+/"), "/live/USER/PASS/")
            .replace(Regex("/movie/[^/]+/[^/]+/"), "/movie/USER/PASS/")
            .replace(Regex("/series/[^/]+/[^/]+/"), "/series/USER/PASS/")
    }
}
