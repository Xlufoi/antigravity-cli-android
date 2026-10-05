package com.google.antigravity.data.ipc

import android.content.Context
import com.google.antigravity.data.model.AgpStreamMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class NativeProcessManager(
    private val context: Context,
    private val workspacePath: String = context.filesDir.absolutePath + "/workspace",
    private val model: String = "Gemini 3.8 Flash (High)",
    private val oauthToken: String? = null
) {
    private var process: Process? = null
    private var writer: BufferedWriter? = null
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val TAG = "NativeProcessManager"
    }

    suspend fun startEngine(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (process != null && process?.isAlive == true) {
                AppLogger.log(TAG, "Process already alive")
                return@withContext true
            }

            val engineDir = File(context.filesDir, "engine")
            val ldLoader = File(engineDir, "ld-linux-aarch64.so.1")
            val agyBinary = File(engineDir, "agy.va39")
            val libDir = File(engineDir, "lib")

            if (!agyBinary.exists() || !ldLoader.exists()) {
                AppLogger.log(TAG, "Engine files missing! agyBinary=${agyBinary.exists()}, ldLoader=${ldLoader.exists()}")
                return@withContext false
            }

            val workspaceDir = File(workspacePath)
            if (!workspaceDir.exists()) workspaceDir.mkdirs()

            // Prepare local gemini config folder inside app's private filesDir
            val geminiDir = File(context.filesDir, ".gemini/antigravity-cli")
            geminiDir.mkdirs()

            // Extract CA certificates to enable HTTPS SSL handshake in glibc
            val caCertFile = File(context.filesDir, "cacert.pem")
            if (!caCertFile.exists() || caCertFile.length() == 0L) {
                try {
                    context.assets.open("cacert.pem").use { input ->
                        caCertFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    AppLogger.log(TAG, "Extracted cacert.pem (${caCertFile.length()} bytes)")
                } catch (e: Exception) {
                    AppLogger.log(TAG, "Failed extracting cacert.pem: ${e.message}")
                }
            }

            // Provision initial OAuth token if provided by user
            val tokenFile = File(geminiDir, "antigravity-oauth-token")
            if (!tokenFile.exists() || tokenFile.length() == 0L) {
                if (!oauthToken.isNullOrBlank()) {
                    val tokenJson = if (oauthToken.trim().startsWith("{")) {
                        oauthToken.trim()
                    } else {
                        """{"token":{"access_token":"${oauthToken.trim()}"}}"""
                    }
                    tokenFile.writeText(tokenJson)
                    AppLogger.log(TAG, "Saved user oauth token to ${tokenFile.absolutePath}")
                }
            }

            // Prefer ld loader extracted into nativeLibraryDir (allowed to execute by Android W^X policy)
            val nativeLibDir = File(context.applicationInfo.nativeLibraryDir)
            val nativeLd = File(nativeLibDir, "libld.so")
            val executableLoader = if (nativeLd.exists()) nativeLd else ldLoader

            val libraryPaths = listOf(
                nativeLibDir.absolutePath,
                libDir.absolutePath
            ).joinToString(":")

            val command = listOf(
                executableLoader.absolutePath,
                "--library-path", libraryPaths,
                agyBinary.absolutePath,
                "--input-format", "stream-json",
                "--output-format", "stream-json",
                "--model", model,
                "--dangerously-skip-permissions",
                "--add-dir", workspaceDir.absolutePath
            )

            AppLogger.log(TAG, "Starting process: ${command.joinToString(" ")}")

            val pb = ProcessBuilder(command)
            pb.directory(workspaceDir)
            pb.redirectErrorStream(false)

            val env = pb.environment()
            env["HOME"] = context.filesDir.absolutePath
            env["ANTIGRAVITY_APP_DATA_DIR"] = geminiDir.absolutePath
            env["SSL_CERT_FILE"] = caCertFile.absolutePath
            env["TERM"] = "xterm-256color"
            env["LANG"] = "en_US.UTF-8"

            val proc = pb.start()
            process = proc
            writer = BufferedWriter(OutputStreamWriter(proc.outputStream))
            AppLogger.log(TAG, "Antigravity engine process started! (Process: $proc)")
            true
        } catch (e: Exception) {
            AppLogger.log(TAG, "Error starting native engine: ${e.message}\n${e.stackTraceToString()}")
            false
        }
    }

    suspend fun sendUserPrompt(prompt: String): Boolean = withContext(Dispatchers.IO) {
        val w = writer ?: return@withContext false
        try {
            val escaped = prompt.replace("\"", "\\\"").replace("\n", "\\n")
            val payload = "{\"type\":\"user_prompt\",\"prompt\":\"$escaped\"}\n"
            w.write(payload)
            w.flush()
            AppLogger.log(TAG, "Sent user prompt to stdin: $prompt")
            true
        } catch (e: Exception) {
            AppLogger.log(TAG, "Error writing to stdin: ${e.message}")
            false
        }
    }

    fun observeEvents(): Flow<AgpStreamMessage> = flow {
        val proc = process ?: return@flow
        val reader = BufferedReader(InputStreamReader(proc.inputStream))
        try {
            var line: String?
            while (proc.isAlive) {
                line = reader.readLine() ?: break
                if (line.isNotBlank()) {
                    AppLogger.log(TAG, "STDOUT: $line")
                    if (line.contains("https://accounts.google.com") || (line.contains("https://") && line.contains("oauth"))) {
                        emit(AgpStreamMessage(type = "auth_url", text = line))
                    } else {
                        try {
                            val message = json.decodeFromString<AgpStreamMessage>(line)
                            emit(message)
                        } catch (e: Exception) {
                            emit(AgpStreamMessage(type = "text", text = line))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.log(TAG, "Stdout reader error: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)

    fun observeErrorStream(): Flow<String> = flow {
        val proc = process ?: return@flow
        val errReader = BufferedReader(InputStreamReader(proc.errorStream))
        try {
            var errLine: String?
            while (proc.isAlive) {
                errLine = errReader.readLine() ?: break
                if (errLine.isNotBlank()) {
                    AppLogger.log(TAG, "STDERR: $errLine")
                    emit(errLine)
                }
            }
        } catch (e: Exception) {
            AppLogger.log(TAG, "Stderr reader error: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)

    fun stopEngine() {
        try {
            writer?.close()
            process?.destroy()
            process = null
            writer = null
            AppLogger.log(TAG, "Engine stopped")
        } catch (e: Exception) {
            AppLogger.log(TAG, "Error stopping engine: ${e.message}")
        }
    }
}
