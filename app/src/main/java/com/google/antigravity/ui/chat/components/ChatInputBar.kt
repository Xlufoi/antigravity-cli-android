package com.google.antigravity.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    onOpenControlCenter: () -> Unit,
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
            .background(AgDarkBackground)
            .border(1.dp, AgBorder, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(top = 8.dp, bottom = 10.dp, start = 10.dp, end = 10.dp)
    ) {
        // Main console command line
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(AgSurfaceVariant)
                .border(1.dp, AgBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "agy ❯",
                color = AgTerminalPrompt,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.padding(end = 4.dp)
            )

            TextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = {
                    Text(
                        "Задайте задачу или команду агенту...",
                        color = AgTextSecondary.copy(alpha = 0.7f),
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
                    unfocusedIndicatorColor = Color.Transparent
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

            IconButton(
                onClick = {
                    if (isStreaming) {
                        onStopSession()
                    } else if (textInput.isNotBlank()) {
                        onSendMessage(textInput.trim())
                        textInput = ""
                    }
                },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isStreaming) AgError else AgPrimary)
            ) {
                Icon(
                    imageVector = if (isStreaming) Icons.Default.Stop else Icons.Default.Send,
                    contentDescription = if (isStreaming) "Stop" else "Send",
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Console Pills Row directly underneath input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(chipScrollState),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Model Chip
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AgTerminalPurple.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, AgTerminalPurple.copy(alpha = 0.4f)),
                modifier = Modifier.clickable { onModelClick() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚡ $activeModel",
                        color = AgTerminalPurple,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = AgTerminalPurple,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // 2. Workspace Chip
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AgSurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, AgBorder),
                modifier = Modifier.clickable { onWorkspaceClick() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        tint = AgTerminalPrompt,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = shortWorkspace,
                        color = AgTextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = AgTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // 3. Permissions Auto-Approve Chip
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (autoApprove) AgAccent.copy(alpha = 0.15f) else AgTerminalAmber.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (autoApprove) AgAccent.copy(alpha = 0.4f) else AgTerminalAmber.copy(alpha = 0.4f)
                ),
                modifier = Modifier.clickable { onToggleAutoApprove() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (autoApprove) Icons.Default.CheckCircle else Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (autoApprove) AgAccent else AgTerminalAmber,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (autoApprove) "auto-approve: ON" else "confirm: ON",
                        color = if (autoApprove) AgAccent else AgTerminalAmber,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }

            // 4. System Privileges Chip (Shizuku / Root)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AgSurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, AgBorder),
                modifier = Modifier.clickable { onOpenControlCenter() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            isRootActive -> "👑 root"
                            isShizukuActive -> "⚡ adb"
                            else -> "🔒 sandboxed"
                        },
                        color = when {
                            isRootActive -> AgTerminalAmber
                            isShizukuActive -> AgTerminalPrompt
                            else -> AgTextSecondary
                        },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
