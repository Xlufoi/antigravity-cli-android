package com.google.antigravity.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Новый диалог",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val messages: List<ChatMessage> = emptyList(),
    val model: String = "gemini-3.8-flash-low",
    val workspace: String = "~/workspace"
)
