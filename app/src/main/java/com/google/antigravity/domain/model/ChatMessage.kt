package com.google.antigravity.domain.model

import java.util.UUID

enum class MessageSender {
    USER,
    AGENT,
    SYSTEM
}

enum class ToolStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED
}

data class ToolCall(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val summary: String,
    val arguments: Map<String, String> = emptyMap(),
    val status: ToolStatus = ToolStatus.RUNNING,
    val output: String? = null
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val toolCall: ToolCall? = null,
    val isStreaming: Boolean = false
)
