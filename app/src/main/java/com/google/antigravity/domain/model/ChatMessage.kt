package com.google.antigravity.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class MessageSender {
    USER,
    AGENT,
    SYSTEM
}

@Serializable
enum class ToolStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED
}

@Serializable
data class ToolCall(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val summary: String,
    val arguments: Map<String, String> = emptyMap(),
    val status: ToolStatus = ToolStatus.RUNNING,
    val output: String? = null
)

@Serializable
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val toolCall: ToolCall? = null,
    val isStreaming: Boolean = false
)
