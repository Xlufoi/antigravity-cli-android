package com.google.antigravity.data.ipc

import android.content.pm.PackageManager
import dev.rikka.shizuku.Shizuku

object ShizukuManager {
    private const val TAG = "ShizukuManager"

    fun isInstalled(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (_: Throwable) {
            false
        }
    }

    fun isPermissionGranted(): Boolean {
        return try {
            if (isInstalled()) {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } else {
                false
            }
        } catch (_: Throwable) {
            false
        }
    }

    fun requestPermission(requestCode: Int = 1001) {
        try {
            if (isInstalled() && !isPermissionGranted()) {
                Shizuku.requestPermission(requestCode)
            }
        } catch (e: Throwable) {
            AppLogger.log(TAG, "Request Shizuku permission error: ${e.message}")
        }
    }

    fun executeAdb(command: String): String {
        return try {
            if (!isPermissionGranted()) {
                return "Ошибка: Доступ Shizuku не предоставлен."
            }
            val proc = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
            val output = proc.inputStream.bufferedReader().readText()
            val error = proc.errorStream.bufferedReader().readText()
            proc.waitFor()
            if (output.isNotBlank()) output.trim() else error.trim()
        } catch (e: Throwable) {
            AppLogger.log(TAG, "Shizuku exec error: ${e.message}")
            "Ошибка Shizuku: ${e.localizedMessage}"
        }
    }
}
