package com.google.antigravity.ui.setup

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.antigravity.data.ipc.AppLogger
import com.google.antigravity.data.ipc.EngineInstaller
import com.google.antigravity.ui.chat.ChatUiState
import com.google.antigravity.ui.chat.ChatViewModel
import com.google.antigravity.ui.control.OAuthWebViewDialog
import com.google.antigravity.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun InitialSetupScreen(
    viewModel: ChatViewModel,
    uiState: ChatUiState,
    onFinishOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var isEngineReady by remember {
        mutableStateOf(EngineInstaller.isEngineReady(context))
    }
    var isInstalling by remember { mutableStateOf(false) }
    var installProgress by remember { mutableFloatStateOf(if (isEngineReady) 1.0f else 0f) }
    var installStatus by remember {
        mutableStateOf(if (isEngineReady) "Linux sandbox core is ready" else "Ready to initialize")
    }

    var tokenInput by remember { mutableStateOf("") }
    var showLoginOptions by remember { mutableStateOf(!isEngineReady || uiState.currentTokenSnippet.isBlank()) }

    // Auto-start installation on first entry if engine is not ready
    LaunchedEffect(Unit) {
        if (!isEngineReady && !isInstalling) {
            isInstalling = true
            installStatus = "Extracting system assets and binaries..."
            AppLogger.log("InitialSetup", "Auto-starting engine installation")
            val success = EngineInstaller.installEngine(context) { progress, status ->
                installProgress = progress
                installStatus = status
            }
            if (success) {
                installProgress = 1.0f
                installStatus = "Extraction completed successfully"
                isEngineReady = true
                viewModel.initEngine()
            } else {
                installStatus = "Extraction encountered an error"
            }
            isInstalling = false
        }
    }

    if (uiState.showOAuthWebView && uiState.generatedAuthUrl != null) {
        OAuthWebViewDialog(
            authUrl = uiState.generatedAuthUrl,
            onAuthCodeReceived = { code ->
                viewModel.submitAuthCode(code) { ok ->
                    if (ok) {
                        Toast.makeText(context, "Authorization completed successfully!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Authorization failed. Please try again.", Toast.LENGTH_LONG).show()
                    }
                }
            },
            onDismiss = { viewModel.closeOAuthWebView() }
        )
    }

    val isUnpackComplete = isEngineReady || installProgress >= 1.0f
    val isAccountReady = uiState.currentTokenSnippet.isNotBlank() || uiState.accounts.isNotEmpty()
    val isStorageReady = uiState.isStorageGranted

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AgDarkBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Terminal Header
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "> ANTIGRAVITY OS INITIAL SETUP",
                color = AgTerminalPrompt,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = "System bootstrap, credentials & device permissions",
                color = AgTextSecondary,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        }

        HorizontalDivider(color = AgSeparator, thickness = 1.dp)

        // ==================== STEP 1: UNPACKING APPLICATION CORE ====================
        SetupStepCard(
            stepNumber = "1",
            title = "APPLICATION UNPACKING (РАСПАКОВКА)",
            statusText = if (isUnpackComplete) "[READY]" else if (isInstalling) "[UNPACKING: ${(installProgress * 100).toInt()}%]" else "[PENDING]",
            statusColor = if (isUnpackComplete) AgTerminalGreen else if (isInstalling) AgTerminalPrompt else AgTerminalAmber
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = installStatus,
                    color = AgTextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )

                LinearProgressIndicator(
                    progress = { installProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = AgTerminalPrompt,
                    trackColor = AgSurfaceVariant
                )

                if (!isUnpackComplete && !isInstalling) {
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                isInstalling = true
                                val ok = EngineInstaller.installEngine(context) { p, s ->
                                    installProgress = p
                                    installStatus = s
                                }
                                if (ok) {
                                    installProgress = 1.0f
                                    installStatus = "Extraction completed"
                                    isEngineReady = true
                                    viewModel.initEngine()
                                }
                                isInstalling = false
                            }
                        },
                        border = BorderStroke(1.dp, AgTerminalPrompt),
                        shape = RoundedCornerShape(2.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AgTerminalPrompt),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("[START UNPACKING ENGINE]", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ==================== STEP 2: ACCOUNT AUTHORIZATION ====================
        SetupStepCard(
            stepNumber = "2",
            title = "GOOGLE ACCOUNT LOGIN (ВХОД В АККАУНТ)",
            statusText = if (isAccountReady) "[AUTHORIZED]" else "[NOT AUTHORIZED]",
            statusColor = if (isAccountReady) AgTerminalGreen else AgTerminalAmber
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isAccountReady) {
                    val display = uiState.activeAccountEmail.ifBlank { "Active Account" }
                    Text(
                        text = "Active: $display",
                        color = AgTerminalGreen,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Token: ${uiState.currentTokenSnippet}",
                        color = AgTextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                    Text(
                        text = if (showLoginOptions) "[HIDE LOGIN OPTIONS]" else "[SWITCH / RE-LOGIN]",
                        color = AgTerminalPrompt,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.clickable { showLoginOptions = !showLoginOptions }
                    )
                } else {
                    Text(
                        text = "Connect Google profile or paste access token to query Gemini & Claude models:",
                        color = AgTextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }

                if (showLoginOptions || !isAccountReady) {
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
                            fontSize = 11.sp
                        )
                    }

                    uiState.generatedAuthUrl?.let { url ->
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
                                Text("[BROWSER]", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Auth URL", url))
                                    Toast.makeText(context, "URL copied", Toast.LENGTH_SHORT).show()
                                },
                                border = BorderStroke(1.dp, AgBorder),
                                shape = RoundedCornerShape(2.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AgTextSecondary)
                            ) {
                                Text("[COPY]", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                            }
                        }
                    }

                    TextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        placeholder = {
                            Text("paste auth code (4/0A...) or token json...", color = AgTerminalDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
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
                                        Toast.makeText(context, "Token saved!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Exchanging auth code with Google...", Toast.LENGTH_SHORT).show()
                                        viewModel.submitAuthCode(input) { success ->
                                            if (success) {
                                                Toast.makeText(context, "Google OAuth success!", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "Exchange failed. Generate fresh URL.", Toast.LENGTH_LONG).show()
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
                            Text("[SUBMIT CODE]", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val success = viewModel.importTokenFromDownloads()
                                if (success) {
                                    Toast.makeText(context, "Token imported from Downloads!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "No token found in /sdcard/Download", Toast.LENGTH_SHORT).show()
                                }
                            },
                            border = BorderStroke(1.dp, AgBorder),
                            shape = RoundedCornerShape(2.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AgTextSecondary)
                        ) {
                            Text("[IMPORT DOWNLOADS]", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // ==================== STEP 3: STORAGE PERMISSION ====================
        SetupStepCard(
            stepNumber = "3",
            title = "DEVICE STORAGE ACCESS (ДОСТУП К ПАМЯТИ)",
            statusText = if (isStorageReady) "[GRANTED]" else "[NOT GRANTED]",
            statusColor = if (isStorageReady) AgTerminalGreen else AgError
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Required for AI agent to inspect, modify, and process files in /storage/emulated/0 (audio, scripts, downloads).",
                    color = AgTextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )

                if (!isStorageReady) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.requestStoragePermission() },
                            border = BorderStroke(1.dp, AgError),
                            shape = RoundedCornerShape(2.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AgError),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("[GRANT STORAGE ACCESS]", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        if (uiState.isRootAvailable) {
                            OutlinedButton(
                                onClick = { viewModel.grantStorageViaRoot() },
                                border = BorderStroke(1.dp, AgTerminalGreen),
                                shape = RoundedCornerShape(2.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AgTerminalGreen)
                            ) {
                                Text("[VIA ROOT]", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ==================== BOTTOM ACTION BUTTONS ====================
        // Rule: ЕСЛИ НЕ СДЕЛАТЬ РАСПАКОВКУ ТО НЕ БУДЕТ КНОПОК
        if (isUnpackComplete) {
            if (isAccountReady && isStorageReady) {
                // All steps completed -> [DONE]
                OutlinedButton(
                    onClick = onFinishOnboarding,
                    border = BorderStroke(1.dp, AgTerminalGreen),
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AgTerminalGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "[DONE]",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            } else {
                // Account or storage not completed -> [SKIP]
                OutlinedButton(
                    onClick = onFinishOnboarding,
                    border = BorderStroke(1.dp, AgTerminalPrompt),
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AgTerminalPrompt),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "[SKIP]",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            // Unpacking still running -> no buttons as requested
            Text(
                text = "> System unpacking in progress... Action buttons will unlock once complete.",
                color = AgTerminalDim,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SetupStepCard(
    stepNumber: String,
    title: String,
    statusText: String,
    statusColor: Color,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(2.dp),
        color = Color.Black,
        border = BorderStroke(1.dp, AgBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "> [$stepNumber] $title",
                    color = AgTextPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = statusText,
                    color = statusColor,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }

            HorizontalDivider(color = AgSeparator, thickness = 1.dp)

            content()
        }
    }
}
