package com.google.antigravity.ui.chat

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.google.antigravity.ui.chat.components.ChatInputBar
import com.google.antigravity.ui.chat.components.MessageBubble
import com.google.antigravity.ui.chat.components.ModelSelectorDialog
import com.google.antigravity.ui.chat.components.WorkspacePickerDialog
import com.google.antigravity.ui.control.ControlCenterScreen
import com.google.antigravity.ui.control.SettingsSubTab
import com.google.antigravity.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    var showSettings by remember { mutableStateOf(false) }
    var initialSettingsTab by remember { mutableStateOf(SettingsSubTab.ACCOUNTS) }
    var showModelDialog by remember { mutableStateOf(false) }
    var showWorkspaceDialog by remember { mutableStateOf(false) }

    // Auto-scroll when messages change or streaming updates arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    LaunchedEffect(uiState.messages.lastOrNull()?.text) {
        if (uiState.messages.isNotEmpty()) {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            if (lastVisibleIndex >= uiState.messages.size - 2) {
                listState.scrollToItem(uiState.messages.size - 1)
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    // Minimalist top-left SETTINGS button requested by user
                    OutlinedButton(
                        onClick = {
                            initialSettingsTab = SettingsSubTab.ACCOUNTS
                            showSettings = !showSettings
                        },
                        border = BorderStroke(1.dp, if (showSettings) AgTerminalGreen else AgTerminalPrompt),
                        shape = RoundedCornerShape(2.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (showSettings) AgTerminalGreen else AgTerminalPrompt
                        ),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = if (showSettings) "[CHAT]" else "[SETTINGS]",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "antigravity",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = AgTerminalPrompt
                        )
                        Text(
                            text = "::cli",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = AgTextSecondary
                        )
                    }
                },
                actions = {
                    // Quick "+ NEW" chat button in top right
                    OutlinedButton(
                        onClick = {
                            viewModel.createNewChat()
                            showSettings = false
                        },
                        border = BorderStroke(1.dp, AgBorder),
                        shape = RoundedCornerShape(2.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AgTextSecondary),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "[+ NEW]",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AgDarkBackground
                )
            )
        },
        bottomBar = {
            if (!showSettings) {
                ChatInputBar(
                    isStreaming = uiState.isStreaming,
                    activeModel = uiState.activeModel,
                    workspacePath = uiState.workspacePath,
                    autoApprove = uiState.autoApprove,
                    isShizukuActive = uiState.isShizukuGranted,
                    isRootActive = uiState.isRootGranted,
                    onSendMessage = { viewModel.sendMessage(it) },
                    onStopSession = { viewModel.stopSession() },
                    onModelClick = { showModelDialog = true },
                    onWorkspaceClick = { showWorkspaceDialog = true },
                    onToggleAutoApprove = { viewModel.toggleAutoApprove() },
                    onAdbClick = {
                        initialSettingsTab = SettingsSubTab.ADVANCED
                        showSettings = true
                    },
                    onRootClick = {
                        initialSettingsTab = SettingsSubTab.ADVANCED
                        showSettings = true
                    }
                )
            }
        },
        containerColor = AgDarkBackground,
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(AgDarkBackground)
        ) {
            if (showSettings) {
                // Settings view with Accounts, History, and Advanced tabs
                ControlCenterScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    initialTab = initialSettingsTab,
                    onCloseSettings = { showSettings = false },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Pure Terminal Stream matching Screenshot 1
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item {
                        // Terminal Banner at start of log
                        val activeSession = uiState.sessions.find { it.id == uiState.currentSessionId }
                        val sessionTitle = activeSession?.title ?: "New Session"

                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(
                                text = "Google Antigravity Mobile CLI [v3.0]",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = AgTerminalPrompt
                            )
                            Text(
                                text = "Session: $sessionTitle",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = AgTextSecondary
                            )
                            Text(
                                text = "Model: ${uiState.activeModel} | auto-approve: ${if (uiState.autoApprove) "ON" else "OFF"}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = AgTextSecondary
                            )
                            Text(
                                text = "Dir: ${uiState.workspacePath}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = AgTerminalDim
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(color = AgSeparator, thickness = 1.dp)
                        }
                    }

                    items(uiState.messages, key = { it.id }) { message ->
                        MessageBubble(message = message)
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    if (showModelDialog) {
        ModelSelectorDialog(
            currentModel = uiState.activeModel,
            availableModels = uiState.availableModels,
            isLoading = uiState.isModelsLoading,
            onSelectModel = { viewModel.selectModel(it) },
            onRefresh = { viewModel.loadModels(forceRefresh = true) },
            onDismiss = { showModelDialog = false }
        )
    }

    if (showWorkspaceDialog) {
        WorkspacePickerDialog(
            currentPath = uiState.workspacePath,
            onSelectPath = {
                viewModel.updateWorkspace(it)
                showWorkspaceDialog = false
            },
            onDismiss = { showWorkspaceDialog = false }
        )
    }
}
