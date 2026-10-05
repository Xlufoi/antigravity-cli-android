package com.google.antigravity.data.ipc

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import java.io.InputStream

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
            val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            newProcessMethod.isAccessible = true
            val proc = newProcessMethod.invoke(null, arrayOf("sh", "-c", command), null, null) as java.lang.Process
            val output = proc.inputStream.bufferedReader().use { it.readText() }
            val error = proc.errorStream.bufferedReader().use { it.readText() }
            proc.waitFor()
            if (output.isNotBlank()) output.trim() else error.trim()
        } catch (e: Throwable) {
            AppLogger.log(TAG, "Shizuku exec error: ${e.message}")
            "Ошибка Shizuku: ${e.localizedMessage}"
        }
    }
}
