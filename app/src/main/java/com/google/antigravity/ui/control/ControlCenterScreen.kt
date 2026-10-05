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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.google.antigravity.ui.chat.ChatUiState
import com.google.antigravity.ui.chat.ChatViewModel
import com.google.antigravity.ui.chat.components.WorkspacePickerDialog
import com.google.antigravity.ui.theme.*

enum class SettingsSubTab {
    ACCOUNTS,
    HISTORY,
    ADVANCED
}

@Composable
fun ControlCenterScreen(
    viewModel: ChatViewModel,
    uiState: ChatUiState,
    initialTab: SettingsSubTab = SettingsSubTab.ACCOUNTS,
    onCloseSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(initialTab) }
    val scrollState = rememberScrollState()
    var showWorkspaceDialog by remember { mutableStateOf(false) }
    var tokenInput by remember { mutableStateOf("") }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var newAccountName by remember { mutableStateOf("") }
    var newAccountToken by remember { mutableStateOf("") }

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
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        // --- TOP TAB SELECTOR (TERMINAL STYLE) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabButton(
                title = "[ACCOUNTS]",
                isSelected = selectedTab == SettingsSubTab.ACCOUNTS,
                onClick = { selectedTab = SettingsSubTab.ACCOUNTS },
                modifier = Modifier.weight(1f)
            )

            TabButton(
                title = "[HISTORY]",
                isSelected = selectedTab == SettingsSubTab.HISTORY,
                onClick = { selectedTab = SettingsSubTab.HISTORY },
                modifier = Modifier.weight(1f)
            )

            TabButton(
                title = "[ADVANCED]",
                isSelected = selectedTab == SettingsSubTab.ADVANCED,
                onClick = { selectedTab = SettingsSubTab.ADVANCED },
                modifier = Modifier.weight(1f)
            )

            // Close button
            OutlinedButton(
                onClick = onCloseSettings,
                border = BorderStroke(1.dp, AgBorder),
                shape = RoundedCornerShape(2.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AgTextSecondary)
            ) {
                Text("[X]", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        HorizontalDivider(color = AgSeparator, thickness = 1.dp)
        Spacer(modifier = Modifier.height(10.dp))

        // --- TAB CONTENTS ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (selectedTab) {
                // ==================== TAB 1: ACCOUNTS & QUOTA ====================
                SettingsSubTab.ACCOUNTS -> {
                    // --- SECTION 1: QUOTA (EXACTLY MATCHING SCREENSHOT 2) ---
                    SectionHeader("QUOTA AND LIMITS")

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // GEMINI MODELS GROUP
                        Text(
                            text = "GEMINI MODELS",
                            color = AgTextPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "  Models within this group: Gemini Flash, Gemini Pro",
                            color = AgTextSecondary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )

                        // Gemini Weekly Limit
                        AsciiQuotaBlock(
                            label = "Weekly Limit Remaining",
                            percent = 100f,
                            refreshesIn = "Refreshes in 168h 0m",
                            isYellow = false
                        )

                        // Gemini 5h limit
                        AsciiQuotaBlock(
                            label = "Five Hour Limit Remaining",
                            percent = ((uiState.usageStats.remainingPercent)).coerceIn(10f, 100f),
                            refreshesIn = "Refreshes in 4h 52m",
                            isYellow = uiState.usageStats.remainingPercent < 50f
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // CLAUDE AND GPT MODELS GROUP
                        Text(
                            text = "CLAUDE AND GPT MODELS",
                            color = AgTextPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "  Models within this group: Claude Opus, Claude Sonnet, GPT-OSS",
                            color = AgTextSecondary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )

                        // Claude Weekly Limit
                        AsciiQuotaBlock(
                            label = "Weekly Limit Remaining",
                            percent = 94.06f,
                            refreshesIn = "Refreshes in 162h 37m",
                            isYellow = false
                        )

                        // Claude 5h limit
                        AsciiQuotaBlock(
                            label = "Five Hour Limit Remaining",
                            percent = 95.99f,
                            refreshesIn = "Refreshes in 4h 37m",
                            isYellow = false
                        )

                        // Active Context details
                        Text(
                            text = "ACTIVE CONTEXT OCCUPANCY",
                            color = AgTextPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        AsciiQuotaBlock(
                            label = "Context Window (${uiState.activeModel})",
                            percent = uiState.usageStats.remainingPercent,
                            refreshesIn = "Free: ${String.format(java.util.Locale.US, "%,d", uiState.usageStats.remainingContextTokens).replace(',', ' ')} / ${String.format(java.util.Locale.US, "%,d", uiState.usageStats.contextWindowLimit).replace(',', ' ')} tokens",
                            isYellow = uiState.usageStats.remainingPercent < 40f
                        )

                        Text(
                            text = "Tier: ${uiState.usageStats.tierName} — Unlimited Quota",
                            color = AgTerminalGreen,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }

                    HorizontalDivider(color = AgSeparator, thickness = 1.dp)

                    // --- SECTION 2: GOOGLE OAUTH LOGIN ---
                    SectionHeader("GOOGLE OAUTH LOGIN")

                    Text(
                        text = "Generate browser login link or paste authorization code / token:",
                        color = AgTextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )

                    OutlinedButton(
                        onClick = { viewModel.requestAuthUrl() },
                        enabled = !uiState.isGeneratingAuthUrl,
                        border = BorderStroke(1.dp, AgTerminalPrompt),
                        shape = RoundedCornerShape(2.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AgTerminalPrompt),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (uiState.isGeneratingAuthUrl) "[GENERATING URL...]" else "[GENERATE LOGIN URL]",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    uiState.generatedAuthUrl?.let { url ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, AgBorder)
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = url.take(120) + "...",
                                color = AgTerminalDim,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    border = BorderStroke(1.dp, AgTerminalPrompt),
                                    shape = RoundedCornerShape(2.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AgTerminalPrompt),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("[OPEN IN BROWSER]", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Auth URL", url))
                                        Toast.makeText(context, "URL copied to clipboard", Toast.LENGTH_SHORT).show()
                                    },
                                    border = BorderStroke(1.dp, AgBorder),
                                    shape = RoundedCornerShape(2.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AgTextSecondary)
                                ) {
                                    Text("[COPY URL]", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    // Direct Code / Token Input
                    TextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        placeholder = {
                            Text("paste auth code (4/0A...) or token json...", color = AgTerminalDim, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = AgSurfaceVariant,
                            unfocusedContainerColor = AgSurfaceVariant,
                            focusedTextColor = AgTextPrimary,
                            unfocusedTextColor = AgTextPrimary,
                            focusedIndicatorColor = AgTerminalPrompt,
                            unfocusedIndicatorColor = AgBorder
                        ),
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (tokenInput.isNotBlank()) {
                                    val input = tokenInput.trim()
                                    if (input.startsWith("{") || input.contains("access_token")) {
                                        viewModel.saveAccount("Default Account", input)
                                        Toast.makeText(context, "Token saved and activated", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Exchanging auth code with Google...", Toast.LENGTH_SHORT).show()
                                        viewModel.submitAuthCode(input) { success ->
                                            if (success) {
                                                Toast.makeText(context, "Google OAuth success!", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "Auth code exchange failed. Please generate new URL.", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                    tokenInput = ""
                                }
                            },
                            border = BorderStroke(1.dp, AgTerminalPrompt),
                            shape = RoundedCornerShape(2.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AgTerminalPrompt),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("[SUBMIT CODE / TOKEN]", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val success = viewModel.importTokenFromDownloads()
                                if (success) {
                                    Toast.makeText(context, "Token loaded from Downloads", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "No token found in Downloads", Toast.LENGTH_SHORT).show()
                                }
                            },
                            border = BorderStroke(1.dp, AgBorder),
                            shape = RoundedCornerShape(2.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AgTextSecondary)
                        ) {
                            Text("[IMPORT DOWNLOADS]", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                    }

                    HorizontalDivider(color = AgSeparator, thickness = 1.dp)

                    // --- SECTION 3: SAVED ACCOUNTS ---
                    SectionHeader("SAVED PROFILES")

                    if (uiState.accounts.isEmpty()) {
                        Text(
                            text = "No saved profiles yet. Token is loaded from Downloads or active session.",
                            color = AgTerminalDim,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    } else {
                        uiState.accounts.forEach { account ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, if (account.isCurrent) AgTerminalPrompt else AgBorder)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "> ${account.name} ${if (account.isCurrent) "[ACTIVE]" else ""}",
                                        color = if (account.isCurrent) AgTerminalPrompt else AgTextPrimary,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "  token: ${account.token.take(24)}...",
                                        color = AgTextSecondary,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (!account.isCurrent) {
                                        Text(
                                            text = "[SWITCH]",
                                            color = AgTerminalGreen,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            modifier = Modifier.clickable { viewModel.switchAccount(account.id) }
                                        )
                                    }
                                    Text(
                                        text = "[DEL]",
                                        color = AgError,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        modifier = Modifier.clickable { viewModel.deleteAccount(account.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                // ==================== TAB 2: HISTORY ====================
                SettingsSubTab.HISTORY -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeader("CHAT SESSIONS")
                        OutlinedButton(
                            onClick = {
                                viewModel.createNewChat()
                                onCloseSettings()
                            },
                            border = BorderStroke(1.dp, AgTerminalGreen),
                            shape = RoundedCornerShape(2.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AgTerminalGreen)
                        ) {
                            Text("[+ NEW CHAT]", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (uiState.sessions.isEmpty()) {
                        Text(
                            text = "No saved chat sessions found.",
                            color = AgTerminalDim,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    } else {
                        uiState.sessions.forEach { session ->
                            val isActive = session.id == uiState.currentSessionId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, if (isActive) AgTerminalPrompt else AgBorder)
                                    .clickable {
                                        viewModel.switchChat(session.id)
                                        onCloseSettings()
                                    }
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "> ${session.title} ${if (isActive) "[CURRENT]" else ""}",
                                        color = if (isActive) AgTerminalPrompt else AgTextPrimary,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "  model: ${session.model} | messages: ${session.messages.size}",
                                        color = AgTextSecondary,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "[LOAD]",
                                        color = AgTerminalPrompt,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        modifier = Modifier.clickable {
                                            viewModel.switchChat(session.id)
                                            onCloseSettings()
                                        }
                                    )
                                    Text(
                                        text = "[DEL]",
                                        color = AgError,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        modifier = Modifier.clickable {
                                            viewModel.deleteChat(session.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // ==================== TAB 3: ADVANCED SETTINGS ====================
                SettingsSubTab.ADVANCED -> {
                    SectionHeader("SYSTEM PRIVILEGES")

                    // 1. ROOT ACCESS
                    PrivilegeBlock(
                        title = "ROOT ACCESS (su)",
                        status = if (uiState.isRootGranted) "[GRANTED]" else if (uiState.isRootAvailable) "[NOT GRANTED]" else "[NO SU BINARY]",
                        statusColor = if (uiState.isRootGranted) AgTerminalGreen else if (uiState.isRootAvailable) AgTerminalAmber else AgTextSecondary,
                        description = "Direct root shell access for unrestricted Linux operations on Android.",
                        actionButtonText = if (uiState.isRootGranted) "[VERIFIED]" else "[REQUEST ROOT]",
                        onAction = {
                            if (!uiState.isRootGranted) {
                                viewModel.grantStorageViaRoot()
                            }
                        },
                        testButtonText = "[TEST ROOT (id)]",
                        onTest = { viewModel.testRootCommand() }
                    )

                    // 2. ADB / SHIZUKU ACCESS
                    PrivilegeBlock(
                        title = "ADB ACCESS (Shizuku)",
                        status = if (uiState.isShizukuGranted) "[GRANTED]" else if (uiState.isShizukuInstalled) "[INSTALLED]" else "[NOT INSTALLED]",
                        statusColor = if (uiState.isShizukuGranted) AgTerminalGreen else if (uiState.isShizukuInstalled) AgTerminalAmber else AgTextSecondary,
                        description = "Enables agent to execute system ADB commands via Shizuku service without root.",
                        actionButtonText = if (uiState.isShizukuGranted) "[VERIFIED]" else "[REQUEST SHIZUKU]",
                        onAction = { viewModel.requestShizukuPermission() },
                        testButtonText = "[TEST ADB (getprop)]",
                        onTest = { viewModel.testShizukuCommand() }
                    )

                    // 3. STORAGE ACCESS
                    PrivilegeBlock(
                        title = "STORAGE ACCESS (All Files)",
                        status = if (uiState.isStorageGranted) "[GRANTED]" else "[NOT GRANTED]",
                        statusColor = if (uiState.isStorageGranted) AgTerminalGreen else AgError,
                        description = "Required to read and modify audio, scripts, and downloads on /storage/emulated/0.",
                        actionButtonText = "[REQUEST STORAGE ACCESS]",
                        onAction = { viewModel.requestStoragePermission() }
                    )

                    // Command result banner if tested
                    uiState.systemCommandResult?.let { result ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, AgTerminalPrompt)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "OUTPUT:",
                                color = AgTerminalPrompt,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = result,
                                color = AgTextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    }

                    HorizontalDivider(color = AgSeparator, thickness = 1.dp)

                    // 4. AUTO-APPROVE COMMANDS
                    SectionHeader("EXECUTION PERMISSION MODE")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AgBorder)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-approve agent tool commands",
                                color = AgTextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "--dangerously-skip-permissions: ${if (uiState.autoApprove) "ON" else "OFF"}",
                                color = if (uiState.autoApprove) AgTerminalGreen else AgTerminalAmber,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                        }

                        OutlinedButton(
                            onClick = { viewModel.toggleAutoApprove() },
                            border = BorderStroke(1.dp, if (uiState.autoApprove) AgTerminalGreen else AgTerminalAmber),
                            shape = RoundedCornerShape(2.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (uiState.autoApprove) AgTerminalGreen else AgTerminalAmber
                            )
                        ) {
                            Text(
                                text = if (uiState.autoApprove) "[ON]" else "[OFF]",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(color = AgSeparator, thickness = 1.dp)

                    // 5. WORKSPACE DIRECTORY
                    SectionHeader("PROJECT WORKSPACE")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AgBorder)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uiState.workspacePath,
                                color = AgTextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }

                        OutlinedButton(
                            onClick = { showWorkspaceDialog = true },
                            border = BorderStroke(1.dp, AgTerminalPrompt),
                            shape = RoundedCornerShape(2.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AgTerminalPrompt)
                        ) {
                            Text("[CHANGE DIR]", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
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

@Composable
private fun TabButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        border = BorderStroke(1.dp, if (isSelected) AgTerminalPrompt else AgBorder),
        shape = RoundedCornerShape(2.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (isSelected) AgTerminalPrompt else AgTextSecondary
        ),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
        modifier = modifier
    ) {
        Text(
            text = title,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = AgTerminalPrompt,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp
    )
}

/**
 * ASCII Quota Bar strictly matching Screenshot 2:
 * [████████████░░░░░░░░░░░░░░░░░░░░] 46.14%
 * Refreshes in 65h 19m
 */
@Composable
private fun AsciiQuotaBlock(
    label: String,
    percent: Float,
    refreshesIn: String,
    isYellow: Boolean = false
) {
    val clamped = percent.coerceIn(0f, 100f)
    val totalBlocks = 28
    val filledCount = ((clamped / 100f) * totalBlocks).toInt()
    val emptyCount = (totalBlocks - filledCount).coerceAtLeast(0)
    val filledStr = "█".repeat(filledCount)
    val emptyStr = "░".repeat(emptyCount)

    val barColor = if (isYellow) AgTerminalAmber else AgTerminalGreen

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = "  $label",
            color = AgTextPrimary,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "[",
                color = AgTextPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
            Text(
                text = filledStr,
                color = barColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
            Text(
                text = emptyStr,
                color = AgTerminalDim,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
            Text(
                text = "] ${String.format(java.util.Locale.US, "%.2f", clamped)}%",
                color = AgTextPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
        }

        Text(
            text = "  $refreshesIn",
            color = barColor,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 1.dp)
        )
    }
}

@Composable
private fun PrivilegeBlock(
    title: String,
    status: String,
    statusColor: Color,
    description: String,
    actionButtonText: String,
    onAction: () -> Unit,
    testButtonText: String? = null,
    onTest: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AgBorder)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = AgTextPrimary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Text(
                text = status,
                color = statusColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }

        Text(
            text = description,
            color = AgTextSecondary,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onAction,
                border = BorderStroke(1.dp, AgTerminalPrompt),
                shape = RoundedCornerShape(2.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AgTerminalPrompt),
                modifier = Modifier.weight(1f)
            ) {
                Text(actionButtonText, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }

            if (testButtonText != null && onTest != null) {
                OutlinedButton(
                    onClick = onTest,
                    border = BorderStroke(1.dp, AgBorder),
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AgTextSecondary)
                ) {
                    Text(testButtonText, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }
        }
    }
}
