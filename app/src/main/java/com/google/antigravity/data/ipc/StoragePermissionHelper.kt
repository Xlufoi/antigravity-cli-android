package com.google.antigravity.data.ipc

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings

object StoragePermissionHelper {
    private const val TAG = "StoragePermissionHelper"

    fun hasStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    fun requestStoragePermission(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        } catch (_: Exception) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                AppLogger.log(TAG, "Failed to launch storage settings: ${e.message}")
            }
        }
    }

    fun tryGrantStorageViaRoot(context: Context): Boolean {
        if (!RootHelper.isRootAvailable()) return false
        return try {
            val pkg = context.packageName
            val cmd = "appops set $pkg MANAGE_EXTERNAL_STORAGE allow; pm grant $pkg android.permission.READ_EXTERNAL_STORAGE 2>/dev/null; pm grant $pkg android.permission.WRITE_EXTERNAL_STORAGE 2>/dev/null; pm grant $pkg android.permission.READ_MEDIA_AUDIO 2>/dev/null; pm grant $pkg android.permission.READ_MEDIA_VIDEO 2>/dev/null; pm grant $pkg android.permission.READ_MEDIA_IMAGES 2>/dev/null"
            val output = RootHelper.executeRootCommand(cmd)
            val granted = hasStoragePermission(context)
            AppLogger.log(TAG, "Grant storage via root: granted=$granted (output=$output)")
            granted
        } catch (e: Exception) {
            AppLogger.log(TAG, "Error granting storage via root: ${e.message}")
            false
        }
    }
}
