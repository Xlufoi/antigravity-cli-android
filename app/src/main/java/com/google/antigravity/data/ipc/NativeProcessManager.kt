package com.google.antigravity.data.ipc

import android.content.Context
import android.util.Log
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
                return@withContext true
            }

            val engineDir = EngineInstaller.ensureInstalled(context)
            val ldLoader = File(engineDir, "ld-linux-aarch64.so.1")
            val agyBinary = File(engineDir, "agy.va39")
            val libDir = File(engineDir, "lib")

            if (!agyBinary.exists() || !ldLoader.exists()) {
                Log.e(TAG, "Engine files missing in $engineDir")
                return@withContext false
            }

            val workspaceDir = File(workspacePath)
            if (!workspaceDir.exists()) workspaceDir.mkdirs()

            // Prepare local gemini config folder inside app's private filesDir
            val geminiDir = File(context.filesDir, ".gemini/antigravity-cli")
            geminiDir.mkdirs()

            // Automatically provision initial OAuth token if available
            val tokenFile = File(geminiDir, "antigravity-oauth-token")
            if (!oauthToken.isNullOrBlank()) {
                val tokenJson = if (oauthToken.trim().startsWith("{")) {
                    oauthToken.trim()
                } else {
                    """{"token":{"access_token":"${oauthToken.trim()}"}}"""
                }
                tokenFile.writeText(tokenJson)
            }

            val command = listOf(
                ldLoader.absolutePath,
                "--library-path", libDir.absolutePath,
                agyBinary.absolutePath,
                "--input-format", "stream-json",
                "--output-format", "stream-json",
                "--model", model,
                "--dangerously-skip-permissions",
                "--add-dir", workspaceDir.absolutePath
            )

            val pb = ProcessBuilder(command)
            pb.directory(workspaceDir)
            pb.redirectErrorStream(false)

            val env = pb.environment()
            env["HOME"] = context.filesDir.absolutePath
            env["TERM"] = "xterm-256color"
            env["LANG"] = "en_US.UTF-8"

            val proc = pb.start()
            process = proc
            writer = BufferedWriter(OutputStreamWriter(proc.outputStream))
            Log.i(TAG, "Antigravity engine process started successfully (PID: $proc)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting native engine", e)
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
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error writing to process stdin", e)
            false
        }
    }

    fun observeEvents(): Flow<AgpStreamMessage> = flow {
        val proc = process ?: return@flow
        val reader = BufferedReader(InputStreamReader(proc.inputStream))
        val errReader = BufferedReader(InputStreamReader(proc.errorStream))

        // Monitor stdout
        try {
            var line: String?
            while (proc.isAlive) {
                line = reader.readLine() ?: break
                if (line.isNotBlank()) {
                    // Check if stdout contains an auth URL prompt
                    if (line.contains("https://accounts.google.com") || line.contains("https://") && line.contains("oauth")) {
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
            Log.e(TAG, "Error reading from process stdout", e)
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
                    emit(errLine)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading stderr", e)
        }
    }.flowOn(Dispatchers.IO)

    fun stopEngine() {
        try {
            writer?.close()
            process?.destroy()
            process = null
            writer = null
            Log.i(TAG, "Engine stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping engine", e)
        }
    }
}
