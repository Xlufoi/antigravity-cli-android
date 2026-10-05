package com.google.antigravity.data.repository

import android.content.Context
import com.google.antigravity.data.ipc.AccountManager
import com.google.antigravity.data.ipc.ChatHistoryManager
import com.google.antigravity.data.ipc.NativeProcessManager
import com.google.antigravity.domain.model.ChatMessage
import com.google.antigravity.domain.model.MessageSender
import com.google.antigravity.domain.model.ModelInfo
import com.google.antigravity.domain.model.UsageStats
import com.google.antigravity.domain.repository.AgentRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class AgentRepositoryImpl(private val context: Context) : AgentRepository {

    private var processManager: NativeProcessManager? = null
    private val _messagesFlow = MutableSharedFlow<ChatMessage>(replay = 50)
    private val scope = CoroutineScope(Dispatchers.IO)
    override val accountManager = AccountManager(context)
    private var currentWorkspace: String = context.filesDir.absolutePath + "/workspace"
    private var currentAutoApprove: Boolean = true

    override suspend fun startSession(
        model: String,
        oauthToken: String?,
        workspacePath: String?,
        autoApprove: Boolean
    ): Boolean {
        if (!workspacePath.isNullOrBlank()) {
            currentWorkspace = workspacePath
        }
        currentAutoApprove = autoApprove

        val manager = NativeProcessManager(
            context = context,
            workspacePath = currentWorkspace,
            model = model,
            oauthToken = oauthToken,
            autoApprovePermissions = autoApprove
        )
        val started = manager.startEngine()
        if (started) {
            processManager = manager
        }
        return started
    }

    override suspend fun sendMessage(prompt: String): Boolean {
        val manager = processManager ?: return false

        val userMessageId = UUID.randomUUID().toString()
        _messagesFlow.emit(
            ChatMessage(
                id = userMessageId,
                sender = MessageSender.USER,
                text = prompt,
                isStreaming = false
            )
        )

        scope.launch {
            val messageId = UUID.randomUUID().toString()
            var currentAgentMessage = ""

            // Immediately emit streaming placeholder so user sees typing indicator instantly
            _messagesFlow.emit(
                ChatMessage(
                    id = messageId,
                    sender = MessageSender.AGENT,
                    text = "",
                    isStreaming = true
                )
            )

            manager.executePrompt(prompt).collect { event ->
                when (event.type) {
                    "auth_url" -> {
                        event.text?.let { urlText ->
                            _messagesFlow.emit(
                                ChatMessage(
                                    sender = MessageSender.SYSTEM,
                                    text = "[AUTH REQUIRED] Google OAuth:\n$urlText\n\nClick button below to open in browser, then paste authorization code here."
                                )
                            )
                        }
                    }
                    "chunk" -> {
                        event.text?.let { chunk ->
                            currentAgentMessage += chunk
                            _messagesFlow.emit(
                                ChatMessage(
                                    id = messageId,
                                    sender = MessageSender.AGENT,
                                    text = currentAgentMessage,
                                    isStreaming = true
                                )
                            )
                        }
                    }
                    "tool_start" -> {
                        event.text?.let { toolStatus ->
                            val displayText = if (currentAgentMessage.isNotBlank()) {
                                "$currentAgentMessage\n\n_${toolStatus}_"
                            } else {
                                "_${toolStatus}_"
                            }
                            _messagesFlow.emit(
                                ChatMessage(
                                    id = messageId,
                                    sender = MessageSender.AGENT,
                                    text = displayText,
                                    isStreaming = true
                                )
                            )
                        }
                    }
                    "final" -> {
                        event.text?.let { fullText ->
                            currentAgentMessage = fullText
                            _messagesFlow.emit(
                                ChatMessage(
                                    id = messageId,
                                    sender = MessageSender.AGENT,
                                    text = currentAgentMessage,
                                    isStreaming = true
                                )
                            )
                        }
                    }
                    "done" -> {
                        _messagesFlow.emit(
                            ChatMessage(
                                id = messageId,
                                sender = MessageSender.AGENT,
                                text = currentAgentMessage,
                                isStreaming = false
                            )
                        )
                    }
                }
            }
        }
        return true
    }

    override fun sendInput(input: String): Boolean {
        return processManager?.sendInput(input) ?: false
    }

    override fun importTokenFromDownloads(): Boolean {
        return processManager?.importTokenFromDownloads() ?: false
    }

    override fun getMessagesFlow(): Flow<ChatMessage> = _messagesFlow.asSharedFlow()

    override fun stopSession() {
        processManager?.stopEngine()
    }

    override suspend fun fetchAvailableModels(): List<ModelInfo> {
        val manager = processManager ?: NativeProcessManager(context = context)
        return manager.fetchAvailableModels()
    }

    override fun generateAuthUrl(): Flow<String> {
        val manager = processManager ?: NativeProcessManager(context = context)
        return manager.generateAuthUrl()
    }

    override fun setWorkspacePath(path: String) {
        currentWorkspace = path
        processManager?.workspacePath = path
    }

    override fun getWorkspacePath(): String = currentWorkspace

    override fun setAutoApprove(autoApprove: Boolean) {
        currentAutoApprove = autoApprove
        processManager?.autoApprovePermissions = autoApprove
    }

    override fun isAutoApprove(): Boolean = currentAutoApprove

    override fun getUsageStats(): StateFlow<UsageStats> {
        return processManager?.usageStats ?: MutableStateFlow(UsageStats()).asStateFlow()
    }

    override val chatHistoryManager: ChatHistoryManager by lazy { ChatHistoryManager(context) }

    override suspend fun submitAuthCode(code: String): Boolean {
        val manager = processManager ?: NativeProcessManager(context = context)
        return manager.submitAuthCode(code)
    }

    override fun executeShellCommand(cmd: String, isRoot: Boolean, isShizuku: Boolean): String {
        val manager = processManager ?: NativeProcessManager(context = context)
        return manager.executeShellCommand(cmd, isRoot = isRoot, isShizuku = isShizuku)
    }

    override fun hasStoragePermission(): Boolean = com.google.antigravity.data.ipc.StoragePermissionHelper.hasStoragePermission(context)
    override fun requestStoragePermission() = com.google.antigravity.data.ipc.StoragePermissionHelper.requestStoragePermission(context)
    override fun grantStorageViaRoot(): Boolean = com.google.antigravity.data.ipc.StoragePermissionHelper.tryGrantStorageViaRoot(context)

    override suspend fun syncQuota(): com.google.antigravity.domain.model.UserQuotaSummary {
        val token = accountManager.getActiveAccessToken() ?: ""
        return com.google.antigravity.data.ipc.QuotaClient.fetchQuotaSummary(token)
    }
}
