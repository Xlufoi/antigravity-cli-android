package com.google.antigravity.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.antigravity.data.ipc.AccountProfile
import com.google.antigravity.data.ipc.RootHelper
import com.google.antigravity.data.ipc.ShizukuManager
import com.google.antigravity.domain.model.ChatMessage
import com.google.antigravity.domain.model.ModelInfo
import com.google.antigravity.domain.model.UsageStats
import com.google.antigravity.domain.repository.AgentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppTab {
    CONSOLE,
    CONTROL_CENTER,
    LOGS
}

data class ChatUiState(
    val currentTab: AppTab = AppTab.CONSOLE,
    val messages: List<ChatMessage> = emptyList(),
    val isStreaming: Boolean = false,
    val isEngineReady: Boolean = false,
    val activeModel: String = "gemini-3.8-flash-low",
    val availableModels: List<ModelInfo> = emptyList(),
    val isModelsLoading: Boolean = false,
    val oauthToken: String = "",
    val workspacePath: String = "",
    val autoApprove: Boolean = true,
    // Shizuku & Root
    val isShizukuInstalled: Boolean = false,
    val isShizukuGranted: Boolean = false,
    val isRootAvailable: Boolean = false,
    val isRootGranted: Boolean = false,
    val systemCommandResult: String? = null,
    // Accounts
    val accounts: List<AccountProfile> = emptyList(),
    val currentTokenSnippet: String = "",
    // Auth URL generator
    val generatedAuthUrl: String? = null,
    val isGeneratingAuthUrl: Boolean = false,
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
        initEngine()
    }

    fun setTab(tab: AppTab) {
        _uiState.update { it.copy(currentTab = tab) }
        if (tab == AppTab.CONTROL_CENTER) {
            checkSystemPrivileges()
            loadAccounts()
        }
    }

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
                _uiState.update { it.copy(usageStats = stats) }
            }
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
        _uiState.update { it.copy(activeModel = newModel) }
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

    fun requestAuthUrl() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingAuthUrl = true, generatedAuthUrl = null) }
            repository.generateAuthUrl().collect { url ->
                _uiState.update { it.copy(generatedAuthUrl = url, isGeneratingAuthUrl = false) }
            }
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

        _uiState.update {
            it.copy(
                isShizukuInstalled = shizukuInstalled,
                isShizukuGranted = shizukuGranted,
                isRootAvailable = rootAvail,
                isRootGranted = rootGranted
            )
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

    fun sendMessage(prompt: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isStreaming = true) }
            repository.sendMessage(prompt)
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
