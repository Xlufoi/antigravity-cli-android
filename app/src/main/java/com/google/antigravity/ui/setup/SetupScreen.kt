package com.google.antigravity.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
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
import com.google.antigravity.ui.theme.*

@Composable
fun SetupScreen(
    progress: Float,
    statusText: String,
    isComplete: Boolean,
    onStartInstall: () -> Unit,
    onLaunchApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val logs by AppLogger.liveLogs.collectAsState()
    val listState = rememberLazyListState()
    var exportMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AgDarkBackground)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Antigravity Mobile",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = AgTextPrimary
        )
        Text(
            text = "Первоначальная распаковка и настройка ядра",
            fontSize = 13.sp,
            color = AgTextSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Progress Bar Card
        Card(
            colors = CardDefaults.cardColors(containerColor = AgSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = statusText, color = AgTextPrimary, fontSize = 13.sp)
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        color = AgPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = AgPrimary,
                    trackColor = AgSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live Log Box
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Журнал распаковки (Live Logs):", fontSize = 12.sp, color = AgTextSecondary)
            TextButton(
                onClick = {
                    val path = AppLogger.exportToDownloads(context)
                    exportMessage = "Лог сохранён:\n$path"
                }
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Скачать лог в Downloads", fontSize = 12.sp)
            }
        }

        exportMessage?.let { msg ->
            Text(text = msg, color = AgAccent, fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .border(1.dp, AgBorder, RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            LazyColumn(state = listState) {
                items(logs) { line ->
                    Text(
                        text = line,
                        color = if (line.contains("ERROR") || line.contains("Ошибка")) AgError else AgTextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons
        if (isComplete) {
            Button(
                onClick = onLaunchApp,
                colors = ButtonDefaults.buttonColors(containerColor = AgAccent),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Запустить Antigravity", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = onStartInstall,
                colors = ButtonDefaults.buttonColors(containerColor = AgPrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Начать распаковку ядра")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}
