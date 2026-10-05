package com.google.antigravity.data.ipc

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

@Serializable
data class AccountProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val token: String,
    val isCurrent: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

class AccountManager(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val geminiDir = File(context.filesDir, ".gemini/antigravity-cli")
    private val accountsFile: File
        get() = File(geminiDir, "accounts.json")
    private val tokenFile: File
        get() = File(geminiDir, "antigravity-oauth-token")

    companion object {
        private const val TAG = "AccountManager"
    }

    init {
        geminiDir.mkdirs()
    }

    fun getAccounts(): List<AccountProfile> {
        return try {
            if (!accountsFile.exists()) emptyList()
            else json.decodeFromString<List<AccountProfile>>(accountsFile.readText())
        } catch (e: Exception) {
            AppLogger.log(TAG, "Failed to read accounts: ${e.message}")
            emptyList()
        }
    }

    fun getCurrentTokenSnippet(): String {
        return try {
            if (!tokenFile.exists()) return "Нет активного токена"
            val content = tokenFile.readText().trim()
            if (content.length <= 20) content
            else "${content.take(10)}...${content.takeLast(6)}"
        } catch (_: Exception) {
            "Ошибка чтения"
        }
    }

    fun saveAccount(name: String, rawToken: String, makeActive: Boolean = true): AccountProfile {
        val current = getAccounts().toMutableList()
        val trimmedToken = rawToken.trim()
        val formattedToken = if (trimmedToken.startsWith("{")) {
            trimmedToken
        } else {
            """{
  "token": {
    "access_token": "$trimmedToken",
    "token_type": "Bearer",
    "refresh_token": "",
    "expiry": "2099-01-01T00:00:00Z"
  },
  "auth_method": "consumer"
}"""
        }

        val existingIndex = current.indexOfFirst { it.name.equals(name, ignoreCase = true) }
        val id = if (existingIndex != -1) current[existingIndex].id else UUID.randomUUID().toString()
        val newProfile = AccountProfile(
            id = id,
            name = name,
            token = formattedToken,
            isCurrent = makeActive
        )

        val updatedList = current.map {
            if (makeActive) it.copy(isCurrent = false) else it
        }.toMutableList()

        if (existingIndex != -1) {
            updatedList[existingIndex] = newProfile
        } else {
            updatedList.add(newProfile)
        }

        accountsFile.writeText(json.encodeToString(updatedList))

        if (makeActive) {
            tokenFile.writeText(formattedToken)
            AppLogger.log(TAG, "Activated account '$name' (${formattedToken.length} bytes)")
        }

        return newProfile
    }

    fun switchAccount(id: String): Boolean {
        val current = getAccounts()
        val target = current.find { it.id == id } ?: return false

        val updated = current.map { it.copy(isCurrent = it.id == id) }
        accountsFile.writeText(json.encodeToString(updated))
        tokenFile.writeText(target.token)
        AppLogger.log(TAG, "Switched active account to '${target.name}'")
        return true
    }

    fun deleteAccount(id: String) {
        val current = getAccounts()
        val target = current.find { it.id == id }
        val updated = current.filterNot { it.id == id }
        accountsFile.writeText(json.encodeToString(updated))

        if (target?.isCurrent == true) {
            if (updated.isNotEmpty()) {
                switchAccount(updated.first().id)
            } else {
                tokenFile.delete()
            }
        }
        AppLogger.log(TAG, "Deleted account ID: $id")
    }
}
