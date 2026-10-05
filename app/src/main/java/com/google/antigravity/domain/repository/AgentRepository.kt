package com.google.antigravity.domain.repository

import com.google.antigravity.data.ipc.AccountManager
import com.google.antigravity.domain.model.ChatMessage
import com.google.antigravity.domain.model.ModelInfo
import com.google.antigravity.domain.model.UsageStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AgentRepository {
    suspend fun startSession(
        model: String = "gemini-3.8-flash-low",
        oauthToken: String? = null,
        workspacePath: String? = null,
        autoApprove: Boolean = true
    ): Boolean

    suspend fun sendMessage(prompt: String): Boolean
    fun sendInput(input: String): Boolean
    fun importTokenFromDownloads(): Boolean
    fun getMessagesFlow(): Flow<ChatMessage>
    fun stopSession()

    suspend fun fetchAvailableModels(): List<ModelInfo>
    fun generateAuthUrl(): Flow<String>
    fun setWorkspacePath(path: String)
    fun getWorkspacePath(): String
    fun setAutoApprove(autoApprove: Boolean)
    fun isAutoApprove(): Boolean
    fun getUsageStats(): StateFlow<UsageStats>
    val accountManager: AccountManager
    val chatHistoryManager: com.google.antigravity.data.ipc.ChatHistoryManager
    suspend fun submitAuthCode(code: String): Boolean
    fun executeShellCommand(cmd: String, isRoot: Boolean, isShizuku: Boolean): String
    fun hasStoragePermission(): Boolean
    fun requestStoragePermission()
    fun grantStorageViaRoot(): Boolean
    suspend fun syncQuota(): com.google.antigravity.domain.model.UserQuotaSummary
}
