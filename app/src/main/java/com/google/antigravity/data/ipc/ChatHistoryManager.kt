package com.google.antigravity.data.ipc

import android.content.Context
import com.google.antigravity.domain.model.ChatMessage
import com.google.antigravity.domain.model.ChatSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class ChatHistoryManager(private val context: Context) {
    private val TAG = "ChatHistoryManager"
    private val sessionsDir: File
        get() = File(context.filesDir, "chat_sessions").apply { mkdirs() }

    private val prefs by lazy {
        context.getSharedPreferences("ag_chat_history", Context.MODE_PRIVATE)
    }

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    suspend fun getAllSessions(): List<ChatSession> = withContext(Dispatchers.IO) {
        try {
            val files = sessionsDir.listFiles { file -> file.extension == "json" } ?: emptyArray()
            files.mapNotNull { file ->
                try {
                    json.decodeFromString<ChatSession>(file.readText())
                } catch (e: Exception) {
                    AppLogger.log(TAG, "Error reading session ${file.name}: ${e.message}")
                    null
                }
            }.sortedByDescending { it.updatedAt }
        } catch (e: Exception) {
            AppLogger.log(TAG, "Error getting all sessions: ${e.message}")
            emptyList()
        }
    }

    suspend fun getSession(id: String): ChatSession? = withContext(Dispatchers.IO) {
        try {
            val file = File(sessionsDir, "$id.json")
            if (file.exists()) {
                json.decodeFromString<ChatSession>(file.readText())
            } else null
        } catch (e: Exception) {
            AppLogger.log(TAG, "Error getting session $id: ${e.message}")
            null
        }
    }

    private val writeLock = Any()

    suspend fun saveSession(session: ChatSession) = withContext(Dispatchers.IO) {
        synchronized(writeLock) {
            try {
                val file = File(sessionsDir, "${session.id}.json")
                val tempFile = File(sessionsDir, "${session.id}.tmp")
                tempFile.writeText(json.encodeToString(session))
                if (file.exists()) file.delete()
                tempFile.renameTo(file)
            } catch (e: Exception) {
                AppLogger.log(TAG, "Error saving session ${session.id}: ${e.message}")
            }
        }
    }

    suspend fun deleteSession(id: String) = withContext(Dispatchers.IO) {
        try {
            val file = File(sessionsDir, "$id.json")
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            AppLogger.log(TAG, "Error deleting session $id: ${e.message}")
        }
    }

    fun getLastActiveSessionId(): String? {
        return prefs.getString("last_active_session_id", null)
    }

    fun setLastActiveSessionId(id: String) {
        prefs.edit().putString("last_active_session_id", id).apply()
    }
}
