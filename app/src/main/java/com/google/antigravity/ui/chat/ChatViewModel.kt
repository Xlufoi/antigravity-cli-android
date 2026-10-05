package com.google.antigravity.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.antigravity.data.ipc.AccountProfile
import com.google.antigravity.data.ipc.RootHelper
import com.google.antigravity.data.ipc.ShizukuManager
import com.google.antigravity.domain.model.ChatMessage
import com.google.antigravity.domain.model.ChatSession
import com.google.antigravity.domain.model.MessageSender
import com.google.antigravity.domain.model.ModelInfo
import com.google.antigravity.domain.model.UsageStats
import com.google.antigravity.domain.repository.AgentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppTab {
    CONSOLE,
    CONTROL_CENTER,
    LOGS
}

data class ChatUiState(
    val currentTab: AppTab = AppTab.CONSOLE,
    val currentSessionId: String = UUID.randomUUID().toString(),
    val sessions: List<ChatSession> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val isStreaming: Boolean = false,
    val isEngineReady: Boolean = false,
    val activeModel: String = "gemini-3.8-flash-low",
    val availableModels: List<ModelInfo> = emptyList(),
    val isModelsLoading: Boolean = false,
    val oauthToken: String = "",
    val workspacePath: String = "",
    val autoApprove: Boolean = true,
    // Shizuku & Root & Storage
    val isShizukuInstalled: Boolean = false,
    val isShizukuGranted: Boolean = false,
    val isRootAvailable: Boolean = false,
    val isRootGranted: Boolean = false,
    val isStorageGranted: Boolean = false,
    val systemCommandResult: String? = null,
    // Accounts
    val accounts: List<AccountProfile> = emptyList(),
    val currentTokenSnippet: String = "",
    // Auth URL generator & In-app OAuth WebView
    val generatedAuthUrl: String? = null,
    val isGeneratingAuthUrl: Boolean = false,
    val showOAuthWebView: Boolean = false,
    // Quota & Usage
    val usageStats: UsageStats = UsageStats()
)

class ChatViewModel(
    private val repository: AgentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(workspacePath = repository.getWorkspacePath()) }
        observeMessages()
        observeUsageStats()
        loadModels()
        loadAccounts()
        checkSystemPrivileges()
        loadSessions()
        initEngine()
    }

    fun setTab(tab: AppTab) {
        _uiState.update { it.copy(currentTab = tab) }
        if (tab == AppTab.CONTROL_CENTER) {
            checkSystemPrivileges()
            loadAccounts()
        }
    }

    // --- Chat Sessions & Persistence ---
    private fun loadSessions() {
        viewModelScope.launch {
            val sessionsList = repository.chatHistoryManager.getAllSessions()
            val lastActiveId = repository.chatHistoryManager.getLastActiveSessionId()
            val activeSession = sessionsList.find { it.id == lastActiveId } ?: sessionsList.firstOrNull()

            if (activeSession != null) {
                _uiState.update {
                    it.copy(
                        sessions = sessionsList,
                        currentSessionId = activeSession.id,
                        messages = activeSession.messages,
                        activeModel = activeSession.model.ifBlank { it.activeModel },
                        workspacePath = activeSession.workspace.ifBlank { it.workspacePath }
                    )
                }
            } else {
                val newId = UUID.randomUUID().toString()
                val newSession = ChatSession(
                    id = newId,
                    title = "Новый диалог",
                    model = _uiState.value.activeModel,
                    workspace = _uiState.value.workspacePath
                )
                repository.chatHistoryManager.saveSession(newSession)
                repository.chatHistoryManager.setLastActiveSessionId(newId)
                _uiState.update {
                    it.copy(
                        sessions = listOf(newSession),
                        currentSessionId = newId,
                        messages = emptyList()
                    )
                }
            }
        }
    }

    fun createNewChat() {
        viewModelScope.launch {
            val newId = UUID.randomUUID().toString()
            val newSession = ChatSession(
                id = newId,
                title = "Новый диалог",
                model = _uiState.value.activeModel,
                workspace = _uiState.value.workspacePath
            )
            repository.chatHistoryManager.saveSession(newSession)
            repository.chatHistoryManager.setLastActiveSessionId(newId)
            val updatedSessions = repository.chatHistoryManager.getAllSessions()

            _uiState.update {
                it.copy(
                    sessions = updatedSessions,
                    currentSessionId = newId,
                    messages = emptyList()
                )
            }
        }
    }

    fun switchChat(sessionId: String) {
        viewModelScope.launch {
            val session = repository.chatHistoryManager.getSession(sessionId) ?: return@launch
            repository.chatHistoryManager.setLastActiveSessionId(sessionId)
            val updatedSessions = repository.chatHistoryManager.getAllSessions()

            _uiState.update {
                it.copy(
                    sessions = updatedSessions,
                    currentSessionId = session.id,
                    messages = session.messages,
                    activeModel = session.model.ifBlank { it.activeModel }
                )
            }
        }
    }

    fun deleteChat(sessionId: String) {
        viewModelScope.launch {
            repository.chatHistoryManager.deleteSession(sessionId)
            val updatedSessions = repository.chatHistoryManager.getAllSessions()
            if (_uiState.value.currentSessionId == sessionId) {
                if (updatedSessions.isNotEmpty()) {
                    switchChat(updatedSessions.first().id)
                } else {
                    createNewChat()
                }
            } else {
                _uiState.update { it.copy(sessions = updatedSessions) }
            }
        }
    }

    private fun persistCurrentMessages(newMessages: List<ChatMessage>) {
        viewModelScope.launch {
            val currentId = _uiState.value.currentSessionId
            var currentTitle = _uiState.value.sessions.find { it.id == currentId }?.title ?: "Новый диалог"
            if (currentTitle == "Новый диалог" && newMessages.any { it.sender == MessageSender.USER }) {
                val firstUserMsg = newMessages.first { it.sender == MessageSender.USER }.text.trim()
                if (firstUserMsg.isNotBlank()) {
                    currentTitle = firstUserMsg.take(28) + if (firstUserMsg.length > 28) "…" else ""
                }
            }

            val session = ChatSession(
                id = currentId,
                title = currentTitle,
                updatedAt = System.currentTimeMillis(),
                messages = newMessages,
                model = _uiState.value.activeModel,
                workspace = _uiState.value.workspacePath
            )
            repository.chatHistoryManager.saveSession(session)
            val updatedSessions = repository.chatHistoryManager.getAllSessions()
            _uiState.update { it.copy(sessions = updatedSessions) }
        }
    }

    // --- Engine & Execution ---
    fun initEngine(token: String? = null) {
        viewModelScope.launch {
            val tokenToUse = token ?: _uiState.value.oauthToken.ifBlank { null }
            val success = repository.startSession(
                model = _uiState.value.activeModel,
                oauthToken = tokenToUse,
                workspacePath = _uiState.value.workspacePath,
                autoApprove = _uiState.value.autoApprove
            )
            _uiState.update {
                it.copy(
                    isEngineReady = success,
                    currentTokenSnippet = repository.accountManager.getCurrentTokenSnippet()
                )
            }
        }
    }

    private fun observeMessages() {
        viewModelScope.launch {
            repository.getMessagesFlow().collect { incomingMsg ->
                _uiState.update { state ->
                    val existingIndex = state.messages.indexOfLast { it.id == incomingMsg.id }
                    val updatedList = if (existingIndex != -1) {
                        state.messages.toMutableList().apply {
                            set(existingIndex, incomingMsg)
                        }
                    } else {
                        state.messages + incomingMsg
                    }
                    if (!incomingMsg.isStreaming) {
                        persistCurrentMessages(updatedList)
                    }
                    state.copy(
                        messages = updatedList,
                        isStreaming = incomingMsg.isStreaming
                    )
                }
            }
        }
    }

    private fun observeUsageStats() {
        viewModelScope.launch {
            repository.getUsageStats().collect { stats ->
                val currentModel = _uiState.value.activeModel
                val limit = getContextWindowForModel(currentModel)
                _uiState.update { it.copy(usageStats = stats.copy(contextWindowLimit = limit)) }
            }
        }
    }

    private fun getContextWindowForModel(modelName: String): Long {
        val m = modelName.lowercase()
        return when {
            m.contains("gemini") -> 1_000_000L
            m.contains("claude") -> 200_000L
            m.contains("gpt") -> 128_000L
            else -> 1_000_000L
        }
    }

    fun loadModels(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isModelsLoading = true) }
            val models = repository.fetchAvailableModels()
            _uiState.update {
                it.copy(
                    availableModels = models,
                    isModelsLoading = false
                )
            }
        }
    }

    fun selectModel(newModel: String) {
        val limit = getContextWindowForModel(newModel)
        _uiState.update {
            it.copy(
                activeModel = newModel,
                usageStats = it.usageStats.copy(contextWindowLimit = limit)
            )
        }
        initEngine()
    }

    fun updateWorkspace(path: String) {
        if (path.isNotBlank()) {
            repository.setWorkspacePath(path.trim())
            _uiState.update { it.copy(workspacePath = path.trim()) }
            initEngine()
        }
    }

    fun toggleAutoApprove() {
        val next = !_uiState.value.autoApprove
        repository.setAutoApprove(next)
        _uiState.update { it.copy(autoApprove = next) }
    }

    // --- OAuth & Browser Login ---
    fun requestAuthUrl() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingAuthUrl = true, generatedAuthUrl = null) }
            repository.generateAuthUrl().collect { url ->
                _uiState.update { it.copy(generatedAuthUrl = url, isGeneratingAuthUrl = false) }
            }
        }
    }

    fun openOAuthWebView() {
        _uiState.update { it.copy(showOAuthWebView = true) }
    }

    fun closeOAuthWebView() {
        _uiState.update { it.copy(showOAuthWebView = false) }
    }

    fun submitAuthCode(code: String, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val success = repository.submitAuthCode(code)
            if (success) {
                loadAccounts()
                initEngine()
                _uiState.update { it.copy(showOAuthWebView = false, generatedAuthUrl = null) }
            }
            onComplete?.invoke(success)
        }
    }

    fun loadAccounts() {
        val list = repository.accountManager.getAccounts()
        val snippet = repository.accountManager.getCurrentTokenSnippet()
        _uiState.update {
            it.copy(
                accounts = list,
                currentTokenSnippet = snippet
            )
        }
    }

    fun saveAccount(name: String, token: String) {
        repository.accountManager.saveAccount(name.trim(), token.trim(), makeActive = true)
        loadAccounts()
        initEngine(token.trim())
    }

    fun switchAccount(id: String) {
        if (repository.accountManager.switchAccount(id)) {
            loadAccounts()
            initEngine()
        }
    }

    fun deleteAccount(id: String) {
        repository.accountManager.deleteAccount(id)
        loadAccounts()
    }

    fun checkSystemPrivileges() {
        val shizukuInstalled = ShizukuManager.isInstalled()
        val shizukuGranted = ShizukuManager.isPermissionGranted()
        val rootAvail = RootHelper.isRootAvailable()
        val rootGranted = if (rootAvail) RootHelper.isRootGranted() else false
        var storageGranted = repository.hasStoragePermission()

        if (!storageGranted && rootAvail) {
            repository.grantStorageViaRoot()
            storageGranted = repository.hasStoragePermission()
        }

        _uiState.update {
            it.copy(
                isShizukuInstalled = shizukuInstalled,
                isShizukuGranted = shizukuGranted,
                isRootAvailable = rootAvail,
                isRootGranted = rootGranted,
                isStorageGranted = storageGranted
            )
        }
    }

    fun requestStoragePermission() {
        repository.requestStoragePermission()
    }

    fun grantStorageViaRoot() {
        viewModelScope.launch {
            repository.grantStorageViaRoot()
            checkSystemPrivileges()
        }
    }

    fun requestShizukuPermission() {
        ShizukuManager.requestPermission()
        checkSystemPrivileges()
    }

    fun testShizukuCommand() {
        viewModelScope.launch {
            val result = ShizukuManager.executeAdb("getprop ro.product.model; whoami")
            _uiState.update { it.copy(systemCommandResult = "Shizuku: $result") }
        }
    }

    fun testRootCommand() {
        viewModelScope.launch {
            val result = RootHelper.executeRootCommand("id; uname -a")
            _uiState.update { it.copy(systemCommandResult = "Root: $result") }
        }
    }

    // --- Message Sending & Shell Execution ---
    fun sendMessage(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) return

        // Direct Linux / Termux shell command execution
        if (trimmed.startsWith("!") || trimmed.startsWith("$")) {
            val shellCmd = trimmed.substring(1).trim()
            val userMsg = ChatMessage(
                id = UUID.randomUUID().toString(),
                sender = MessageSender.USER,
                text = prompt
            )
            val updatedWithUser = _uiState.value.messages + userMsg
            _uiState.update { it.copy(messages = updatedWithUser) }
            persistCurrentMessages(updatedWithUser)

            viewModelScope.launch {
                val output = repository.executeShellCommand(
                    cmd = shellCmd,
                    isRoot = _uiState.value.isRootGranted,
                    isShizuku = _uiState.value.isShizukuGranted
                )
                val prefix = when {
                    _uiState.value.isRootGranted -> "[root@android]#"
                    _uiState.value.isShizukuGranted -> "[adb@shizuku]$"
                    else -> "[sandbox@agy]$"
                }
                val sysMsg = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    sender = MessageSender.SYSTEM,
                    text = "$prefix $shellCmd\n$output"
                )
                val updatedWithSys = _uiState.value.messages + sysMsg
                _uiState.update { it.copy(messages = updatedWithSys) }
                persistCurrentMessages(updatedWithSys)
            }
            return
        }

        // Standard AI turn
        viewModelScope.launch {
            _uiState.update { it.copy(isStreaming = true) }
            repository.sendMessage(trimmed)
        }
    }

    fun updateToken(token: String) {
        _uiState.update { it.copy(oauthToken = token) }
        initEngine(token)
    }

    fun importTokenFromDownloads(): Boolean {
        val success = repository.importTokenFromDownloads()
        if (success) {
            loadAccounts()
            initEngine()
        }
        return success
    }

    fun stopSession() {
        repository.stopSession()
        _uiState.update { it.copy(isStreaming = false) }
    }
}
