package com.google.antigravity.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.antigravity.domain.model.ChatMessage
import com.google.antigravity.domain.repository.AgentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isStreaming: Boolean = false,
    val isEngineReady: Boolean = false,
    val activeModel: String = "Gemini 3.8 Flash (High)",
    val oauthToken: String = ""
)

class ChatViewModel(
    private val repository: AgentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        observeMessages()
        initEngine()
    }

    fun initEngine(token: String? = null) {
        viewModelScope.launch {
            val tokenToUse = token ?: _uiState.value.oauthToken.ifBlank { null }
            val success = repository.startSession(
                model = _uiState.value.activeModel,
                oauthToken = tokenToUse
            )
            _uiState.update { it.copy(isEngineReady = success) }
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

    fun stopSession() {
        repository.stopSession()
        _uiState.update { it.copy(isStreaming = false) }
    }
}
