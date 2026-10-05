package com.google.antigravity.data.ipc

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
    private val binaryPath: String,
    private val workspacePath: String,
    private val model: String = "Gemini 3.8 Flash (High)"
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

            val binaryFile = File(binaryPath)
            if (!binaryFile.exists()) {
                Log.e(TAG, "Antigravity binary not found at $binaryPath")
                return@withContext false
            }

            val command = listOf(
                binaryPath,
                "--input-format", "stream-json",
                "--output-format", "stream-json",
                "--model", model,
                "--add-dir", workspacePath
            )

            val pb = ProcessBuilder(command)
            pb.directory(File(workspacePath))
            
            // Setup Android environment variables
            val env = pb.environment()
            env["HOME"] = workspacePath
            env["TERM"] = "xterm-256color"
            env["LANG"] = "en_US.UTF-8"
            
            val proc = pb.start()
            process = proc
            writer = BufferedWriter(OutputStreamWriter(proc.outputStream))
            Log.i(TAG, "Antigravity engine process started successfully (PID: ${proc})")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting native engine", e)
            false
        }
    }

    suspend fun sendUserPrompt(prompt: String): Boolean = withContext(Dispatchers.IO) {
        val w = writer ?: return@withContext false
        try {
            // Write prompt formatted as a single-line NDJSON message
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
        try {
            var line: String?
            while (proc.isAlive) {
                line = reader.readLine() ?: break
                if (line.isNotBlank()) {
                    try {
                        val message = json.decodeFromString<AgpStreamMessage>(line)
                        emit(message)
                    } catch (e: Exception) {
                        // Fallback text event if line is plain text
                        emit(AgpStreamMessage(type = "text", text = line))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading from process stdout", e)
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
