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
        if (resetTime.isNotBlank()) {
            try {
                val resetMillis = java.time.Instant.parse(resetTime).toEpochMilli()
                val diffMillis = (resetMillis - System.currentTimeMillis()).coerceAtLeast(0L)
                val hours = diffMillis / 3600000L
                val minutes = (diffMillis % 3600000L) / 60000L
                return "Refreshes in ${hours}h ${minutes}m"
            } catch (_: Exception) {}
        }
        if (description.contains("it will fully refresh in", ignoreCase = true)) {
            val part = description.substringAfter("it will fully refresh in").trim().removeSuffix(".")
            val formatted = part
                .replace(Regex("([0-9]+)\\s*hours?", RegexOption.IGNORE_CASE), "$1h")
                .replace(Regex("([0-9]+)\\s*minutes?", RegexOption.IGNORE_CASE), "$1m")
                .replace(",", "")
                .trim()
            return "Refreshes in $formatted"
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
