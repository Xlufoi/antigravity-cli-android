package com.google.antigravity.ui.chat.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.antigravity.domain.model.ChatMessage
import com.google.antigravity.domain.model.MessageSender
import com.google.antigravity.ui.theme.*

@Composable
fun MessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isUser = message.sender == MessageSender.USER
    val isSystem = message.sender == MessageSender.SYSTEM
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bgColor = when {
        isUser -> AgUserBubble
        isSystem -> AgSurfaceVariant
        else -> AgAgentBubble
    }

    val extractedUrl = remember(message.text) {
        val match = Regex("""https?://[^\s]+""").find(message.text)
        match?.value
    }

    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bgColor)
                .border(
                    1.dp,
                    if (isSystem) AgPrimary.copy(alpha = 0.5f) else AgBorder,
                    RoundedCornerShape(12.dp)
                )
                .padding(12.dp)
        ) {
            Column {
                // Console sender header
                val senderTag = when {
                    isUser -> "user ›"
                    isSystem -> "sys ›"
                    else -> "agy ›"
                }
                val senderColor = when {
                    isUser -> AgTerminalGreen
                    isSystem -> AgTerminalAmber
                    else -> AgTerminalPrompt
                }
                Text(
                    text = senderTag,
                    color = senderColor,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                if (message.text.isNotBlank()) {
                    val displayText = if (message.isStreaming && !isUser) {
                        if (cursorAlpha > 0.5f) "${message.text} ▌" else "${message.text}   "
                    } else {
                        message.text
                    }
                    Text(
                        text = displayText,
                        color = AgTextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 19.sp
                    )
                } else if (message.isStreaming && !isUser) {
                    val cursorChar = if (cursorAlpha > 0.5f) "▌" else " "
                    Text(
                        text = cursorChar,
                        color = AgAccent,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }

                if (!extractedUrl.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(extractedUrl))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AgPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Открыть в браузере", fontSize = 13.sp, color = Color.White)
                    }
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

