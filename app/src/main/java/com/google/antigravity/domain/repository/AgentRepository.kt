package com.google.antigravity.domain.repository

import com.google.antigravity.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface AgentRepository {
    suspend fun startSession(model: String, oauthToken: String?): Boolean
    suspend fun sendMessage(prompt: String): Boolean
    fun sendInput(input: String): Boolean
    fun importTokenFromDownloads(): Boolean
    fun getMessagesFlow(): Flow<ChatMessage>
    fun stopSession()
}
