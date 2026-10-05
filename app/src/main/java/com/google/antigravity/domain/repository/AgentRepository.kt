package com.google.antigravity.domain.repository

import com.google.antigravity.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface AgentRepository {
    suspend fun startSession(binaryPath: String, workspacePath: String, model: String): Boolean
    suspend fun sendMessage(prompt: String): Boolean
    fun getMessagesFlow(): Flow<ChatMessage>
    fun stopSession()
}
