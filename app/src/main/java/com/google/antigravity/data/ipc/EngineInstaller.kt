package com.google.antigravity.data.ipc

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object EngineInstaller {
    private const val TAG = "EngineInstaller"

    suspend fun ensureInstalled(
        context: Context,
        onStatusUpdate: ((String) -> Unit)? = null
    ): File = withContext(Dispatchers.IO) {
        val engineDir = File(context.filesDir, "engine")
        val agyBinary = File(engineDir, "agy.va39")
        val ldLoader = File(engineDir, "ld-linux-aarch64.so.1")

        if (agyBinary.exists() && ldLoader.exists() && agyBinary.length() > 50_000_000) {
            Log.i(TAG, "Engine already installed in $engineDir (${agyBinary.length()} bytes)")
            onStatusUpdate?.invoke("Ядро готово к запуску")
            return@withContext engineDir
        }

        engineDir.mkdirs()
        onStatusUpdate?.invoke("Копирование движка из пакета...")
        Log.i(TAG, "Extracting engine from assets...")

        try {
            // Find existing asset: engine-bundle.tar or engine-bundle.tar.gz
            val assetNames = context.assets.list("") ?: emptyArray()
            val tarAssetName = assetNames.firstOrNull { it.startsWith("engine-bundle") } ?: "engine-bundle.tar"
            val isGzip = tarAssetName.endsWith(".gz")

            val tempTarFile = File(context.cacheDir, "engine-temp.tar")
            context.assets.open(tarAssetName).use { input ->
                FileOutputStream(tempTarFile).use { output ->
                    input.copyTo(output)
                }
            }

            onStatusUpdate?.invoke("Распаковка движка (200 МБ)...")
            val tarFlags = if (isGzip) "-xzf" else "-xf"
            
            // Extract using Toybox tar on Android
            val pb = ProcessBuilder(
                "/system/bin/toybox", "tar", tarFlags, tempTarFile.absolutePath, "-C", engineDir.absolutePath
            )
            pb.redirectErrorStream(true)
            val proc = pb.start()
            val logOutput = proc.inputStream.bufferedReader().readText()
            val exitCode = proc.waitFor()
            Log.i(TAG, "Tar extraction exitCode=$exitCode output=$logOutput")

            tempTarFile.delete()

            onStatusUpdate?.invoke("Настройка прав запуска...")
            ldLoader.setExecutable(true, false)
            agyBinary.setExecutable(true, false)
            File(engineDir, "lib").listFiles()?.forEach { it.setExecutable(true, false) }

            Log.i(TAG, "Engine successfully configured! agy.va39 size=${agyBinary.length()}")
            onStatusUpdate?.invoke("Движок успешно распакован!")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unpack engine bundle", e)
            onStatusUpdate?.invoke("Ошибка распаковки: ${e.localizedMessage}")
        }

        engineDir
    }
}
