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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
    MENU,
    ACCOUNTS,
    HISTORY,
    ADVANCED
}

@Composable
fun ControlCenterScreen(
    viewModel: ChatViewModel,
    uiState: ChatUiState,
    initialTab: SettingsSubTab = SettingsSubTab.MENU,
    onCloseSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }
    val scrollState = rememberScrollState()
    var showWorkspaceDialog by remember { mutableStateOf(false) }
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
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        if (selectedTab == SettingsSubTab.MENU) {
            SettingsMenuList(
                onCloseSettings = onCloseSettings,
                onSelectTab = { selectedTab = it },
                scrollState = scrollState
            )
        } else {
            // Full-screen sub-tab view with top navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "[< BACK]",
                    color = AgTerminalPrompt,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clickable { selectedTab = SettingsSubTab.MENU }
                        .padding(vertical = 4.dp, horizontal = 2.dp)
                )

                Text(
                    text = when (selectedTab) {
                        SettingsSubTab.ACCOUNTS -> "ACCOUNTS & QUOTA"
                        SettingsSubTab.HISTORY -> "CHAT HISTORY"
                        SettingsSubTab.ADVANCED -> "ADVANCED SETTINGS"
                        else -> "SETTINGS"
                    },
                    color = AgTextPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                Text(
                    text = "[CHAT]",
                    color = AgTerminalGreen,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clickable { onCloseSettings() }
                        .padding(vertical = 4.dp, horizontal = 2.dp)
                )
            }

            HorizontalDivider(color = AgSeparator, thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTab) {
                    SettingsSubTab.ACCOUNTS -> {
                        AccountsTabContent(
                            viewModel = viewModel,
                            uiState = uiState,
                            tokenInput = tokenInput,
                            onTokenInputChange = { tokenInput = it }
                        )
                    }
                    SettingsSubTab.HISTORY -> {
                        HistoryTabContent(
                            viewModel = viewModel,
                            uiState = uiState,
                            onCloseSettings = onCloseSettings
                        )
                    }
                    SettingsSubTab.ADVANCED -> {
                        AdvancedTabContent(
                            viewModel = viewModel,
                            uiState = uiState,
                            onOpenWorkspaceDialog = { showWorkspaceDialog = true }
                        )
                    }
                    SettingsSubTab.MENU -> {}
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
private fun SettingsMenuList(
    onCloseSettings: () -> Unit,
    onSelectTab: (SettingsSubTab) -> Unit,
    scrollState: androidx.compose.foundation.ScrollState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SETTINGS & SYSTEM",
                color = AgTerminalPrompt,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = "[CHAT]",
                color = AgTerminalGreen,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.clickable { onCloseSettings() }
            )
        }

        Text(
            text = "Select a category to view full-screen parameters:",
            color = AgTextSecondary,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp
        )

        HorizontalDivider(color = AgSeparator, thickness = 1.dp)

        SettingsCategoryCard(
            index = "1",
            title = "ACCOUNTS & QUOTA",
            subtitle = "Google authorization, saved profiles switcher & live API quota limits",
            onClick = { onSelectTab(SettingsSubTab.ACCOUNTS) }
        )

        SettingsCategoryCard(
            index = "2",
            title = "CHAT HISTORY",
            subtitle = "Browse saved conversation sessions, resume previous chats or delete",
            onClick = { onSelectTab(SettingsSubTab.HISTORY) }
        )

        SettingsCategoryCard(
            index = "3",
            title = "ADVANCED SETTINGS",
            subtitle = "Root access, ADB / Shizuku privileges, workspace permissions & live system logs",
            onClick = { onSelectTab(SettingsSubTab.ADVANCED) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onCloseSettings,
            border = BorderStroke(1.dp, AgBorder),
            shape = RoundedCornerShape(2.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AgTerminalPrompt),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "[< RETURN TO TERMINAL CHAT]",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun AccountsTabContent(
    viewModel: ChatViewModel,
    uiState: ChatUiState,
    tokenInput: String,
    onTokenInputChange: (String) -> Unit
) {
    val context = LocalContext.current

    // --- SECTION 1: GOOGLE OAUTH LOGIN & ACTIVE ACCOUNT (TOP) ---
    SectionHeader("ACTIVE ACCOUNT & GOOGLE LOGIN")

    val displayEmail = uiState.activeAccountEmail.ifBlank { "Default Account" }
    Text(
        text = "Active: $displayEmail",
        color = if (uiState.activeAccountEmail.isNotBlank()) AgTerminalGreen else AgTextPrimary,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp
    )
    Text(
        text = "Token: ${uiState.currentTokenSnippet}",
        color = AgTextSecondary,
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp
    )

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
        onValueChange = onTokenInputChange,
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
                    onTokenInputChange("")
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

    // --- SAVED ACCOUNTS ---
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
                    val display = account.email.ifBlank { account.name }
                    Text(
                        text = "> $display ${if (account.isCurrent) "[ACTIVE]" else ""}",
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

    HorizontalDivider(color = AgSeparator, thickness = 1.dp)

    // --- SECTION 2: QUOTA AND LIMITS (BELOW) ---
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionHeader("QUOTA AND LIMITS")
        Text(
            text = if (uiState.isSyncingQuota) "[SYNCING...]" else "[SYNC QUOTA]",
            color = if (uiState.isSyncingQuota) AgTerminalAmber else AgTerminalPrompt,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            modifier = Modifier.clickable(enabled = !uiState.isSyncingQuota) {
                viewModel.syncQuota()
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (uiState.quotaSummary.groups.isNotEmpty()) {
            uiState.quotaSummary.groups.forEach { group ->
                Text(
                    text = group.displayName.uppercase(),
                    color = AgTextPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                if (group.description.isNotBlank()) {
                    Text(
                        text = "  ${group.description}",
                        color = AgTextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
                group.buckets.forEach { bucket ->
                    AsciiQuotaBlock(
                        label = bucket.displayName,
                        percent = bucket.remainingPercent,
                        refreshesIn = bucket.getRefreshText(),
                        isYellow = bucket.remainingPercent < 50f
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
            }
        } else {
            // Default display matching Screenshot 2 until first sync
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
            AsciiQuotaBlock(
                label = "Weekly Limit Remaining",
                percent = 100f,
                refreshesIn = "Refreshes in 168h 0m",
                isYellow = false
            )
            AsciiQuotaBlock(
                label = "Five Hour Limit Remaining",
                percent = ((uiState.usageStats.remainingPercent)).coerceIn(10f, 100f),
                refreshesIn = "Refreshes in 4h 52m",
                isYellow = uiState.usageStats.remainingPercent < 50f
            )

            Spacer(modifier = Modifier.height(4.dp))

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
            AsciiQuotaBlock(
                label = "Weekly Limit Remaining",
                percent = 94.06f,
                refreshesIn = "Refreshes in 162h 37m",
                isYellow = false
            )
            AsciiQuotaBlock(
                label = "Five Hour Limit Remaining",
                percent = 95.99f,
                refreshesIn = "Refreshes in 4h 37m",
                isYellow = false
            )
        }

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
}

@Composable
private fun HistoryTabContent(
    viewModel: ChatViewModel,
    uiState: ChatUiState,
    onCloseSettings: () -> Unit
) {
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

@Composable
private fun AdvancedTabContent(
    viewModel: ChatViewModel,
    uiState: ChatUiState,
    onOpenWorkspaceDialog: () -> Unit
) {
    val context = LocalContext.current

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
            onClick = onOpenWorkspaceDialog,
            border = BorderStroke(1.dp, AgTerminalPrompt),
            shape = RoundedCornerShape(2.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AgTerminalPrompt)
        ) {
            Text("[CHANGE DIR]", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        }
    }

    HorizontalDivider(color = AgSeparator, thickness = 1.dp)

    // 6. SYSTEM & ENGINE LOGGING
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionHeader("SYSTEM & ENGINE LOGS")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "[COPY]",
                color = AgTerminalPrompt,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                modifier = Modifier.clickable {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    val clip = ClipData.newPlainText("Antigravity Logs", uiState.liveLogs.joinToString("\n"))
                    clipboard?.setPrimaryClip(clip)
                    Toast.makeText(context, "Logs copied to clipboard", Toast.LENGTH_SHORT).show()
                }
            )
            Text(
                text = "[EXPORT]",
                color = AgTerminalGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                modifier = Modifier.clickable {
                    val path = viewModel.exportLogsToDownloads(context)
                    Toast.makeText(context, "Saved to $path", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    // Monospace terminal log box
    val logBoxState = rememberLazyListState()
    LaunchedEffect(uiState.liveLogs.size) {
        if (uiState.liveLogs.isNotEmpty()) {
            logBoxState.scrollToItem(uiState.liveLogs.size - 1)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(Color(0xFF0A0A0A))
            .border(1.dp, AgBorder)
            .padding(8.dp)
    ) {
        if (uiState.liveLogs.isEmpty()) {
            Text(
                text = "> Logs will appear here as engine and commands execute...",
                color = AgTerminalDim,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
        } else {
            LazyColumn(
                state = logBoxState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.liveLogs) { line ->
                    val textColor = when {
                        line.contains("ERR", ignoreCase = true) || line.contains("failed", ignoreCase = true) -> AgError
                        line.contains("SUCCESS", ignoreCase = true) || line.contains("OK", ignoreCase = true) -> AgTerminalGreen
                        line.contains("STDERR", ignoreCase = true) -> AgTerminalAmber
                        else -> AgTextSecondary
                    }
                    Text(
                        text = line,
                        color = textColor,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }
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

@Composable
private fun SettingsCategoryCard(
    index: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(2.dp),
        color = Color.Black,
        border = BorderStroke(1.dp, AgBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "> [$index] $title",
                    color = AgTerminalPrompt,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "[OPEN >]",
                    color = AgTextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "  $subtitle",
                color = AgTextSecondary,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}
