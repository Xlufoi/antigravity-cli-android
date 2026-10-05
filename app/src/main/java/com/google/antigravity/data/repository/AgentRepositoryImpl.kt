package com.google.antigravity.data.repository

import android.content.Context
import com.google.antigravity.data.ipc.NativeProcessManager
import com.google.antigravity.domain.model.ChatMessage
import com.google.antigravity.domain.model.MessageSender
import com.google.antigravity.domain.model.ToolCall
import com.google.antigravity.domain.model.ToolStatus
import com.google.antigravity.domain.repository.AgentRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class AgentRepositoryImpl(private val context: Context) : AgentRepository {

    private var processManager: NativeProcessManager? = null
    private val _messagesFlow = MutableSharedFlow<ChatMessage>(replay = 50)
    private val scope = CoroutineScope(Dispatchers.IO)

    override suspend fun startSession(
        model: String,
        oauthToken: String?
    ): Boolean {
        val manager = NativeProcessManager(
            context = context,
            model = model,
            oauthToken = oauthToken
        )
        val started = manager.startEngine()
        if (started) {
            processManager = manager
        }
        return started
    }

    override suspend fun sendMessage(prompt: String): Boolean {
        val manager = processManager ?: return false
        _messagesFlow.emit(
            ChatMessage(
                sender = MessageSender.USER,
                text = prompt,
                isStreaming = false
            )
        )

        scope.launch {
            var currentAgentMessage = ""
            manager.executePrompt(prompt).collect { event ->
                when (event.type) {
                    "auth_url" -> {
                        event.text?.let { urlText ->
                            _messagesFlow.emit(
                                ChatMessage(
                                    sender = MessageSender.SYSTEM,
                                    text = "🔗 Для входа перейдите по ссылке авторизации:\n\n$urlText"
                                )
                            )
                        }
                    }
                    "chunk" -> {
                        event.text?.let { chunk ->
                            currentAgentMessage += chunk
                            _messagesFlow.emit(
                                ChatMessage(
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
                                sender = MessageSender.AGENT,
                                text = currentAgentMessage,
                                isStreaming = false
                            )
                        )
                        currentAgentMessage = ""
                    }
                }
            }
        }
        return true
    }

    override fun getMessagesFlow(): Flow<ChatMessage> = _messagesFlow.asSharedFlow()

    override fun stopSession() {
        processManager?.stopEngine()
    }
}
