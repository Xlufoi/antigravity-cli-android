package com.google.antigravity.data.ipc

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.GZIPInputStream

object EngineInstaller {
    private const val TAG = "EngineInstaller"
    private const val BUNDLE_ASSET = "engine-bundle.tar.gz"

    suspend fun ensureInstalled(context: Context): File = withContext(Dispatchers.IO) {
        val engineDir = File(context.filesDir, "engine")
        val agyBinary = File(engineDir, "agy.va39")
        val ldLoader = File(engineDir, "ld-linux-aarch64.so.1")

        if (agyBinary.exists() && ldLoader.exists() && agyBinary.length() > 10_000_000) {
            Log.i(TAG, "Engine already installed in $engineDir (${agyBinary.length()} bytes)")
            return@withContext engineDir
        }

        engineDir.mkdirs()
        Log.i(TAG, "Unpacking engine bundle from assets...")

        try {
            val tarGzFile = File(context.cacheDir, "engine-bundle.tar.gz")
            context.assets.open(BUNDLE_ASSET).use { input ->
                FileOutputStream(tarGzFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Extract using Toybox tar on Android
            val pb = ProcessBuilder(
                "/system/bin/toybox", "tar", "-xzf", tarGzFile.absolutePath, "-C", engineDir.absolutePath
            )
            pb.redirectErrorStream(true)
            val proc = pb.start()
            val output = proc.inputStream.bufferedReader().readText()
            val exitCode = proc.waitFor()
            Log.i(TAG, "Tar extraction exit code: $exitCode, output: $output")

            tarGzFile.delete()

            // Set executable permissions
            ldLoader.setExecutable(true, false)
            agyBinary.setExecutable(true, false)
            File(engineDir, "lib").listFiles()?.forEach { it.setExecutable(true, false) }

            Log.i(TAG, "Engine ready: agy.va39 exists=${agyBinary.exists()} size=${agyBinary.length()}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unpack engine bundle", e)
        }

        engineDir
    }
}
