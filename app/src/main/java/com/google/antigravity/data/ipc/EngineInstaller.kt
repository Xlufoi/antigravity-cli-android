package com.google.antigravity.data.ipc

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object EngineInstaller {
    private const val TAG = "EngineInstaller"
    private const val ENGINE_VERSION = "2.2"

    fun isEngineReady(context: Context): Boolean {
        val engineDir = File(context.filesDir, "engine")
        val agyBinary = File(engineDir, "agy.va39")
        val ldLoader = File(engineDir, "ld-linux-aarch64.so.1")
        val versionFile = File(engineDir, ".version")
        val isVersionMatch = versionFile.exists() && versionFile.readText().trim() == ENGINE_VERSION
        return agyBinary.exists() && ldLoader.exists() && agyBinary.length() > 50_000_000 && isVersionMatch
    }

    suspend fun installEngine(
        context: Context,
        onProgress: (progress: Float, status: String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val engineDir = File(context.filesDir, "engine")
        val agyBinary = File(engineDir, "agy.va39")
        val ldLoader = File(engineDir, "ld-linux-aarch64.so.1")

        if (isEngineReady(context)) {
            AppLogger.log(TAG, "Engine already installed in $engineDir (${agyBinary.length()} bytes)")
            onProgress(1.0f, "Ядро уже распаковано и готово!")
            return@withContext true
        }

        try {
            engineDir.mkdirs()
            onProgress(0.05f, "Поиск архива в APK...")
            AppLogger.log(TAG, "Checking assets in APK...")

            val assetNames = context.assets.list("") ?: emptyArray()
            AppLogger.log(TAG, "Assets found: ${assetNames.joinToString(", ")}")
            val tarAssetName = assetNames.firstOrNull { it.startsWith("engine-bundle") }

            if (tarAssetName == null) {
                val errorMsg = "Ошибка: архив engine-bundle не найден в assets!"
                AppLogger.log(TAG, errorMsg)
                onProgress(0f, errorMsg)
                return@withContext false
            }

            AppLogger.log(TAG, "Using asset: $tarAssetName")
            onProgress(0.15f, "Копирование архива $tarAssetName...")

            val tempTarFile = File(context.cacheDir, "engine-temp.tar")
            context.assets.open(tarAssetName).use { input ->
                FileOutputStream(tempTarFile).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    var totalCopied = 0L
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalCopied += bytesRead
                        val copyProgress = 0.15f + (totalCopied / 80_000_000f).coerceAtMost(0.35f)
                        onProgress(copyProgress, "Копирование: ${totalCopied / (1024 * 1024)} МБ...")
                    }
                }
            }

            AppLogger.log(TAG, "Copied temp archive: ${tempTarFile.length()} bytes")
            onProgress(0.55f, "Распаковка Toybox TAR в engine/ (200 МБ)...")

            val isGzip = tarAssetName.endsWith(".gz")
            val tarFlag = if (isGzip) "-xzf" else "-xf"

            val pb = ProcessBuilder(
                "/system/bin/toybox", "tar", tarFlag, tempTarFile.absolutePath, "-C", engineDir.absolutePath
            )
            pb.redirectErrorStream(true)
            val proc = pb.start()
            val logOutput = proc.inputStream.bufferedReader().readText()
            val exitCode = proc.waitFor()
            AppLogger.log(TAG, "Toybox tar exitCode=$exitCode, output: $logOutput")

            tempTarFile.delete()

            onProgress(0.85f, "Проверка распакованных файлов...")
            val extractedFiles = engineDir.listFiles()?.map { "${it.name} (${it.length()}b)" } ?: emptyList()
            AppLogger.log(TAG, "Extracted files: ${extractedFiles.joinToString(", ")}")

            onProgress(0.95f, "Выставление прав на исполнение (chmod 755)...")
            ldLoader.setExecutable(true, false)
            agyBinary.setExecutable(true, false)
            File(engineDir, "lib").listFiles()?.forEach { it.setExecutable(true, false) }

            File(engineDir, ".version").writeText(ENGINE_VERSION)

            val success = isEngineReady(context)
            if (success) {
                AppLogger.log(TAG, "INSTALLATION COMPLETED SUCCESSFULLY! Binary size=${agyBinary.length()}")
                onProgress(1.0f, "Распаковка успешно завершена!")
            } else {
                AppLogger.log(TAG, "INSTALLATION VERIFICATION FAILED! Missing binary or loader.")
                onProgress(0f, "Ошибка проверки: файлы не найдены.")
            }
            success
        } catch (e: Exception) {
            AppLogger.log(TAG, "Installation exception: ${e.message}\n${e.stackTraceToString()}")
            onProgress(0f, "Исключение при распаковке: ${e.localizedMessage}")
            false
        }
    }
}
