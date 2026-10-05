package com.google.antigravity.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.antigravity.ui.theme.AgPrimary
import com.google.antigravity.ui.theme.AgSurfaceVariant
import com.google.antigravity.ui.theme.AgTextPrimary
import com.google.antigravity.ui.theme.AgTextSecondary

@Composable
fun ChatInputBar(
    isStreaming: Boolean,
    onSendMessage: (String) -> Unit,
    onStopSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    var textInput by remember { mutableStateOf("") }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = textInput,
            onValueChange = { textInput = it },
            placeholder = {
                Text("Спросите Antigravity или дайте задачу...", color = AgTextSecondary, fontSize = 14.sp)
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = AgSurfaceVariant,
                unfocusedContainerColor = AgSurfaceVariant,
                focusedTextColor = AgTextPrimary,
                unfocusedTextColor = AgTextPrimary,
                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
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
                .size(44.dp)
                .clip(CircleShape)
                .background(AgPrimary)
        ) {
            Icon(
                imageVector = if (isStreaming) Icons.Default.Stop else Icons.Default.Send,
                contentDescription = if (isStreaming) "Stop" else "Send",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
