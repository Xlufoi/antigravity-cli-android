package com.google.antigravity.ui.control

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.google.antigravity.ui.chat.ChatUiState
import com.google.antigravity.ui.chat.ChatViewModel
import com.google.antigravity.ui.chat.components.WorkspacePickerDialog
import com.google.antigravity.ui.theme.*

@Composable
fun ControlCenterScreen(
    viewModel: ChatViewModel,
    uiState: ChatUiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showWorkspaceDialog by remember { mutableStateOf(false) }
    var newAccountName by remember { mutableStateOf("") }
    var newAccountToken by remember { mutableStateOf("") }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var tokenInput by remember { mutableStateOf("") }

    if (uiState.showOAuthWebView && uiState.generatedAuthUrl != null) {
        OAuthWebViewDialog(
            authUrl = uiState.generatedAuthUrl,
            onAuthCodeReceived = { code ->
                viewModel.submitAuthCode(code) { ok ->
                    if (ok) {
                        Toast.makeText(context, "Авторизация успешно завершена!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Не удалось завершить авторизацию", Toast.LENGTH_LONG).show()
                    }
                }
            },
            onDismiss = { viewModel.closeOAuthWebView() }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AgDarkBackground)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- SECTION 1: GOOGLE OAUTH LOGIN URL GENERATOR ---
        Card(
            colors = CardDefaults.cardColors(containerColor = AgSurface),
            border = BorderStroke(1.dp, AgBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = AgTerminalPrompt, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Авторизация Google OAuth", color = AgTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                Text(
                    "Сгенерируйте ссылку для входа через ваш браузер или используйте сохраненный токен:",
                    color = AgTextSecondary,
                    fontSize = 12.sp
                )

                // Generate Auth URL button
                Button(
                    onClick = { viewModel.requestAuthUrl() },
                    enabled = !uiState.isGeneratingAuthUrl,
                    colors = ButtonDefaults.buttonColors(containerColor = AgTerminalPrompt),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isGeneratingAuthUrl) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Генерация ссылки...", color = Color.Black, fontSize = 13.sp)
                    } else {
                        Icon(Icons.Default.Link, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Сгенерировать ссылку для входа", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Display generated URL
                uiState.generatedAuthUrl?.let { url ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AgSurfaceVariant,
                        border = BorderStroke(1.dp, AgPrimary.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Ссылка для авторизации:", color = AgTerminalPrompt, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Text(
                                text = url,
                                color = AgTextPrimary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 3
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.openOAuthWebView() },
                                    colors = ButtonDefaults.buttonColors(containerColor = AgAccent),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Language, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("В приложении", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Ошибка открытия браузера: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AgPrimary),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Браузер", fontSize = 11.sp, color = Color.Black)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Auth URL", url))
                                        Toast.makeText(context, "Ссылка скопирована в буфер!", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AgTerminalPrompt)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // Direct Token / Code Input
                TextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it },
                    placeholder = { Text("Вставьте код (4/0A...) или ссылку с кодом...", fontSize = 12.sp, color = AgTextSecondary) },
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = AgSurfaceVariant,
                        unfocusedContainerColor = AgSurfaceVariant,
                        focusedTextColor = AgTextPrimary,
                        unfocusedTextColor = AgTextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (tokenInput.isNotBlank()) {
                                val input = tokenInput.trim()
                                if (input.startsWith("{") || input.contains("refresh_token") || input.contains("access_token")) {
                                    viewModel.saveAccount("Основной токен", input)
                                    Toast.makeText(context, "Токен сохранён и активирован!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Выполняется вход через Google...", Toast.LENGTH_SHORT).show()
                                    viewModel.submitAuthCode(input) { success ->
                                        if (success) {
                                            Toast.makeText(context, "Успешная авторизация Google!", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Не удалось обменять код на токен. Попробуйте сгенерировать ссылку заново.", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                                tokenInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AgAccent),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Отправить код / токен", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            val success = viewModel.importTokenFromDownloads()
                            if (success) {
                                Toast.makeText(context, "Токен загружен из Downloads!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Файл не найден в Downloads", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AgTerminalPrompt)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Downloads", fontSize = 12.sp)
                    }
                }
            }
        }

        // --- SECTION 2: ACCOUNT MANAGER ---
        Card(
            colors = CardDefaults.cardColors(containerColor = AgSurface),
            border = BorderStroke(1.dp, AgBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, tint = AgTerminalPurple, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Менеджер аккаунтов", color = AgTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }

                    IconButton(onClick = { showAddAccountDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Добавить аккаунт", tint = AgPrimary)
                    }
                }

                Text(
                    text = "Текущий активный токен: ${uiState.currentTokenSnippet}",
                    color = AgTerminalPrompt,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                if (uiState.accounts.isEmpty()) {
                    Text("Нет сохранённых профилей. Добавьте токен выше.", color = AgTextSecondary, fontSize = 12.sp)
                } else {
                    uiState.accounts.forEach { account ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (account.isCurrent) AgTerminalPurple.copy(alpha = 0.15f) else AgSurfaceVariant,
                            border = BorderStroke(1.dp, if (account.isCurrent) AgTerminalPurple else AgBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.switchAccount(account.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = account.name, color = AgTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        if (account.isCurrent) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = AgTerminalPurple.copy(alpha = 0.3f)
                                            ) {
                                                Text("ACTIVE", color = AgTerminalPurple, fontSize = 9.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${account.token.take(15)}...",
                                        color = AgTextSecondary,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Row {
                                    if (!account.isCurrent) {
                                        IconButton(onClick = { viewModel.switchAccount(account.id) }) {
                                            Icon(Icons.Default.Check, contentDescription = "Активировать", tint = AgAccent, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    IconButton(onClick = { viewModel.deleteAccount(account.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = AgError, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- SECTION 3: TOKEN LIMITS & USAGE STATS ---
        Card(
            colors = CardDefaults.cardColors(containerColor = AgSurface),
            border = BorderStroke(1.dp, AgBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.QueryStats, contentDescription = null, tint = AgAccent, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Использование и лимиты токенов", color = AgTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Turn tokens
                    Surface(shape = RoundedCornerShape(8.dp), color = AgSurfaceVariant, modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("ПОСЛЕДНИЙ ЗАПРОС", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = AgTextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${uiState.usageStats.lastTotalTokens}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AgTerminalPrompt, fontFamily = FontFamily.Monospace)
                            Text("in: ${uiState.usageStats.lastInputTokens} | out: ${uiState.usageStats.lastOutputTokens}", fontSize = 9.sp, color = AgTextSecondary)
                        }
                    }

                    // Session total
                    Surface(shape = RoundedCornerShape(8.dp), color = AgSurfaceVariant, modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("ВСЕГО ЗА СЕССИЮ", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = AgTextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${uiState.usageStats.sessionTotalTokens}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AgAccent, fontFamily = FontFamily.Monospace)
                            Text("ходов: ${uiState.usageStats.turnsCount}", fontSize = 9.sp, color = AgTextSecondary)
                        }
                    }
                }

                Text(
                    "Лимит контекста Gemini 3.8: 1 000 000 токенов (квота обновляется серверами Google).",
                    color = AgTextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        // --- SECTION 4: COMMAND PERMISSION CONFIRMATION ---
        Card(
            colors = CardDefaults.cardColors(containerColor = AgSurface),
            border = BorderStroke(1.dp, AgBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Авто-подтверждение команд",
                            color = AgTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (uiState.autoApprove) "--dangerously-skip-permissions ВКЛ (быстро)" else "Спрашивать подтверждение перед действиями",
                            color = if (uiState.autoApprove) AgAccent else AgTerminalAmber,
                            fontSize = 11.sp
                        )
                    }

                    Switch(
                        checked = uiState.autoApprove,
                        onCheckedChange = { viewModel.toggleAutoApprove() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = AgAccent
                        )
                    )
                }
            }
        }

        // --- SECTION 5: SHIZUKU (ADB PRIVILEGES) ---
        Card(
            colors = CardDefaults.cardColors(containerColor = AgSurface),
            border = BorderStroke(1.dp, AgBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Terminal, contentDescription = null, tint = AgTerminalPrompt, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ADB доступ через Shizuku", color = AgTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when {
                            uiState.isShizukuGranted -> AgAccent.copy(alpha = 0.2f)
                            uiState.isShizukuInstalled -> AgTerminalAmber.copy(alpha = 0.2f)
                            else -> AgError.copy(alpha = 0.2f)
                        }
                    ) {
                        Text(
                            text = when {
                                uiState.isShizukuGranted -> "GRANTED"
                                uiState.isShizukuInstalled -> "INSTALLED"
                                else -> "NOT FOUND"
                            },
                            color = when {
                                uiState.isShizukuGranted -> AgAccent
                                uiState.isShizukuInstalled -> AgTerminalAmber
                                else -> AgError
                            },
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    "Позволяет агенту выполнять системные ADB команды без root-доступа через Shizuku сервис.",
                    color = AgTextSecondary,
                    fontSize = 11.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (uiState.isShizukuInstalled && !uiState.isShizukuGranted) {
                        Button(
                            onClick = { viewModel.requestShizukuPermission() },
                            colors = ButtonDefaults.buttonColors(containerColor = AgTerminalPrompt),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Запросить доступ Shizuku", fontSize = 12.sp, color = Color.Black)
                        }
                    }

                    if (uiState.isShizukuGranted) {
                        OutlinedButton(
                            onClick = { viewModel.testShizukuCommand() },
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AgTerminalPrompt),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Тест ADB команды", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // --- SECTION 6: ROOT ACCESS (su) ---
        Card(
            colors = CardDefaults.cardColors(containerColor = AgSurface),
            border = BorderStroke(1.dp, AgBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = AgTerminalAmber, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Root доступ (su)", color = AgTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when {
                            uiState.isRootGranted -> AgAccent.copy(alpha = 0.2f)
                            uiState.isRootAvailable -> AgTerminalAmber.copy(alpha = 0.2f)
                            else -> AgBorder
                        }
                    ) {
                        Text(
                            text = when {
                                uiState.isRootGranted -> "ROOT GRANTED"
                                uiState.isRootAvailable -> "AVAILABLE"
                                else -> "NO ROOT"
                            },
                            color = when {
                                uiState.isRootGranted -> AgAccent
                                uiState.isRootAvailable -> AgTerminalAmber
                                else -> AgTextSecondary
                            },
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    "Прямой root-доступ для выполнения низкоуровневых команд в системе Android.",
                    color = AgTextSecondary,
                    fontSize = 11.sp
                )

                if (uiState.isRootAvailable) {
                    Button(
                        onClick = { viewModel.testRootCommand() },
                        colors = ButtonDefaults.buttonColors(containerColor = AgTerminalAmber),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Тест Root доступа (id)", fontSize = 12.sp, color = Color.Black)
                    }
                }

                uiState.systemCommandResult?.let { res ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, AgBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = res,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = AgTerminalPrompt,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }

        // --- SECTION 7: STORAGE ACCESS ---
        Card(
            colors = CardDefaults.cardColors(containerColor = AgSurface),
            border = BorderStroke(1.dp, AgBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = AgTerminalPrompt, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Доступ к памяти (Все файлы)", color = AgTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (uiState.isStorageGranted) AgAccent.copy(alpha = 0.2f) else AgError.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = if (uiState.isStorageGranted) "GRANTED" else "DENIED",
                            color = if (uiState.isStorageGranted) AgAccent else AgError,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    "Необходим для чтения и редактирования файлов в папках на устройстве (/sdcard, Download, музыка, проекты).",
                    color = AgTextSecondary,
                    fontSize = 11.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.requestStoragePermission() },
                        colors = ButtonDefaults.buttonColors(containerColor = if (uiState.isStorageGranted) AgSurfaceVariant else AgTerminalPrompt),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (uiState.isStorageGranted) "Настройки доступа" else "Разрешить доступ",
                            fontSize = 12.sp,
                            color = if (uiState.isStorageGranted) AgTextPrimary else Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (uiState.isRootAvailable && !uiState.isStorageGranted) {
                        Button(
                            onClick = { viewModel.grantStorageViaRoot() },
                            colors = ButtonDefaults.buttonColors(containerColor = AgTerminalAmber),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Выдать через Root", fontSize = 12.sp, color = Color.Black)
                        }
                    }
                }
            }
        }

        // --- SECTION 8: WORKSPACE FOLDER PICKER ---
        Card(
            colors = CardDefaults.cardColors(containerColor = AgSurface),
            border = BorderStroke(1.dp, AgBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = AgTerminalPrompt, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Рабочая папка проекта (Workspace)", color = AgTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                Text(
                    text = uiState.workspacePath,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = AgTextSecondary
                )

                Button(
                    onClick = { showWorkspaceDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = AgSurfaceVariant),
                    border = BorderStroke(1.dp, AgBorder),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = AgTerminalPrompt, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Сменить папку проекта", color = AgTextPrimary, fontSize = 12.sp)
                }
            }
        }
    }

    if (showWorkspaceDialog) {
        WorkspacePickerDialog(
            currentPath = uiState.workspacePath,
            onSelectPath = { viewModel.updateWorkspace(it) },
            onDismiss = { showWorkspaceDialog = false }
        )
    }

    if (showAddAccountDialog) {
        AlertDialog(
            onDismissRequest = { showAddAccountDialog = false },
            title = { Text("Добавить профиль / аккаунт", color = AgTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = newAccountName,
                        onValueChange = { newAccountName = it },
                        placeholder = { Text("Название (например: Рабочий аккаунт)", fontSize = 12.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = AgSurfaceVariant,
                            unfocusedContainerColor = AgSurfaceVariant,
                            focusedTextColor = AgTextPrimary,
                            unfocusedTextColor = AgTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextField(
                        value = newAccountToken,
                        onValueChange = { newAccountToken = it },
                        placeholder = { Text("OAuth Токен (ya29... или JSON)", fontSize = 12.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = AgSurfaceVariant,
                            unfocusedContainerColor = AgSurfaceVariant,
                            focusedTextColor = AgTextPrimary,
                            unfocusedTextColor = AgTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newAccountName.isNotBlank() && newAccountToken.isNotBlank()) {
                            viewModel.saveAccount(newAccountName.trim(), newAccountToken.trim())
                            newAccountName = ""
                            newAccountToken = ""
                            showAddAccountDialog = false
                        }
                    }
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAccountDialog = false }) {
                    Text("Отмена")
                }
            },
            containerColor = AgSurface
        )
    }
}
