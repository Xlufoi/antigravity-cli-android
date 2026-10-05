package com.google.antigravity.data.repository

import android.content.Context
import com.google.antigravity.data.ipc.NativeProcessManager
import com.google.antigravity.domain.model.ChatMessage
import com.google.antigravity.domain.model.MessageSender
import com.google.antigravity.domain.repository.AgentRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.UUID

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

        // If process is already waiting for input (e.g. auth code or prompt)
        if (manager.sendInput(prompt)) {
            _messagesFlow.emit(
                ChatMessage(
                    sender = MessageSender.USER,
                    text = prompt,
                    isStreaming = false
                )
            )
            return true
        }

        _messagesFlow.emit(
            ChatMessage(
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
                                    text = "🔗 Требуется авторизация Google:\n$urlText\n\nНажмите кнопку ниже, чтобы открыть в браузере, затем вставьте код авторизации сюда."
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
}
