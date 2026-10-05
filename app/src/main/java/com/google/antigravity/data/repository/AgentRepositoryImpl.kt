package com.google.antigravity.data.repository

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

class AgentRepositoryImpl : AgentRepository {

    private var processManager: NativeProcessManager? = null
    private val _messagesFlow = MutableSharedFlow<ChatMessage>(replay = 50)
    private val scope = CoroutineScope(Dispatchers.IO)

    override suspend fun startSession(
        binaryPath: String,
        workspacePath: String,
        model: String
    ): Boolean {
        val manager = NativeProcessManager(binaryPath, workspacePath, model)
        val started = manager.startEngine()
        if (started) {
            processManager = manager
            listenToProcessEvents(manager)
        }
        return started
    }

    private fun listenToProcessEvents(manager: NativeProcessManager) {
        scope.launch {
            var currentAgentMessage = ""
            manager.observeEvents().collect { event ->
                when (event.type) {
                    "text", "chunk" -> {
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
                    "tool_use" -> {
                        val tool = ToolCall(
                            name = event.tool_name ?: "Tool",
                            summary = event.tool_summary ?: event.tool_action ?: "Executing action...",
                            status = ToolStatus.RUNNING
                        )
                        _messagesFlow.emit(
                            ChatMessage(
                                sender = MessageSender.AGENT,
                                text = currentAgentMessage,
                                toolCall = tool,
                                isStreaming = true
                            )
                        )
                    }
                    "tool_result" -> {
                        val tool = ToolCall(
                            name = event.tool_name ?: "Tool",
                            summary = event.tool_summary ?: "Completed",
                            status = if (event.is_error == true) ToolStatus.FAILED else ToolStatus.COMPLETED,
                            output = event.tool_output
                        )
                        _messagesFlow.emit(
                            ChatMessage(
                                sender = MessageSender.AGENT,
                                text = currentAgentMessage,
                                toolCall = tool,
                                isStreaming = true
                            )
                        )
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
    }

    override suspend fun sendMessage(prompt: String): Boolean {
        _messagesFlow.emit(
            ChatMessage(
                sender = MessageSender.USER,
                text = prompt,
                isStreaming = false
            )
        )
        return processManager?.sendUserPrompt(prompt) ?: false
    }

    override fun getMessagesFlow(): Flow<ChatMessage> = _messagesFlow.asSharedFlow()

    override fun stopSession() {
        processManager?.stopEngine()
        processManager = null
    }
}
