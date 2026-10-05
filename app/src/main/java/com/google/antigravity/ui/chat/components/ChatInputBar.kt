package com.google.antigravity.ui.chat.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.antigravity.ui.theme.*

@Composable
fun ChatInputBar(
    isStreaming: Boolean,
    activeModel: String,
    workspacePath: String,
    autoApprove: Boolean,
    isShizukuActive: Boolean,
    isRootActive: Boolean,
    onSendMessage: (String) -> Unit,
    onStopSession: () -> Unit,
    onModelClick: () -> Unit,
    onWorkspaceClick: () -> Unit,
    onToggleAutoApprove: () -> Unit,
    onAdbClick: () -> Unit,
    onRootClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var textInput by remember { mutableStateOf("") }
    val chipScrollState = rememberScrollState()

    val shortWorkspace = remember(workspacePath) {
        if (workspacePath.isBlank()) "~/workspace"
        else {
            val parts = workspacePath.trimEnd('/').split('/')
            parts.takeLast(2).joinToString("/")
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .background(AgDarkBackground)
    ) {
        // Terminal Divider Line above input (matching Screenshot 1)
        HorizontalDivider(color = AgSeparator, thickness = 1.dp)

        // Main prompt and command input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "> ",
                color = AgTerminalPrompt,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            TextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = {
                    Text(
                        "type prompt or /command...",
                        color = AgTerminalDim,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = AgTextPrimary,
                    unfocusedTextColor = AgTextPrimary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = AgTerminalPrompt
                ),
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(0.dp),
                maxLines = 4
            )

            // Send / Stop button in terminal style
            Text(
                text = if (isStreaming) "[STOP]" else "[SEND]",
                color = if (isStreaming) AgError else if (textInput.isNotBlank()) AgTerminalPrompt else AgTerminalDim,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier
                    .clickable {
                        if (isStreaming) {
                            onStopSession()
                        } else if (textInput.isNotBlank()) {
                            onSendMessage(textInput.trim())
                            textInput = ""
                        }
                    }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }

        // Horizontal line separator (matching Screenshot 1)
        HorizontalDivider(color = AgSeparator, thickness = 1.dp)

        // Status Line: "? for shortcuts" on left, model name on right (EXACT Screenshot 1)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "? for shortcuts",
                color = AgTextSecondary,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
            Text(
                text = activeModel,
                color = AgTerminalPrompt,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                modifier = Modifier.clickable { onModelClick() }
            )
        }

        // Quick Console Chips directly below
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(chipScrollState)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TerminalChip(
                text = if (isRootActive) "[root: ON]" else "[root: OFF]",
                color = if (isRootActive) AgTerminalGreen else AgTextSecondary,
                onClick = onRootClick
            )

            TerminalChip(
                text = if (isShizukuActive) "[adb: ON]" else "[adb: OFF]",
                color = if (isShizukuActive) AgTerminalGreen else AgTextSecondary,
                onClick = onAdbClick
            )

            TerminalChip(
                text = "[$shortWorkspace]",
                color = AgTextSecondary,
                onClick = onWorkspaceClick
            )

            TerminalChip(
                text = "[/plan]",
                color = AgTerminalAmber,
                onClick = {
                    textInput = if (textInput.isBlank()) "/plan " else "$textInput /plan"
                }
            )

            TerminalChip(
                text = "[/boost]",
                color = AgTerminalAmber,
                onClick = {
                    textInput = if (textInput.isBlank()) "/boost " else "$textInput /boost"
                }
            )

            TerminalChip(
                text = "[/model]",
                color = AgTerminalPrompt,
                onClick = onModelClick
            )

            TerminalChip(
                text = "[/stats]",
                color = AgTextSecondary,
                onClick = { onSendMessage("/stats") }
            )

            TerminalChip(
                text = "[/clear]",
                color = AgTextSecondary,
                onClick = { onSendMessage("/clear") }
            )
        }
    }
}

@Composable
private fun TerminalChip(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(2.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            color = color,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}
