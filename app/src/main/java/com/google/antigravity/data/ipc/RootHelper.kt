package com.google.antigravity.data.ipc

import java.io.File

object RootHelper {
    private const val TAG = "RootHelper"

    fun isRootAvailable(): Boolean {
        val suPaths = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/su/bin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su"
        )
        if (suPaths.any { File(it).exists() }) return true

        return try {
            val proc = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val line = proc.inputStream.bufferedReader().readLine()
            proc.waitFor()
            !line.isNullOrBlank()
        } catch (_: Exception) {
            false
        }
    }

    fun isRootGranted(): Boolean {
        return try {
            val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val output = proc.inputStream.bufferedReader().readLine()
            proc.waitFor()
            output?.contains("uid=0") == true
        } catch (_: Exception) {
            false
        }
    }

    fun executeRootCommand(command: String): String {
        return try {
            val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val output = proc.inputStream.bufferedReader().readText()
            val error = proc.errorStream.bufferedReader().readText()
            proc.waitFor()
            if (output.isNotBlank()) output.trim() else error.trim()
        } catch (e: Exception) {
            AppLogger.log(TAG, "Root exec error: ${e.message}")
            "Root error: ${e.localizedMessage}"
        }
    }
}
