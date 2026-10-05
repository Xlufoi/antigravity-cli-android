package com.google.antigravity.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class AgpStreamMessage(
    val type: String,
    val text: String? = null,
    val tool_name: String? = null,
    val tool_action: String? = null,
    val tool_summary: String? = null,
    val tool_args: Map<String, JsonElement>? = null,
    val tool_output: String? = null,
    val is_error: Boolean? = null,
    val done: Boolean? = null,
    val thought: String? = null,
    val thought_duration: String? = null
)
