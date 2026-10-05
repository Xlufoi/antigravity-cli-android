package com.google.antigravity.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.antigravity.ui.chat.components.ChatInputBar
import com.google.antigravity.ui.chat.components.MessageBubble
import com.google.antigravity.ui.theme.AgAccent
import com.google.antigravity.ui.theme.AgBorder
import com.google.antigravity.ui.theme.AgDarkBackground
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
                                text = if (uiState.isEngineReady) "Engine Ready • ${uiState.activeModel}" else "Initializing...",
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
                    IconButton(onClick = { /* Open workspace picker */ }) {
                        Icon(Icons.Default.Folder, contentDescription = "Workspace", tint = AgTextSecondary)
                    }
                    IconButton(onClick = { /* Open settings */ }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = AgTextSecondary)
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
}
