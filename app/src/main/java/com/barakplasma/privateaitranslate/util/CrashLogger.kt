package com.barakplasma.privateaitranslate.util

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashLogger {
    private const val TAG = "CrashLogger"
    private const val NTFY_TOPIC = "privateaitranslate-crash"
    private const val NTFY_URL = "https://ntfy.sh/$NTFY_TOPIC"
    private const val LOG_FILE = "crashlog.txt"
    private const val MAX_LOG_SIZE = 256 * 1024

    private lateinit var logFile: File
    private lateinit var appContext: Context
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        logFile = File(appContext.filesDir, LOG_FILE)

        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val sw = StringWriter()
            PrintWriter(sw).use { pw ->
                pw.println("=== CRASH ${isoNow()} ===")
                pw.println("Thread: ${thread.name}")
                pw.println("Device: ${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})")
                pw.println("App: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                pw.println()
                throwable.printStackTrace(pw)
                pw.println()
            }
            val crashLog = sw.toString()
            appendLog(crashLog)
            postToNtfy("CRASH", crashLog, highPriority = true)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    fun e(tag: String, msg: String, throwable: Throwable? = null) {
        Log.e(tag, msg, throwable)
        val entry = buildEntry("E", tag, msg, throwable)
        appendLog(entry)
    }

    fun w(tag: String, msg: String, throwable: Throwable? = null) {
        Log.w(tag, msg, throwable)
        val entry = buildEntry("W", tag, msg, throwable)
        appendLog(entry)
    }

    fun i(tag: String, msg: String) {
        Log.i(tag, msg)
        val entry = buildEntry("I", tag, msg)
        appendLog(entry)
    }

    fun readLog(): String {
        return try {
            if (logFile.exists()) logFile.readText() else "(no logs)"
        } catch (e: Exception) {
            "(error reading log: ${e.message})"
        }
    }

    fun clearLog() {
        try {
            logFile.delete()
        } catch (_: Exception) {}
    }

    fun sendLogsToNtfy() {
        val logs = readLog()
        if (logs.isBlank() || logs == "(no logs)") return
        Thread {
            postToNtfy("LOGS", logs, highPriority = false)
        }.start()
    }

    private fun buildEntry(level: String, tag: String, msg: String, throwable: Throwable? = null): String {
        val sb = StringBuilder()
        sb.appendLine("${isoNow()} $level/$tag: $msg")
        if (throwable != null) {
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            sb.appendLine(sw.toString())
        }
        return sb.toString()
    }

    private fun appendLog(entry: String) {
        try {
            if (logFile.exists() && logFile.length() > MAX_LOG_SIZE) {
                val content = logFile.readText()
                val keep = content.takeLast(MAX_LOG_SIZE / 2)
                logFile.writeText(keep)
            }
            logFile.appendText(entry)
        } catch (_: Exception) {}
    }

    private fun postToNtfy(title: String, body: String, highPriority: Boolean) {
        try {
            val conn = URL(NTFY_URL).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Title", "PrivateAI $title")
            conn.setRequestProperty("Tags", if (highPriority) "rotating_light" else "memo")
            if (highPriority) {
                conn.setRequestProperty("Priority", "high")
            }
            conn.setRequestProperty("Click", "https://github.com/barakplasma/TranslateYou")
            val payload = if (body.length > 4000) body.take(4000) + "\n...truncated" else body
            conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            conn.responseCode
            conn.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post to ntfy: ${e.message}")
        }
    }

    private fun isoNow(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
    }
}
