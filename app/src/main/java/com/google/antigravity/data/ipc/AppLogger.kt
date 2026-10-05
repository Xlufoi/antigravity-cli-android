package com.google.antigravity.data.ipc

import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {
    private const val TAG = "AppLogger"
    private val logLines = mutableListOf<String>()
    private val _liveLogs = MutableStateFlow<List<String>>(emptyList())
    val liveLogs: StateFlow<List<String>> = _liveLogs.asStateFlow()

    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    fun log(tag: String, message: String) {
        val timestamp = timeFormat.format(Date())
        val formatted = "[$timestamp] [$tag] $message"
        Log.i(tag, message)
        synchronized(logLines) {
            logLines.add(formatted)
            if (logLines.size > 1000) logLines.removeAt(0)
            _liveLogs.value = logLines.toList()
        }
    }

    fun exportToDownloads(context: Context): String {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val logFile = File(downloadsDir, "antigravity_debug.log")
            FileWriter(logFile, false).use { writer ->
                synchronized(logLines) {
                    for (line in logLines) {
                        writer.write(line + "\n")
                    }
                }
            }
            log(TAG, "Logs exported to ${logFile.absolutePath}")
            logFile.absolutePath
        } catch (e: Exception) {
            val fallbackFile = File(context.filesDir, "antigravity_debug.log")
            fallbackFile.writeText(logLines.joinToString("\n"))
            log(TAG, "Export to Downloads failed, saved to internal: ${fallbackFile.absolutePath}")
            fallbackFile.absolutePath
        }
    }
}
