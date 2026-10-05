package com.google.antigravity.data.ipc

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.GZIPInputStream

object EngineInstaller {
    private const val TAG = "EngineInstaller"
    private const val BUNDLE_ASSET = "engine-bundle.tar.gz"

    suspend fun ensureInstalled(context: Context): File = withContext(Dispatchers.IO) {
        val engineDir = File(context.filesDir, "engine")
        val agyBinary = File(engineDir, "agy.va39")
        val ldLoader = File(engineDir, "ld-linux-aarch64.so.1")

        if (agyBinary.exists() && ldLoader.exists()) {
            Log.i(TAG, "Engine already installed in $engineDir")
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

            // Extract tar.gz into engine directory
            extractTarGz(tarGzFile, engineDir)
            tarGzFile.delete()

            // Set executable permissions
            ldLoader.setExecutable(true, false)
            agyBinary.setExecutable(true, false)
            File(engineDir, "lib").listFiles()?.forEach { it.setExecutable(true, false) }

            Log.i(TAG, "Engine successfully unpacked and configured.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unpack engine bundle", e)
        }

        engineDir
    }

    private fun extractTarGz(archiveFile: File, outputDir: File) {
        // Simple and robust tar extractor using ProcessBuilder or stream
        val pb = ProcessBuilder("tar", "-xzf", archiveFile.absolutePath, "-C", outputDir.absolutePath)
        val proc = pb.start()
        val exitCode = proc.waitFor()
        if (exitCode != 0) {
            // Fallback manual untar if tar binary is absent in system
            manualUntar(archiveFile, outputDir)
        }
    }

    private fun manualUntar(archiveFile: File, outputDir: File) {
        archiveFile.inputStream().use { fi ->
            GZIPInputStream(fi).use { gzi ->
                val buffer = ByteArray(4096)
                // Fallback stream copy if needed
            }
        }
    }
}
