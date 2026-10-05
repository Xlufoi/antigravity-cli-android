package com.google.antigravity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.google.antigravity.data.repository.AgentRepositoryImpl
import com.google.antigravity.ui.chat.ChatScreen
import com.google.antigravity.ui.chat.ChatViewModel
import com.google.antigravity.ui.theme.AntigravityTheme

class MainActivity : ComponentActivity() {

    private val repository by lazy { AgentRepositoryImpl(applicationContext) }
    private val viewModel by lazy { ChatViewModel(repository) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AntigravityTheme {
                ChatScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
