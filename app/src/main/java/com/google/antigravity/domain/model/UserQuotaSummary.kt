package com.google.antigravity.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class QuotaBucket(
    val bucketId: String = "",
    val displayName: String = "",
    val window: String = "",
    val resetTime: String = "",
    val description: String = "",
    val remainingFraction: Double = 1.0
) {
    val remainingPercent: Float
        get() = (remainingFraction * 100.0).toFloat().coerceIn(0f, 100f)

    fun getRefreshText(): String {
        if (description.contains("it will fully refresh in", ignoreCase = true)) {
            val part = description.substringAfter("it will fully refresh in").trim().removeSuffix(".")
            return "Refreshes in $part"
        }
        return if (window.isNotBlank()) "Refreshes in $window" else ""
    }
}

@Serializable
data class QuotaGroup(
    val displayName: String = "",
    val description: String = "",
    val buckets: List<QuotaBucket> = emptyList()
)

@Serializable
data class UserQuotaSummary(
    val groups: List<QuotaGroup> = emptyList(),
    val description: String = "",
    val lastSyncTime: Long = 0L,
    val isSyncing: Boolean = false,
    val syncError: String? = null
)
