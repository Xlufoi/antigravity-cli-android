package com.google.antigravity.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.antigravity.domain.model.ChatMessage
import com.google.antigravity.domain.model.MessageSender
import com.google.antigravity.ui.theme.AgAgentBubble
import com.google.antigravity.ui.theme.AgBorder
import com.google.antigravity.ui.theme.AgTextPrimary
import com.google.antigravity.ui.theme.AgUserBubble

@Composable
fun MessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val isUser = message.sender == MessageSender.USER
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bgColor = if (isUser) AgUserBubble else AgAgentBubble

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bgColor)
                .border(1.dp, AgBorder, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Column {
                if (message.text.isNotBlank()) {
                    Text(
                        text = message.text,
                        color = AgTextPrimary,
                        fontSize = 15.sp,
                        lineHeight = 20.sp
                    )
                }

                message.toolCall?.let { tool ->
                    if (message.text.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    ToolCallCard(toolCall = tool)
                }
            }
        }
    }
}
