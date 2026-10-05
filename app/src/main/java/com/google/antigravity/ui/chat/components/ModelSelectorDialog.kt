package com.google.antigravity.ui.chat.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Модели Antigravity",
                        color = AgTextPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        "Список загружается напрямую с серверов",
                        color = AgTextSecondary,
                        fontSize = 11.sp
                    )
                }
                IconButton(onClick = onRefresh, enabled = !isLoading) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = AgPrimary, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить", tint = AgPrimary)
                    }
                }
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
            ) {
                if (availableModels.isEmpty() && isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AgPrimary)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(availableModels, key = { it.id }) { model ->
                            val isSelected = currentModel == model.id
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) AgTerminalPurple.copy(alpha = 0.2f) else AgSurfaceVariant,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) AgTerminalPurple else AgBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectModel(model.id)
                                        onDismiss()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            onSelectModel(model.id)
                                            onDismiss()
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = AgTerminalPurple)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = model.displayName,
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) AgTextPrimary else AgTextSecondary
                                            )
                                            if (model.isRecommended) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = AgAccent.copy(alpha = 0.2f)
                                                ) {
                                                    Text(
                                                        "FAST",
                                                        color = AgAccent,
                                                        fontSize = 9.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = model.id,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = AgTextSecondary.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть", color = AgTextSecondary)
            }
        },
        containerColor = AgSurface
    )
}
