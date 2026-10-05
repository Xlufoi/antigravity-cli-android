package com.google.antigravity.data.ipc

import android.content.Context
import com.google.antigravity.data.model.AgpStreamMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class NativeProcessManager(
    private val context: Context,
    private val workspacePath: String = context.filesDir.absolutePath + "/workspace",
    private val model: String = "Gemini 3.8 Flash (High)",
    private val oauthToken: String? = null
) {
    private var currentProcess: Process? = null
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val TAG = "NativeProcessManager"
    }

    suspend fun startEngine(): Boolean = withContext(Dispatchers.IO) {
        val engineDir = File(context.filesDir, "engine")
        val agyBinary = File(engineDir, "agy.va39")
        val nativeLibDir = File(context.applicationInfo.nativeLibraryDir)
        val nativeLd = File(nativeLibDir, "libld.so")

        val ready = agyBinary.exists() && nativeLd.exists()
        AppLogger.log(TAG, "Checking engine: agyBinary=${agyBinary.exists()}, nativeLd=${nativeLd.exists()} => ready=$ready")

        if (ready) {
            // Ensure workspace directory
            File(workspacePath).mkdirs()
            // Ensure gemini config directory
            val geminiDir = File(context.filesDir, ".gemini/antigravity-cli")
            geminiDir.mkdirs()

            // Extract CA certificates for HTTPS/SSL
            val caCertFile = File(context.filesDir, "cacert.pem")
            if (!caCertFile.exists() || caCertFile.length() == 0L) {
                try {
                    context.assets.open("cacert.pem").use { input ->
                        caCertFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    AppLogger.log(TAG, "Extracted cacert.pem (${caCertFile.length()} bytes)")
                } catch (e: Exception) {
                    AppLogger.log(TAG, "Extract cacert.pem failed: ${e.message}")
                }
            }

            // Save user OAuth token if provided
            val tokenFile = File(geminiDir, "antigravity-oauth-token")
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

        ready
    }

    fun executePrompt(prompt: String): Flow<AgpStreamMessage> = flow {
        val engineDir = File(context.filesDir, "engine")
        val agyBinary = File(engineDir, "agy.va39")
        val nativeLibDir = File(context.applicationInfo.nativeLibraryDir)
        val nativeLd = File(nativeLibDir, "libld.so")
        val libDir = File(engineDir, "lib")
        val workspaceDir = File(workspacePath)
        val geminiDir = File(context.filesDir, ".gemini/antigravity-cli")
        val caCertFile = File(context.filesDir, "cacert.pem")

        val libraryPaths = listOf(
            nativeLibDir.absolutePath,
            libDir.absolutePath
        ).joinToString(":")

        val command = listOf(
            nativeLd.absolutePath,
            "--library-path", libraryPaths,
            agyBinary.absolutePath,
            "-p", prompt,
            "--continue",
            "--output-format", "stream-json",
            "--model", model,
            "--dangerously-skip-permissions",
            "--add-dir", workspaceDir.absolutePath
        )

        AppLogger.log(TAG, "Executing: ${command.joinToString(" ")}")

        val pb = ProcessBuilder(command)
        pb.directory(workspaceDir)
        pb.redirectErrorStream(false)

        val env = pb.environment()
        env["HOME"] = context.filesDir.absolutePath
        env["ANTIGRAVITY_APP_DATA_DIR"] = geminiDir.absolutePath
        env["SSL_CERT_FILE"] = caCertFile.absolutePath
        env["TERM"] = "xterm-256color"
        env["LANG"] = "en_US.UTF-8"

        try {
            val proc = pb.start()
            currentProcess = proc
            val reader = BufferedReader(InputStreamReader(proc.inputStream))
            val errReader = BufferedReader(InputStreamReader(proc.errorStream))

            // Thread to log stderr
            Thread {
                try {
                    errReader.forEachLine { errLine ->
                        AppLogger.log(TAG, "STDERR: $errLine")
                    }
                } catch (_: Exception) {}
            }.start()

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val l = line?.trim() ?: continue
                if (l.isEmpty()) continue
                AppLogger.log(TAG, "STDOUT: $l")

                try {
                    val rootObj = json.parseToJsonElement(l).jsonObject
                    val eventType = rootObj["event"]?.jsonPrimitive?.content

                    when (eventType) {
                        "step_update" -> {
                            val stepUpdate = rootObj["step_update"]?.jsonObject
                            val textDelta = stepUpdate?.get("text_delta")?.jsonPrimitive?.content
                            val stepType = stepUpdate?.get("step_type")?.jsonPrimitive?.content
                            val state = stepUpdate?.get("state")?.jsonPrimitive?.content

                            if (textDelta != null) {
                                emit(AgpStreamMessage(type = "chunk", text = textDelta))
                            }
                            if (state == "DONE" && stepType == "agent_response") {
                                emit(AgpStreamMessage(type = "done"))
                            }
                        }
                        "result" -> {
                            emit(AgpStreamMessage(type = "done"))
                        }
                        else -> {
                            if (l.contains("https://accounts.google.com") || (l.contains("https://") && l.contains("oauth"))) {
                                emit(AgpStreamMessage(type = "auth_url", text = l))
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (l.contains("https://accounts.google.com")) {
                        emit(AgpStreamMessage(type = "auth_url", text = l))
                    } else {
                        emit(AgpStreamMessage(type = "chunk", text = l + "\n"))
                    }
                }
            }

            proc.waitFor()
            emit(AgpStreamMessage(type = "done"))
            AppLogger.log(TAG, "Turn completed with exit code: ${proc.exitValue()}")
        } catch (e: Exception) {
            AppLogger.log(TAG, "Execution failed: ${e.message}\n${e.stackTraceToString()}")
            emit(AgpStreamMessage(type = "chunk", text = "Ошибка выполнения: ${e.localizedMessage}"))
            emit(AgpStreamMessage(type = "done"))
        } finally {
            currentProcess = null
        }
    }.flowOn(Dispatchers.IO)

    fun stopEngine() {
        try {
            currentProcess?.destroy()
            currentProcess = null
            AppLogger.log(TAG, "Current execution stopped by user")
        } catch (e: Exception) {
            AppLogger.log(TAG, "Error stopping execution: ${e.message}")
        }
    }
}
