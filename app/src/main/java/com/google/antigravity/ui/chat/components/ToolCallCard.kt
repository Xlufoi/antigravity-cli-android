package com.google.antigravity.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.antigravity.domain.model.ToolCall
import com.google.antigravity.domain.model.ToolStatus
import com.google.antigravity.ui.theme.*

@Composable
fun ToolCallCard(
    toolCall: ToolCall,
    modifier: Modifier = Modifier
) {
    val statusText = when (toolCall.status) {
        ToolStatus.RUNNING, ToolStatus.PENDING -> "[RUNNING]"
        ToolStatus.COMPLETED -> "[OK]"
        ToolStatus.FAILED -> "[FAILED]"
    }
    val statusColor = when (toolCall.status) {
        ToolStatus.RUNNING, ToolStatus.PENDING -> AgTerminalAmber
        ToolStatus.COMPLETED -> AgTerminalGreen
        ToolStatus.FAILED -> AgError
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "▸ ",
                    color = AgTextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
                Text(
                    text = "[tool: ${toolCall.name}]",
                    color = AgTerminalPrompt,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Text(
                text = statusText,
                color = statusColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }

        if (toolCall.summary.isNotBlank()) {
            Text(
                text = "  ${toolCall.summary}",
                color = AgTextSecondary,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        toolCall.output?.let { output ->
            if (output.isNotBlank()) {
                val cleanOutput = output.take(800) + if (output.length > 800) "\n... [truncated]" else ""
                Text(
                    text = "└  $cleanOutput",
                    color = AgTerminalDim,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 2.dp, start = 4.dp)
                )
            }
        }
    }
}
