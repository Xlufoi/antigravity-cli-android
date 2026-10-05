package com.google.antigravity.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.antigravity.ui.chat.components.ChatInputBar
import com.google.antigravity.ui.chat.components.MessageBubble
import com.google.antigravity.ui.theme.AgAccent
import com.google.antigravity.ui.theme.AgDarkBackground
import com.google.antigravity.ui.theme.AgSurfaceVariant
import com.google.antigravity.ui.theme.AgTextPrimary
import com.google.antigravity.ui.theme.AgTextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var showAuthDialog by remember { mutableStateOf(false) }
    var tokenInput by remember { mutableStateOf("") }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(uiState.messages.size - 1)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Antigravity Mobile", fontSize = 18.sp, color = AgTextPrimary)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (uiState.isEngineReady) AgAccent else AgTextSecondary,
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (uiState.isEngineReady) "Engine Ready • ${uiState.activeModel}" else "Initializing Engine...",
                                fontSize = 11.sp,
                                color = AgTextSecondary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AgDarkBackground
                ),
                actions = {
                    IconButton(onClick = { showAuthDialog = true }) {
                        Icon(Icons.Default.Key, contentDescription = "OAuth Token", tint = AgTextSecondary)
                    }
                }
            )
        },
        bottomBar = {
            ChatInputBar(
                isStreaming = uiState.isStreaming,
                onSendMessage = { viewModel.sendMessage(it) },
                onStopSession = { viewModel.stopSession() }
            )
        },
        containerColor = AgDarkBackground,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.messages, key = { it.id }) { message ->
                MessageBubble(message = message)
            }
        }
    }

    if (showAuthDialog) {
        AlertDialog(
            onDismissRequest = { showAuthDialog = false },
            title = { Text("Авторизация Gemini / OAuth", color = AgTextPrimary) },
            text = {
                Column {
                    Text(
                        "Вставьте Google OAuth токен или Gemini API токен:",
                        fontSize = 13.sp,
                        color = AgTextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        placeholder = { Text("ya29... или AIza...", fontSize = 12.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = AgSurfaceVariant,
                            unfocusedContainerColor = AgSurfaceVariant,
                            focusedTextColor = AgTextPrimary,
                            unfocusedTextColor = AgTextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tokenInput.isNotBlank()) {
                            viewModel.updateToken(tokenInput.trim())
                        }
                        showAuthDialog = false
                    }
                ) {
                    Text("Сохранить и подключить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAuthDialog = false }) {
                    Text("Отмена")
                }
            },
            containerColor = AgSurfaceVariant
        )
    }
}
