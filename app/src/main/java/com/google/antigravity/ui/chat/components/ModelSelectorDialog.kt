package com.google.antigravity.ui.chat.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.antigravity.domain.model.ModelInfo
import com.google.antigravity.ui.theme.*

@Composable
fun ModelSelectorDialog(
    currentModel: String,
    availableModels: List<ModelInfo>,
    isLoading: Boolean,
    onSelectModel: (String) -> Unit,
    onRefresh: () -> Unit,
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
                // Monospace Terminal Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "> SELECT ACTIVE MODEL",
                        color = AgTerminalPrompt,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isLoading) "[SYNCING...]" else "[REFRESH]",
                            color = if (isLoading) AgTerminalAmber else AgTerminalPrompt,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.clickable(enabled = !isLoading) { onRefresh() }
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
                }

                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = AgSeparator, thickness = 1.dp)
                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                ) {
                    if (availableModels.isEmpty() && isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Querying live model registry...",
                                color = AgTextSecondary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            items(availableModels, key = { it.id }) { model ->
                                val cleanId = model.id.split(Regex("[\\t\\s]")).first()
                                val isSelected = currentModel.startsWith(cleanId)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSelectModel(cleanId)
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
                                        Text(
                                            text = cleanId,
                                            color = if (isSelected) AgTerminalPrompt else AgTextPrimary,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 12.sp
                                        )
                                        if (model.displayName.isNotBlank() && model.displayName != cleanId) {
                                            Text(
                                                text = model.displayName,
                                                color = AgTextSecondary,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                    if (model.effort.isNotBlank()) {
                                        Text(
                                            text = "[${model.effort}]",
                                            color = AgTerminalDim,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                                HorizontalDivider(color = AgBorder, thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}
