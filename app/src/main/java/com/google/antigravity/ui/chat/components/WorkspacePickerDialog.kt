package com.google.antigravity.ui.chat.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.antigravity.ui.theme.*
import java.io.File

@Composable
fun WorkspacePickerDialog(
    currentPath: String,
    onSelectPath: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var customPath by remember { mutableStateOf(currentPath) }

    val presets = remember {
        listOf(
            "Внутренний воркспейс (App Sandbox)" to (context.filesDir.absolutePath + "/workspace"),
            "Папка в Загрузках (/sdcard/Download/antigravity)" to "/storage/emulated/0/Download/antigravity",
            "Папка Проекты (/sdcard/Projects)" to "/storage/emulated/0/Projects",
            "Termux Home (/data/data/com.termux/files/home)" to "/data/data/com.termux/files/home"
        )
    }

    val exists = remember(customPath) {
        try {
            File(customPath).exists()
        } catch (_: Exception) {
            false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    "Папка проекта (Workspace)",
                    color = AgTextPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    "Агент будет читать и редактировать файлы в этой папке",
                    color = AgTextSecondary,
                    fontSize = 11.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Быстрый выбор:", color = AgTextSecondary, fontSize = 12.sp)

                presets.forEach { (label, path) ->
                    val isSelected = customPath == path
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) AgPrimary.copy(alpha = 0.15f) else AgSurfaceVariant,
                        border = BorderStroke(1.dp, if (isSelected) AgPrimary else AgBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { customPath = path }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = if (isSelected) AgPrimary else AgTextSecondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = label, fontSize = 12.sp, color = AgTextPrimary, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                Text(text = path, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = AgTextSecondary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("Или укажите произвольный путь:", color = AgTextSecondary, fontSize = 12.sp)

                TextField(
                    value = customPath,
                    onValueChange = { customPath = it },
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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (exists) AgAccent else AgTerminalAmber,
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (exists) "Папка существует" else "Папка будет создана автоматически",
                        color = if (exists) AgAccent else AgTerminalAmber,
                        fontSize = 11.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (customPath.isNotBlank()) {
                        onSelectPath(customPath.trim())
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = AgPrimary)
            ) {
                Text("Применить", color = androidx.compose.ui.graphics.Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = AgTextSecondary)
            }
        },
        containerColor = AgSurface
    )
}
