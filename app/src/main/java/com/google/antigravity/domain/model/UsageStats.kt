package com.google.antigravity.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UsageStats(
    val lastInputTokens: Long = 0,
    val lastOutputTokens: Long = 0,
    val lastThinkingTokens: Long = 0,
    val lastTotalTokens: Long = 0,
    val sessionTotalTokens: Long = 0,
    val turnsCount: Int = 0
)
