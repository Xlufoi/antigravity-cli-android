package com.google.antigravity.ui.chat

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.antigravity.data.ipc.AppLogger
import com.google.antigravity.ui.chat.components.ChatInputBar
import com.google.antigravity.ui.chat.components.MessageBubble
import com.google.antigravity.ui.chat.components.ModelSelectorDialog
import com.google.antigravity.ui.chat.components.WorkspacePickerDialog
import com.google.antigravity.ui.control.ControlCenterScreen
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
    val logListState = rememberLazyListState()

    var showModelDialog by remember { mutableStateOf(false) }
    var showWorkspaceDialog by remember { mutableStateOf(false) }

    // Auto-scroll when messages change or new streaming tokens arrive
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
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "agy",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = AgTerminalPrompt
                        )
                        Text(
                            text = "::mobile",
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            color = AgTextSecondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(
                                    if (uiState.isEngineReady) AgAccent else AgTerminalAmber,
                                    shape = CircleShape
                                )
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AgDarkBackground
                ),
                actions = {
                    // Segmented Terminal Tabs in Top Bar
                    Row(
                        modifier = Modifier.padding(end = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Console Tab
                        FilterChip(
                            selected = uiState.currentTab == AppTab.CONSOLE,
                            onClick = { viewModel.setTab(AppTab.CONSOLE) },
                            label = { Text(">_ CONSOLE", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AgTerminalPrompt.copy(alpha = 0.2f),
                                selectedLabelColor = AgTerminalPrompt,
                                containerColor = Color.Transparent,
                                labelColor = AgTextSecondary
                            ),
                            border = BorderStroke(1.dp, if (uiState.currentTab == AppTab.CONSOLE) AgTerminalPrompt else AgBorder)
                        )

                        // Control Center Tab
                        FilterChip(
                            selected = uiState.currentTab == AppTab.CONTROL_CENTER,
                            onClick = { viewModel.setTab(AppTab.CONTROL_CENTER) },
                            label = { Text("CONTROL", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AgTerminalPurple.copy(alpha = 0.2f),
                                selectedLabelColor = AgTerminalPurple,
                                containerColor = Color.Transparent,
                                labelColor = AgTextSecondary
                            ),
                            border = BorderStroke(1.dp, if (uiState.currentTab == AppTab.CONTROL_CENTER) AgTerminalPurple else AgBorder)
                        )

                        // Logs Tab
                        FilterChip(
                            selected = uiState.currentTab == AppTab.LOGS,
                            onClick = { viewModel.setTab(AppTab.LOGS) },
                            label = { Text("LOGS", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AgAccent.copy(alpha = 0.2f),
                                selectedLabelColor = AgAccent,
                                containerColor = Color.Transparent,
                                labelColor = AgTextSecondary
                            ),
                            border = BorderStroke(1.dp, if (uiState.currentTab == AppTab.LOGS) AgAccent else AgBorder)
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (uiState.currentTab == AppTab.CONSOLE) {
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
                    onOpenControlCenter = { viewModel.setTab(AppTab.CONTROL_CENTER) }
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
        ) {
            when (uiState.currentTab) {
                AppTab.CONSOLE -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            // Terminal welcome banner
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AgSurfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, AgBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "Google Antigravity CLI (va39) • On-Device Engine",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = AgTerminalPrompt
                                    )
                                    Text(
                                        text = "Рабочая директория: ${uiState.workspacePath}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = AgTextSecondary
                                    )
                                    Text(
                                        text = "Модель: ${uiState.activeModel} • auto-approve: ${if (uiState.autoApprove) "ON" else "OFF"}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = AgTerminalPurple
                                    )
                                }
                            }
                        }

                        items(uiState.messages, key = { it.id }) { message ->
                            MessageBubble(message = message)
                        }

                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }

                AppTab.CONTROL_CENTER -> {
                    ControlCenterScreen(
                        viewModel = viewModel,
                        uiState = uiState
                    )
                }

                AppTab.LOGS -> {
                    val logs by AppLogger.liveLogs.collectAsState()

                    LaunchedEffect(logs.size) {
                        if (logs.isNotEmpty()) {
                            logListState.scrollToItem(logs.size - 1)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Системный лог ядра (Live)",
                                color = AgTextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = {
                                    val path = AppLogger.exportToDownloads(context)
                                    Toast.makeText(context, "Экспортировано в:\n$path", Toast.LENGTH_LONG).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AgPrimary),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Экспорт в Downloads", fontSize = 11.sp, color = Color.Black)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.8f))
                                .border(1.dp, AgBorder, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            LazyColumn(state = logListState) {
                                items(logs) { line ->
                                    Text(
                                        text = line,
                                        color = if (line.contains("ERROR")) AgError else AgTextSecondary,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dynamic Server Model Selector Dialog
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

    // Workspace Folder Picker Dialog
    if (showWorkspaceDialog) {
        WorkspacePickerDialog(
            currentPath = uiState.workspacePath,
            onSelectPath = { viewModel.updateWorkspace(it) },
            onDismiss = { showWorkspaceDialog = false }
        )
    }
}
