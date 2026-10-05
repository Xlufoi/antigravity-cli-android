package com.google.antigravity.ui.chat.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.antigravity.data.ipc.AccountProfile
import com.google.antigravity.ui.theme.*

@Composable
fun AccountSelectorDialog(
    activeEmail: String,
    accounts: List<AccountProfile>,
    onSelectAccount: (String) -> Unit,
    onOpenAccountsSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(2.dp),
            color = Color.Black,
            border = BorderStroke(1.dp, AgBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "> SELECT GOOGLE ACCOUNT",
                        color = AgTerminalPrompt,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "[X]",
                        color = AgTextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable { onDismiss() }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = AgSeparator, thickness = 1.dp)
                Spacer(modifier = Modifier.height(6.dp))

                if (accounts.isEmpty()) {
                    Text(
                        text = "No saved accounts found.",
                        color = AgTextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(accounts, key = { it.id }) { acc ->
                            val isSelected = acc.isCurrent || (activeEmail.isNotBlank() && (acc.email == activeEmail || acc.name == activeEmail))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectAccount(acc.id)
                                        onDismiss()
                                    }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isSelected) "[x] " else "[ ] ",
                                    color = if (isSelected) AgTerminalGreen else AgTextSecondary,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    val displayName = acc.email.ifBlank { acc.name }
                                    Text(
                                        text = displayName,
                                        color = if (isSelected) AgTerminalPrompt else AgTextPrimary,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                    if (acc.email.isNotBlank() && acc.name.isNotBlank() && acc.name != acc.email) {
                                        Text(
                                            text = acc.name,
                                            color = AgTextSecondary,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Text(
                                        text = "[ACTIVE]",
                                        color = AgTerminalGreen,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            HorizontalDivider(color = AgBorder, thickness = 0.5.dp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onOpenAccountsSettings()
                    },
                    border = BorderStroke(1.dp, AgTerminalPrompt),
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AgTerminalPrompt),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "[+ MANAGE / ADD ACCOUNTS]",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
