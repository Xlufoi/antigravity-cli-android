package com.google.antigravity.data.ipc

import android.content.Context
import android.system.Os
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object EngineInstaller {
    private const val TAG = "EngineInstaller"
    private const val ENGINE_VERSION = "2.6"

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
        "split", "ssl_client", "stat", "strings", "stty", "sync", "sysctl",
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
        val nativeLibDir = File(context.applicationInfo.nativeLibraryDir)
        val libBash = File(nativeLibDir, "libbash.so")

        val ready = agyBinary.exists() && ldLoader.exists() && agyBinary.length() > 50_000_000 &&
                isVersionMatch && libBash.exists()

        if (!ready) {
            AppLogger.log(TAG, "isEngineReady: false (agy=${agyBinary.exists()}/${agyBinary.length()}, ld=${ldLoader.exists()}, ver=$isVersionMatch, libbash=${libBash.exists()})")
        }
        return ready
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

            // Extract engine bundle if missing
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
                            val copyProgress = 0.15f + (totalCopied / 80_000_000f).coerceAtMost(0.45f)
                            onProgress(copyProgress, "Копирование: ${totalCopied / (1024 * 1024)} МБ...")
                        }
                    }
                }

                AppLogger.log(TAG, "Copied temp archive: ${tempTarFile.length()} bytes")
                onProgress(0.65f, "Распаковка ядра в engine/ (200 МБ)...")

                val extracted = extractTarArchive(tempTarFile, engineDir)
                tempTarFile.delete()

                if (!extracted) {
                    val errorMsg = "Ошибка распаковки ядра engine-bundle!"
                    AppLogger.log(TAG, errorMsg)
                    onProgress(0f, errorMsg)
                    return@withContext false
                }

                ldLoader.setExecutable(true, false)
                agyBinary.setExecutable(true, false)
                File(engineDir, "lib").listFiles()?.forEach { it.setExecutable(true, false) }
            }

            onProgress(0.85f, "Настройка изолированного окружения Termux (bash, busybox, curl, git)...")
            setupSandboxLinuxEnvironment(context)

            File(engineDir, ".version").writeText(ENGINE_VERSION)

            val success = isEngineReady(context)
            if (success) {
                AppLogger.log(TAG, "INSTALLATION COMPLETED SUCCESSFULLY! Binary size=${agyBinary.length()}")
                onProgress(1.0f, "Распаковка успешно завершена!")
            } else {
                val errorMsg = "Ошибка проверки: файлы не найдены (agy=${agyBinary.exists()})"
                AppLogger.log(TAG, errorMsg)
                onProgress(0f, errorMsg)
            }
            success
        } catch (e: Exception) {
            AppLogger.log(TAG, "Installation exception: ${e.message}\n${e.stackTraceToString()}")
            onProgress(0f, "Исключение при распаковке: ${e.localizedMessage}")
            false
        }
    }

    private fun extractTarArchive(tarFile: File, targetDir: File): Boolean {
        val isGzip = isGzipFile(tarFile)
        val flagsToTry = if (isGzip) listOf("-xzf", "-xf") else listOf("-xf", "-xzf")
        for (flag in flagsToTry) {
            try {
                val pb = ProcessBuilder("/system/bin/toybox", "tar", flag, tarFile.absolutePath, "-C", targetDir.absolutePath)
                pb.redirectErrorStream(true)
                val proc = pb.start()
                val logOutput = proc.inputStream.bufferedReader().readText()
                val exitCode = proc.waitFor()
                AppLogger.log(TAG, "Toybox tar ($flag) to ${targetDir.name} exitCode=$exitCode, output: $logOutput")
                if (exitCode == 0) return true
            } catch (e: Exception) {
                AppLogger.log(TAG, "Toybox tar error ($flag): ${e.message}")
            }
        }
        return false
    }

    private fun isGzipFile(file: File): Boolean {
        return try {
            file.inputStream().use { input ->
                val b1 = input.read()
                val b2 = input.read()
                b1 == 0x1f && b2 == 0x8b
            }
        } catch (_: Exception) { false }
    }

    fun setupSandboxLinuxEnvironment(context: Context) {
        try {
            val nativeLibDir = File(context.applicationInfo.nativeLibraryDir)
            val usrDir = File(context.filesDir, "usr").apply { mkdirs() }
            val usrBin = File(usrDir, "bin").apply { mkdirs() }
            val binDir = File(context.filesDir, "bin").apply { mkdirs() }

            val libBash = File(nativeLibDir, "libbash.so")
            val libBusybox = File(nativeLibDir, "libbusybox_exec.so")
            val libCurl = File(nativeLibDir, "libcurl_exec.so")
            val libGit = File(nativeLibDir, "libgit_exec.so")

            // 1. Link bash
            if (libBash.exists()) {
                createSymlinkOrCopy(libBash, File(usrBin, "bash"))
                createSymlinkOrCopy(libBash, File(binDir, "bash"))
                createSymlinkOrCopy(libBash, File(usrBin, "sh"))
            }

            // 2. Link curl and git
            if (libCurl.exists()) {
                createSymlinkOrCopy(libCurl, File(usrBin, "curl"))
            }
            if (libGit.exists()) {
                createSymlinkOrCopy(libGit, File(usrBin, "git"))
            }

            // 3. Link busybox and applets (excluding su, bash, curl, git, sh)
            if (libBusybox.exists()) {
                createSymlinkOrCopy(libBusybox, File(usrBin, "busybox"))
                for (applet in BUSYBOX_APPLETS) {
                    if (applet == "busybox" || applet == "bash" || applet == "curl" || applet == "git" || applet == "sh" || applet == "su") continue
                    createSymlinkOrCopy(libBusybox, File(usrBin, applet))
                    createSymlinkOrCopy(libBusybox, File(binDir, applet))
                }
            }

            // Ensure su is NEVER linked to busybox so real root /system/bin/su works
            try {
                File(usrBin, "su").delete()
                File(binDir, "su").delete()
            } catch (_: Exception) {}

            // 4. Setup TLS / SSL certificates for curl and git
            val caCertFile = File(context.filesDir, "cacert.pem")
            if (caCertFile.exists()) {
                val tlsDir = File(usrDir, "etc/tls").apply { mkdirs() }
                val sslDir = File(usrDir, "ssl").apply { mkdirs() }
                try {
                    caCertFile.copyTo(File(tlsDir, "cert.pem"), overwrite = true)
                    caCertFile.copyTo(File(sslDir, "cert.pem"), overwrite = true)
                } catch (_: Exception) {}
            }

            // 5. Setup .bashrc for bash commands (bridge to Termux tools if Root is available)
            val bashrcFile = File(context.filesDir, ".bashrc")
            bashrcFile.writeText(
                """
                # Antigravity mobile shell environment
                export PATH="${nativeLibDir.absolutePath}:${usrBin.absolutePath}:${binDir.absolutePath}:/system/bin:/system/xbin:/product/bin"
                export LD_LIBRARY_PATH="${nativeLibDir.absolutePath}:/system/lib64"
                
                # Non-intrusive bridge: if Termux is installed and Root is available, expose tools without modifying Termux
                if [ -x /system/bin/su ] && [ -d /data/data/com.termux/files/usr/bin ]; then
                    python3() { /system/bin/su -c "export PATH=/data/data/com.termux/files/usr/bin:\$PATH; export LD_LIBRARY_PATH=/data/data/com.termux/files/usr/lib; export HOME=/data/data/com.termux/files/home; python3 \"\$@\""; }
                    python() { /system/bin/su -c "export PATH=/data/data/com.termux/files/usr/bin:\$PATH; export LD_LIBRARY_PATH=/data/data/com.termux/files/usr/lib; export HOME=/data/data/com.termux/files/home; python \"\$@\""; }
                    ffmpeg() { /system/bin/su -c "export PATH=/data/data/com.termux/files/usr/bin:\$PATH; export LD_LIBRARY_PATH=/data/data/com.termux/files/usr/lib; export HOME=/data/data/com.termux/files/home; ffmpeg \"\$@\""; }
                    pip() { /system/bin/su -c "export PATH=/data/data/com.termux/files/usr/bin:\$PATH; export LD_LIBRARY_PATH=/data/data/com.termux/files/usr/lib; export HOME=/data/data/com.termux/files/home; pip \"\$@\""; }
                    pip3() { /system/bin/su -c "export PATH=/data/data/com.termux/files/usr/bin:\$PATH; export LD_LIBRARY_PATH=/data/data/com.termux/files/usr/lib; export HOME=/data/data/com.termux/files/home; pip3 \"\$@\""; }
                fi
                """.trimIndent()
            )

            // 6. Ensure workspace directory exists
            File(context.filesDir, "workspace").mkdirs()
            AppLogger.log(TAG, "Sandbox Linux environment configured successfully with nativeLibDir: ${nativeLibDir.absolutePath}")
        } catch (e: Exception) {
            AppLogger.log(TAG, "Error configuring sandbox environment: ${e.message}")
        }
    }

    private fun createSymlinkOrCopy(targetFile: File, linkFile: File) {
        try {
            try {
                Os.remove(linkFile.absolutePath)
            } catch (_: Exception) {
                linkFile.delete()
            }
            Os.symlink(targetFile.absolutePath, linkFile.absolutePath)
        } catch (e: Exception) {
            AppLogger.log(TAG, "Symlink error for ${linkFile.name} -> ${targetFile.name}: ${e.message}")
        }
    }
}
