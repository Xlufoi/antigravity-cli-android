package com.google.antigravity.ui.chat

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.antigravity.data.ipc.AppLogger
import com.google.antigravity.ui.chat.components.ChatInputBar
import com.google.antigravity.ui.chat.components.MessageBubble
import com.google.antigravity.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var showAuthDialog by remember { mutableStateOf(false) }
    var showLogsDialog by remember { mutableStateOf(false) }
    var showModelDialog by remember { mutableStateOf(false) }
    var tokenInput by remember { mutableStateOf("") }

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
                    Column(modifier = Modifier.clickable { showModelDialog = true }) {
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
                                text = if (uiState.isEngineReady) "Engine Ready • ${uiState.activeModel}" else "Engine Initializing...",
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
                    // Model Switcher Button
                    IconButton(onClick = { showModelDialog = true }) {
                        Icon(Icons.Default.Tune, contentDescription = "Select Model", tint = AgTextSecondary)
                    }
                    // Logs Button
                    IconButton(onClick = { showLogsDialog = true }) {
                        Icon(Icons.Default.BugReport, contentDescription = "Logs", tint = AgTextSecondary)
                    }
                    // OAuth Key Button
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

    // Live Logs Dialog
    if (showLogsDialog) {
        val logs by AppLogger.liveLogs.collectAsState()
        val logListState = rememberLazyListState()

        AlertDialog(
            onDismissRequest = { showLogsDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Системный лог", color = AgTextPrimary)
                    IconButton(onClick = {
                        val path = AppLogger.exportToDownloads(context)
                        Toast.makeText(context, "Лог сохранён в Downloads:\n$path", Toast.LENGTH_LONG).show()
                    }) {
                        Icon(Icons.Default.Download, contentDescription = "Export Log", tint = AgPrimary)
                    }
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
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
            },
            confirmButton = {
                Button(onClick = {
                    val path = AppLogger.exportToDownloads(context)
                    Toast.makeText(context, "Лог экспортирован в:\n$path", Toast.LENGTH_LONG).show()
                }) {
                    Text("Экспорт в Downloads")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogsDialog = false }) {
                    Text("Закрыть")
                }
            },
            containerColor = AgSurfaceVariant
        )
    }

    if (showAuthDialog) {
        AlertDialog(
            onDismissRequest = { showAuthDialog = false },
            title = { Text("Авторизация Gemini / OAuth", color = AgTextPrimary) },
            text = {
                Column {
                    // Quick import button
                    OutlinedButton(
                        onClick = {
                            val imported = viewModel.importTokenFromDownloads()
                            if (imported) {
                                Toast.makeText(context, "Токен успешно загружен из Downloads!", Toast.LENGTH_SHORT).show()
                                showAuthDialog = false
                            } else {
                                Toast.makeText(context, "Файл /sdcard/Download/antigravity-oauth-token не найден", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AgAccent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Загрузить токен из Downloads", fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Или вставьте Google OAuth токен (JSON, ya29... или код):",
                        fontSize = 12.sp,
                        color = AgTextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        placeholder = { Text("ya29... или {...} токен", fontSize = 12.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = AgSurfaceVariant,
                            unfocusedContainerColor = AgSurfaceVariant,
                            focusedTextColor = AgTextPrimary,
                            unfocusedTextColor = AgTextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tokenInput.isNotBlank()) {
                            viewModel.updateToken(tokenInput.trim())
                            Toast.makeText(context, "Токен сохранён!", Toast.LENGTH_SHORT).show()
                        }
                        showAuthDialog = false
                    }
                ) {
                    Text("Сохранить")
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

    if (showModelDialog) {
        val models = listOf(
            "gemini-3.8-flash-low" to "Gemini 3.8 Flash (Low ⚡ Моментальный)",
            "gemini-3.8-flash-medium" to "Gemini 3.8 Flash (Medium ⚖️ Баланс)",
            "gemini-3.8-flash-high" to "Gemini 3.8 Flash (High 🧠 Мышление)",
            "claude-sonnet-4-6" to "Claude Sonnet 4.6 (Claude 🎭)"
        )

        AlertDialog(
            onDismissRequest = { showModelDialog = false },
            title = { Text("Выбор модели", color = AgTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    models.forEach { (modelId, label) ->
                        val isSelected = uiState.activeModel == modelId
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) AgPrimary.copy(alpha = 0.2f) else AgSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) AgPrimary else AgBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectModel(modelId)
                                    showModelDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.selectModel(modelId)
                                        showModelDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = AgPrimary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = label,
                                        fontSize = 13.sp,
                                        color = if (isSelected) AgTextPrimary else AgTextSecondary
                                    )
                                    Text(
                                        text = modelId,
                                        fontSize = 11.sp,
                                        color = AgTextSecondary.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showModelDialog = false }) {
                    Text("Закрыть")
                }
            },
            containerColor = AgSurfaceVariant
        )
    }
}
