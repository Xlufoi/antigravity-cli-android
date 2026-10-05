package com.google.antigravity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.google.antigravity.data.ipc.AppLogger
import com.google.antigravity.data.ipc.EngineInstaller
import com.google.antigravity.data.repository.AgentRepositoryImpl
import com.google.antigravity.ui.chat.ChatScreen
import com.google.antigravity.ui.chat.ChatViewModel
import com.google.antigravity.ui.setup.SetupScreen
import com.google.antigravity.ui.theme.AntigravityTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val repository by lazy { AgentRepositoryImpl(applicationContext) }
    private val viewModel by lazy { ChatViewModel(repository) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AntigravityTheme {
                var isEngineReady by remember {
                    mutableStateOf(EngineInstaller.isEngineReady(applicationContext))
                }
                var installProgress by remember { mutableFloatStateOf(0f) }
                var installStatus by remember { mutableStateOf("Нажмите кнопку для распаковки") }

                if (!isEngineReady) {
                    SetupScreen(
                        progress = installProgress,
                        statusText = installStatus,
                        isComplete = installProgress >= 1.0f,
                        onStartInstall = {
                            lifecycleScope.launch {
                                installStatus = "Подготовка..."
                                AppLogger.log("MainActivity", "User requested engine install")
                                val success = EngineInstaller.installEngine(applicationContext) { progress, status ->
                                    installProgress = progress
                                    installStatus = status
                                }
                                if (success) {
                                    installProgress = 1.0f
                                    installStatus = "Готово к запуску!"
                                }
                            }
                        },
                        onLaunchApp = {
                            isEngineReady = true
                            viewModel.initEngine()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    ChatScreen(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkSystemPrivileges()
    }
}
