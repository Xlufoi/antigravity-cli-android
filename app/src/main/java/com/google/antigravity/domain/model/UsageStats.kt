package com.google.antigravity.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UsageStats(
    val lastInputTokens: Long = 0,
    val lastOutputTokens: Long = 0,
    val lastThinkingTokens: Long = 0,
    val lastTotalTokens: Long = 0,
    val sessionTotalTokens: Long = 0,
    val turnsCount: Int = 0,
    val contextWindowLimit: Long = 1_000_000L,
    val tierName: String = "Gemini Code Assist (Standard Tier)",
    val isQuotaUnlimited: Boolean = true
) {
    // Current tokens consumed in the active conversation context
    val currentContextTokens: Long
        get() = if (lastTotalTokens > 0) lastTotalTokens else (lastInputTokens + lastOutputTokens)

    val remainingContextTokens: Long
        get() = (contextWindowLimit - currentContextTokens).coerceAtLeast(0L)

    val remainingPercent: Float
        get() = if (contextWindowLimit > 0) {
            ((contextWindowLimit - currentContextTokens).toFloat() / contextWindowLimit.toFloat() * 100f).coerceIn(0f, 100f)
        } else 100f
}
