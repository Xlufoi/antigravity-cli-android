package com.google.antigravity.data.ipc

import android.content.Context
import android.system.Os
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object EngineInstaller {
    private const val TAG = "EngineInstaller"
    private const val ENGINE_VERSION = "2.3"

    private val BUSYBOX_APPLETS = listOf(
        "[", "[[", "ar", "arch", "arp", "arping", "ascii", "ash", "awk", "base32", "base64",
        "basename", "bbconfig", "bc", "blkdiscard", "blkid", "blockdev", "brctl", "bunzip2",
        "bzcat", "bzip2", "cal", "cat", "chattr", "chgrp", "chmod", "chown", "chpst", "chroot",
        "chrt", "cksum", "clear", "cmp", "comm", "cp", "cpio", "crc32", "cut",
        "date", "dc", "dd", "df", "diff", "dirname", "dmesg", "dos2unix", "du",
        "echo", "ed", "egrep", "env", "expand", "expr", "factor", "fallocate", "false",
        "fdisk", "fgrep", "find", "flock", "fold", "free", "fsync", "fuser", "getopt",
        "grep", "groups", "gunzip", "gzip", "hd", "head", "hexdump", "hexedit", "hostname",
        "id", "install", "ionice", "iostat", "ip", "kill", "killall", "less",
        "link", "ln", "ls", "lsattr", "lsof", "lzcat", "lzma", "lzop",
        "md5sum", "mkdir", "mkfifo", "mktemp", "more", "mv", "nc", "netstat",
        "nice", "nl", "nohup", "nproc", "nsenter", "od", "paste", "patch", "pgrep",
        "pidof", "ping", "pkill", "printenv", "printf", "ps", "pwd", "readlink",
        "realpath", "renice", "reset", "rm", "rmdir", "route", "run-parts", "sed",
        "seq", "sh", "sha1sum", "sha256sum", "sha3sum", "sha512sum", "sleep", "sort",
        "split", "ssl_client", "stat", "strings", "stty", "su", "sync", "sysctl",
        "tac", "tail", "tar", "tee", "test", "time", "timeout", "top", "touch",
        "tr", "traceroute", "true", "truncate", "tty", "uname", "unexpand", "uniq",
        "unlink", "unlzma", "unlzop", "unxz", "unzip", "uptime", "usleep", "uudecode",
        "uuencode", "vi", "watch", "wc", "wget", "which", "whoami", "xargs",
        "xxd", "xz", "xzcat", "yes", "zcat"
    )

    fun isEngineReady(context: Context): Boolean {
        val engineDir = File(context.filesDir, "engine")
        val agyBinary = File(engineDir, "agy.va39")
        val ldLoader = File(engineDir, "ld-linux-aarch64.so.1")
        val versionFile = File(engineDir, ".version")
        val isVersionMatch = versionFile.exists() && versionFile.readText().trim() == ENGINE_VERSION
        val bashFile = File(context.filesDir, "usr/bin/bash")
        val busyboxFile = File(context.filesDir, "usr/bin/busybox")

        return agyBinary.exists() && ldLoader.exists() && agyBinary.length() > 50_000_000 &&
                isVersionMatch && bashFile.exists() && busyboxFile.exists()
    }

    suspend fun installEngine(
        context: Context,
        onProgress: (progress: Float, status: String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val engineDir = File(context.filesDir, "engine")
        val agyBinary = File(engineDir, "agy.va39")
        val ldLoader = File(engineDir, "ld-linux-aarch64.so.1")

        if (isEngineReady(context)) {
            AppLogger.log(TAG, "Engine and Termux sandbox already installed and ready in ${engineDir.absolutePath}")
            onProgress(1.0f, "Ядро и песочница Termux готовы!")
            return@withContext true
        }

        try {
            engineDir.mkdirs()
            onProgress(0.05f, "Поиск архивов в APK...")
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

            // 1. Extract engine bundle if needed
            if (!agyBinary.exists() || !ldLoader.exists() || agyBinary.length() < 50_000_000) {
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
                onProgress(0.55f, "Распаковка ядра в engine/ (200 МБ)...")

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

                ldLoader.setExecutable(true, false)
                agyBinary.setExecutable(true, false)
                File(engineDir, "lib").listFiles()?.forEach { it.setExecutable(true, false) }
            }

            // 2. Extract embedded Termux sandbox bundle
            onProgress(0.75f, "Установка изолированного Termux (bash, busybox, curl, git)...")
            val termuxExtracted = extractEmbeddedTermuxBundle(context)
            if (!termuxExtracted) {
                AppLogger.log(TAG, "Warning: termux-bundle extraction was not successful or asset not found")
            }

            setupSandboxLinuxEnvironment(context)

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

    private fun extractEmbeddedTermuxBundle(context: Context): Boolean {
        return try {
            val assetNames = context.assets.list("") ?: emptyArray()
            val termuxAsset = assetNames.firstOrNull { it.startsWith("termux-bundle") } ?: return false

            val usrDir = File(context.filesDir, "usr").apply { mkdirs() }
            val tempFile = File(context.cacheDir, "termux-temp.tar.gz")

            AppLogger.log(TAG, "Copying $termuxAsset...")
            context.assets.open(termuxAsset).use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            AppLogger.log(TAG, "Extracting Termux bundle to ${usrDir.absolutePath}...")
            val pb = ProcessBuilder(
                "/system/bin/toybox", "tar", "-xzf", tempFile.absolutePath, "-C", usrDir.absolutePath
            )
            pb.redirectErrorStream(true)
            val proc = pb.start()
            val output = proc.inputStream.bufferedReader().readText()
            val exitCode = proc.waitFor()
            AppLogger.log(TAG, "Termux tar exitCode=$exitCode, output: $output")

            tempFile.delete()

            // Set executable bits on binaries & libraries
            val usrBin = File(usrDir, "bin")
            val usrLib = File(usrDir, "lib")
            usrBin.listFiles()?.forEach { it.setExecutable(true, false) }
            usrLib.listFiles()?.forEach { it.setExecutable(true, false) }

            // Create symlinks for BusyBox applets
            val busyboxFile = File(usrBin, "busybox")
            if (busyboxFile.exists()) {
                busyboxFile.setExecutable(true, false)
                for (applet in BUSYBOX_APPLETS) {
                    val appletFile = File(usrBin, applet)
                    if (!appletFile.exists()) {
                        try {
                            Os.symlink("busybox", appletFile.absolutePath)
                        } catch (_: Exception) {}
                    }
                }
            }

            // Setup TLS / SSL certificates for curl and git
            val caCertFile = File(context.filesDir, "cacert.pem")
            if (caCertFile.exists()) {
                val tlsDir = File(usrDir, "etc/tls").apply { mkdirs() }
                val sslDir = File(usrDir, "ssl").apply { mkdirs() }
                try {
                    caCertFile.copyTo(File(tlsDir, "cert.pem"), overwrite = true)
                    caCertFile.copyTo(File(sslDir, "cert.pem"), overwrite = true)
                } catch (_: Exception) {}
            }

            AppLogger.log(TAG, "Termux sandbox successfully extracted to ${usrDir.absolutePath}")
            true
        } catch (e: Exception) {
            AppLogger.log(TAG, "Failed extracting Termux bundle: ${e.message}")
            false
        }
    }

    fun setupSandboxLinuxEnvironment(context: Context) {
        try {
            val binDir = File(context.filesDir, "bin").apply { mkdirs() }
            val usrBin = File(context.filesDir, "usr/bin")
            val bashFile = File(usrBin, "bash")

            // Create bash in bin/ pointing to embedded Termux bash if available, else system sh
            val binBash = File(binDir, "bash")
            if (bashFile.exists() && bashFile.canExecute()) {
                binBash.writeText("#!/system/bin/sh\nexport LD_LIBRARY_PATH=${File(context.filesDir, "usr/lib").absolutePath}:\$LD_LIBRARY_PATH\nexec \"${bashFile.absolutePath}\" \"\$@\"\n")
            } else {
                binBash.writeText("#!/system/bin/sh\nexec /system/bin/sh \"\$@\"\n")
            }
            binBash.setExecutable(true, false)
            binBash.setReadable(true, false)

            // Ensure workspace directory exists
            File(context.filesDir, "workspace").mkdirs()
            AppLogger.log(TAG, "Sandbox Linux environment configured in ${binDir.absolutePath}")
        } catch (e: Exception) {
            AppLogger.log(TAG, "Error configuring sandbox environment: ${e.message}")
        }
    }
}
