package com.google.antigravity.data.ipc

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import com.google.antigravity.data.model.AgpStreamMessage
import com.google.antigravity.domain.model.ModelInfo
import com.google.antigravity.domain.model.UsageStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.UUID

class NativeProcessManager(
    private val context: Context,
    var workspacePath: String = context.filesDir.absolutePath + "/workspace",
    var model: String = "gemini-3.8-flash-low",
    private val oauthToken: String? = null,
    var autoApprovePermissions: Boolean = true
) {
    private var currentProcess: Process? = null
    private val json = Json { ignoreUnknownKeys = true }
    private val _usageStats = MutableStateFlow(UsageStats())
    val usageStats: StateFlow<UsageStats> = _usageStats.asStateFlow()

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
            File(workspacePath).mkdirs()
            val geminiDir = File(context.filesDir, ".gemini/antigravity-cli")
            geminiDir.mkdirs()

            // 1. Ensure installation_id exists
            val installIdFile = File(geminiDir, "installation_id")
            if (!installIdFile.exists() || installIdFile.length() == 0L) {
                installIdFile.writeText(UUID.randomUUID().toString())
                AppLogger.log(TAG, "Initialized installation_id: ${installIdFile.readText()}")
            }

            // 2. Ensure jetski_state.pbtxt exists to avoid headless onboarding abort
            val jetskiFile = File(geminiDir, "jetski_state.pbtxt")
            if (!jetskiFile.exists() || jetskiFile.length() == 0L) {
                jetskiFile.writeText(
                    """
                    post_onboarding: {
                      completed_steps: POST_ONBOARDING_STEP_TYPE_MANAGER_WELCOME
                      completed_steps: POST_ONBOARDING_STEP_TYPE_USAGE_MODE
                      completed_steps: POST_ONBOARDING_STEP_TYPE_AGENT_CONFIGURATION
                      completed_steps: POST_ONBOARDING_STEP_TYPE_ADD_WORKSPACE
                    }
                    installation_uuid: "${UUID.randomUUID()}"
                    migrations: {
                      key: 3
                      value: MIGRATION_STATUS_COMPLETED
                    }
                    migrations: {
                      key: 4
                      value: MIGRATION_STATUS_COMPLETED
                    }
                    migrations: {
                      key: 5
                      value: MIGRATION_STATUS_COMPLETED
                    }
                    """.trimIndent()
                )
                AppLogger.log(TAG, "Initialized default jetski_state.pbtxt")
            }

            // 3. Ensure settings.json exists
            val settingsFile = File(geminiDir, "settings.json")
            if (!settingsFile.exists() || settingsFile.length() == 0L || settingsFile.readText().contains("High")) {
                settingsFile.writeText(
                    """
                    {
                      "model": "gemini-3.8-flash-low",
                      "trustedWorkspaces": [
                        "$workspacePath"
                      ]
                    }
                    """.trimIndent()
                )
                AppLogger.log(TAG, "Initialized default settings.json (gemini-3.8-flash-low)")
            }

            // 4. Create resolv.conf, hosts, and nsswitch.conf for glibc networking & localhost resolution
            val resolvFile = File(context.filesDir, "resolv.conf")
            resolvFile.writeText(
                """
                nameserver 8.8.8.8
                nameserver 8.8.4.4
                nameserver 1.1.1.1
                options timeout:2 attempts:3
                """.trimIndent()
            )
            resolvFile.setReadable(true, false)
            AppLogger.log(TAG, "Created resolv.conf (${resolvFile.length()} bytes)")

            val hostsFile = File(context.filesDir, "hosts")
            hostsFile.writeText(
                """
                127.0.0.1 localhost localhost.localdomain
                ::1 localhost ip6-localhost ip6-loopback
                """.trimIndent()
            )
            hostsFile.setReadable(true, false)
            AppLogger.log(TAG, "Created hosts (${hostsFile.length()} bytes)")

            val nsswitchFile = File(context.filesDir, "nsswitch.conf")
            nsswitchFile.writeText(
                """
                hosts: files dns
                networks: files
                protocols: files
                services: files
                """.trimIndent()
            )
            nsswitchFile.setReadable(true, false)
            AppLogger.log(TAG, "Created nsswitch.conf (${nsswitchFile.length()} bytes)")

            // 5. Extract CA certificates for HTTPS/SSL
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

            // 6. Auto-import token from Downloads if token file is missing
            val tokenFile = File(geminiDir, "antigravity-oauth-token")
            val downloadToken = File("/storage/emulated/0/Download/antigravity-oauth-token")
            if ((!tokenFile.exists() || tokenFile.length() == 0L) && downloadToken.exists()) {
                try {
                    downloadToken.copyTo(tokenFile, overwrite = true)
                    AppLogger.log(TAG, "Auto-imported token from /sdcard/Download/antigravity-oauth-token (${tokenFile.length()} bytes)")
                } catch (e: Exception) {
                    AppLogger.log(TAG, "Failed to auto-import token from Downloads: ${e.message}")
                }
            }

            // 7. Save user OAuth token if passed directly
            if (!oauthToken.isNullOrBlank()) {
                saveToken(oauthToken)
            }
        }

        ready
    }

    fun saveToken(rawToken: String): Boolean {
        return try {
            val geminiDir = File(context.filesDir, ".gemini/antigravity-cli")
            geminiDir.mkdirs()
            val tokenFile = File(geminiDir, "antigravity-oauth-token")
            val trimmed = rawToken.trim()

            val tokenJson = if (trimmed.startsWith("{")) {
                trimmed
            } else {
                """{
  "token": {
    "access_token": "$trimmed",
    "token_type": "Bearer",
    "refresh_token": "",
    "expiry": "2099-01-01T00:00:00Z"
  },
  "auth_method": "consumer"
}"""
            }
            tokenFile.writeText(tokenJson)
            AppLogger.log(TAG, "Saved user oauth token to ${tokenFile.absolutePath} (${tokenFile.length()} bytes)")
            true
        } catch (e: Exception) {
            AppLogger.log(TAG, "Failed to save oauth token: ${e.message}")
            false
        }
    }

    fun importTokenFromDownloads(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            AppLogger.log(TAG, "Requesting MANAGE_EXTERNAL_STORAGE permission for Downloads access")
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
            return false
        }
        return try {
            val downloadToken = File("/storage/emulated/0/Download/antigravity-oauth-token")
            if (!downloadToken.exists() || downloadToken.length() == 0L) {
                AppLogger.log(TAG, "No token found in /sdcard/Download/antigravity-oauth-token")
                return false
            }
            val geminiDir = File(context.filesDir, ".gemini/antigravity-cli")
            geminiDir.mkdirs()
            val tokenFile = File(geminiDir, "antigravity-oauth-token")
            downloadToken.copyTo(tokenFile, overwrite = true)
            AppLogger.log(TAG, "Successfully imported token from Downloads (${tokenFile.length()} bytes)")
            true
        } catch (e: Exception) {
            AppLogger.log(TAG, "Error importing token: ${e.message}")
            false
        }
    }

    fun sendInput(text: String): Boolean {
        return try {
            val proc = currentProcess
            if (proc != null && proc.isAlive) {
                proc.outputStream.write((text.trim() + "\n").toByteArray())
                proc.outputStream.flush()
                AppLogger.log(TAG, "Piped user input to stdin (${text.length} chars)")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            AppLogger.log(TAG, "Failed piping input to stdin: ${e.message}")
            false
        }
    }

    fun executePrompt(prompt: String): Flow<AgpStreamMessage> = channelFlow {
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

        val effortValue = when {
            model.contains("high", ignoreCase = true) -> "high"
            model.contains("medium", ignoreCase = true) -> "medium"
            else -> "low"
        }

        val command = mutableListOf(
            nativeLd.absolutePath,
            "--library-path", libraryPaths,
            agyBinary.absolutePath,
            "-p", prompt,
            "--continue",
            "--output-format", "stream-json",
            "--model", model,
            "--effort", effortValue,
            "--add-dir", workspaceDir.absolutePath
        )
        if (autoApprovePermissions) {
            command.add("--dangerously-skip-permissions")
        }

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
        // Force Go to use CGO resolver which calls getaddrinfo in patched libc.so.6
        env["GODEBUG"] = "netdns=cgo"
        env["RES_OPTIONS"] = "timeout:2 attempts:3"
        env["PWD"] = workspaceDir.absolutePath
        val tmpDir = File(context.cacheDir, "tmp").apply { mkdirs() }
        env["TMPDIR"] = tmpDir.absolutePath

        var doneEmitted = false
        fun emitDone() {
            if (!doneEmitted) {
                doneEmitted = true
                trySend(AgpStreamMessage(type = "done"))
            }
        }

        try {
            val proc = pb.start()
            currentProcess = proc
            val reader = BufferedReader(InputStreamReader(proc.inputStream))
            val errReader = BufferedReader(InputStreamReader(proc.errorStream))

            // Thread to monitor STDERR for logs & OAuth login URL
            val errThread = Thread {
                try {
                    errReader.forEachLine { errLine ->
                        AppLogger.log(TAG, "STDERR: $errLine")
                        if (errLine.contains("https://accounts.google.com/o/oauth2/auth") ||
                            (errLine.contains("https://") && errLine.contains("oauth"))) {
                            val start = errLine.indexOf("https://")
                            val url = errLine.substring(start).trim()
                            trySend(AgpStreamMessage(type = "auth_url", text = url))
                        }
                    }
                } catch (_: Exception) {}
            }
            errThread.start()

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

                            val usageObj = stepUpdate?.get("usage")?.jsonObject
                            if (usageObj != null) {
                                val inTok = usageObj["input_tokens"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
                                val outTok = usageObj["output_tokens"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
                                val thinkTok = usageObj["thinking_tokens"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
                                val totTok = usageObj["total_tokens"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
                                _usageStats.update { prev ->
                                    prev.copy(
                                        lastInputTokens = inTok,
                                        lastOutputTokens = outTok,
                                        lastThinkingTokens = thinkTok,
                                        lastTotalTokens = totTok,
                                        sessionTotalTokens = prev.sessionTotalTokens + totTok
                                    )
                                }
                            }

                            if (textDelta != null) {
                                send(AgpStreamMessage(type = "chunk", text = textDelta))
                            } else if (stepType == "tool") {
                                val toolName = stepUpdate?.get("tool_name")?.jsonPrimitive?.content ?: "инструмент"
                                if (state == "ACTIVE") {
                                    send(AgpStreamMessage(type = "tool_start", text = "🔧 Выполняется: $toolName..."))
                                }
                            }
                        }
                        "result" -> {
                            val resultObj = rootObj["result"]?.jsonObject
                            val resText = resultObj?.get("response")?.jsonPrimitive?.content

                            val usageObj = resultObj?.get("usage")?.jsonObject
                            if (usageObj != null) {
                                val inTok = usageObj["input_tokens"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
                                val outTok = usageObj["output_tokens"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
                                val thinkTok = usageObj["thinking_tokens"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
                                val totTok = usageObj["total_tokens"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
                                _usageStats.update { prev ->
                                    prev.copy(
                                        lastInputTokens = inTok,
                                        lastOutputTokens = outTok,
                                        lastThinkingTokens = thinkTok,
                                        lastTotalTokens = totTok,
                                        sessionTotalTokens = prev.sessionTotalTokens + totTok,
                                        turnsCount = prev.turnsCount + 1
                                    )
                                }
                            }

                            if (!resText.isNullOrBlank()) {
                                send(AgpStreamMessage(type = "final", text = resText))
                            }
                            emitDone()
                        }
                        else -> {
                            if (l.contains("https://accounts.google.com") || (l.contains("https://") && l.contains("oauth"))) {
                                val start = l.indexOf("https://")
                                send(AgpStreamMessage(type = "auth_url", text = l.substring(start).trim()))
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (l.contains("https://accounts.google.com") || (l.contains("https://") && l.contains("oauth"))) {
                        val start = l.indexOf("https://")
                        send(AgpStreamMessage(type = "auth_url", text = l.substring(start).trim()))
                    } else {
                        send(AgpStreamMessage(type = "chunk", text = l + "\n"))
                    }
                }
            }

            proc.waitFor()
            try { errThread.join(500) } catch (_: Exception) {}
            emitDone()
            AppLogger.log(TAG, "Turn completed with exit code: ${proc.exitValue()}")
        } catch (e: Exception) {
            AppLogger.log(TAG, "Execution failed: ${e.message}\n${e.stackTraceToString()}")
            send(AgpStreamMessage(type = "chunk", text = "Ошибка выполнения: ${e.localizedMessage}"))
            emitDone()
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

    suspend fun fetchAvailableModels(): List<ModelInfo> = withContext(Dispatchers.IO) {
        val engineDir = File(context.filesDir, "engine")
        val agyBinary = File(engineDir, "agy.va39")
        val nativeLibDir = File(context.applicationInfo.nativeLibraryDir)
        val nativeLd = File(nativeLibDir, "libld.so")
        val libDir = File(engineDir, "lib")
        val geminiDir = File(context.filesDir, ".gemini/antigravity-cli")
        val caCertFile = File(context.filesDir, "cacert.pem")

        val fallback = listOf(
            ModelInfo(id = "gemini-3.8-flash-low", displayName = "Gemini 3.8 Flash (Low ⚡ Instant)", isRecommended = true, effort = "low"),
            ModelInfo(id = "gemini-3.8-flash-medium", displayName = "Gemini 3.8 Flash (Medium ⚖️ Balance)", effort = "medium"),
            ModelInfo(id = "gemini-3.8-flash-high", displayName = "Gemini 3.8 Flash (High 🧠 Thinking)", effort = "high"),
            ModelInfo(id = "gemini-3.7-flash-low", displayName = "Gemini 3.7 Flash (Low)", effort = "low"),
            ModelInfo(id = "gemini-3.6-flash-low", displayName = "Gemini 3.6 Flash (Low)", effort = "low"),
            ModelInfo(id = "claude-sonnet-4-6", displayName = "Claude Sonnet 4.6 (Claude 🎭)", effort = "low"),
            ModelInfo(id = "gpt-oss-120b-medium", displayName = "GPT-OSS 120B (Medium)", effort = "medium")
        )

        if (!agyBinary.exists() || !nativeLd.exists()) {
            return@withContext fallback
        }

        val libraryPaths = listOf(nativeLibDir.absolutePath, libDir.absolutePath).joinToString(":")
        val command = listOf(
            nativeLd.absolutePath,
            "--library-path", libraryPaths,
            agyBinary.absolutePath,
            "models"
        )

        try {
            AppLogger.log(TAG, "Fetching dynamic models via: ${command.joinToString(" ")}")
            val pb = ProcessBuilder(command)
            val env = pb.environment()
            env["HOME"] = context.filesDir.absolutePath
            env["ANTIGRAVITY_APP_DATA_DIR"] = geminiDir.absolutePath
            env["SSL_CERT_FILE"] = caCertFile.absolutePath
            env["TERM"] = "xterm-256color"
            env["GODEBUG"] = "netdns=cgo"
            env["RES_OPTIONS"] = "timeout:2 attempts:3"

            val proc = pb.start()
            val reader = BufferedReader(InputStreamReader(proc.inputStream))
            val models = mutableListOf<ModelInfo>()

            reader.forEachLine { line ->
                val l = line.trim()
                if (l.isNotEmpty() && !l.contains("Fetching available models") && !l.startsWith("[") && !l.startsWith("⠋")) {
                    val parts = l.split(Regex("\\s{2,}"), limit = 2)
                    if (parts.isNotEmpty()) {
                        val id = parts[0].trim()
                        val displayName = if (parts.size > 1) parts[1].trim() else id
                        val effort = when {
                            id.contains("high") -> "high"
                            id.contains("medium") -> "medium"
                            else -> "low"
                        }
                        val isRec = id == "gemini-3.8-flash-low"
                        models.add(ModelInfo(id = id, displayName = displayName, isRecommended = isRec, effort = effort))
                    }
                }
            }
            proc.waitFor()
            if (models.isNotEmpty()) {
                AppLogger.log(TAG, "Fetched ${models.size} models from server")
                models
            } else {
                fallback
            }
        } catch (e: Exception) {
            AppLogger.log(TAG, "Failed fetching models from server: ${e.message}")
            fallback
        }
    }

    fun generateAuthUrl(): Flow<String> = channelFlow {
        val engineDir = File(context.filesDir, "engine")
        val agyBinary = File(engineDir, "agy.va39")
        val nativeLibDir = File(context.applicationInfo.nativeLibraryDir)
        val nativeLd = File(nativeLibDir, "libld.so")
        val libDir = File(engineDir, "lib")
        val caCertFile = File(context.filesDir, "cacert.pem")
        val tempGeminiDir = File(context.cacheDir, "temp_auth_${UUID.randomUUID()}").apply { mkdirs() }

        val libraryPaths = listOf(nativeLibDir.absolutePath, libDir.absolutePath).joinToString(":")
        val command = listOf(
            nativeLd.absolutePath,
            "--library-path", libraryPaths,
            agyBinary.absolutePath,
            "-p", "login_trigger"
        )

        AppLogger.log(TAG, "Triggering login URL generation...")
        val pb = ProcessBuilder(command)
        val env = pb.environment()
        env["HOME"] = context.filesDir.absolutePath
        env["ANTIGRAVITY_APP_DATA_DIR"] = tempGeminiDir.absolutePath
        env["SSL_CERT_FILE"] = caCertFile.absolutePath
        env["TERM"] = "xterm-256color"
        env["GODEBUG"] = "netdns=cgo"
        env["RES_OPTIONS"] = "timeout:2 attempts:3"

        try {
            val proc = pb.start()
            val errReader = BufferedReader(InputStreamReader(proc.errorStream))
            val outReader = BufferedReader(InputStreamReader(proc.inputStream))

            val thread = Thread {
                try {
                    errReader.forEachLine { line ->
                        AppLogger.log(TAG, "AUTH STDERR: $line")
                        if (line.contains("https://accounts.google.com/o/oauth2/auth") || (line.contains("https://") && line.contains("oauth"))) {
                            val start = line.indexOf("https://")
                            val url = line.substring(start).trim()
                            trySend(url)
                        }
                    }
                } catch (_: Exception) {}
            }
            thread.start()

            outReader.forEachLine { line ->
                AppLogger.log(TAG, "AUTH STDOUT: $line")
                if (line.contains("https://accounts.google.com/o/oauth2/auth") || (line.contains("https://") && line.contains("oauth"))) {
                    val start = line.indexOf("https://")
                    val url = line.substring(start).trim()
                    trySend(url)
                }
            }

            thread.join(8000)
            proc.destroy()
        } catch (e: Exception) {
            AppLogger.log(TAG, "Auth generation error: ${e.message}")
        } finally {
            tempGeminiDir.deleteRecursively()
        }
    }.flowOn(Dispatchers.IO)
}
