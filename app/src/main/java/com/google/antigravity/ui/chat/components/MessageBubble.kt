package com.google.antigravity.ui.chat.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
            .padding(vertical = 4.dp)
    ) {
        if (isUser) {
            // User prompt in bright console blue (matching Screenshot 1)
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "> ",
                    color = AgTerminalPrompt,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = message.text,
                    color = AgTerminalPrompt,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
        } else if (isSystem) {
            // System / shell feedback
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                Text(
                    text = message.text,
                    color = AgTextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                if (!extractedUrl.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(extractedUrl))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        border = BorderStroke(1.dp, AgTerminalPrompt),
                        shape = RoundedCornerShape(2.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AgTerminalPrompt),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "[OPEN IN BROWSER]",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(
                color = AgSeparator,
                thickness = 1.dp,
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            )
        } else {
            // Agent response in crisp white with thinking support (matching Screenshot 1)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                // 1. Thinking block (▸ Thought for 1s ...)
                if (message.thinkingText.isNotBlank()) {
                    val durationText = if (message.thoughtDuration.isNotBlank()) " for ${message.thoughtDuration}" else ""
                    Text(
                        text = "▸ Thought$durationText",
                        color = AgTextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Text(
                        text = "  ${message.thinkingText.trim()}",
                        color = AgTerminalDim,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                // 2. Response block
                if (message.text.isNotBlank()) {
                    val displayText = if (message.isStreaming) {
                        if (cursorAlpha > 0.5f) "${message.text} █" else "${message.text}  "
                    } else {
                        message.text
                    }

                    Text(
                        text = "  ${displayText.trimStart()}",
                        color = AgTextPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                } else if (message.isStreaming && message.thinkingText.isBlank()) {
                    // Immediate terminal cursor when starting response
                    val cursorText = if (cursorAlpha > 0.5f) "  █" else "   "
                    Text(
                        text = cursorText,
                        color = AgTerminalPrompt,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                if (!extractedUrl.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(extractedUrl))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        border = BorderStroke(1.dp, AgTerminalPrompt),
                        shape = RoundedCornerShape(2.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AgTerminalPrompt),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "[OPEN IN BROWSER]",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                message.toolCall?.let { tool ->
                    Spacer(modifier = Modifier.height(6.dp))
                    ToolCallCard(toolCall = tool)
                }
            }

            if (!message.isStreaming) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(
                    color = AgSeparator,
                    thickness = 1.dp,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )
            }
        }
    }
}
