package com.google.antigravity.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ModelInfo(
    val id: String,
    val displayName: String,
    val isRecommended: Boolean = false,
    val effort: String = "low",
    val description: String = ""
)
