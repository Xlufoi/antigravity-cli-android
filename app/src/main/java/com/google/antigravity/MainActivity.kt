package com.google.antigravity

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.google.antigravity.data.ipc.AgentForegroundService
import com.google.antigravity.data.repository.AgentRepositoryImpl
import com.google.antigravity.ui.chat.ChatScreen
import com.google.antigravity.ui.chat.ChatViewModel
import com.google.antigravity.ui.theme.AntigravityTheme

class MainActivity : ComponentActivity() {

    private var foregroundService: AgentForegroundService? = null
    private var isBound = false
    private val repository = AgentRepositoryImpl()
    private val viewModel by lazy { ChatViewModel(repository) }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as AgentForegroundService.LocalBinder
            foregroundService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            foregroundService = null
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Bind Foreground Service to ensure continuous agent operation
        val intent = Intent(this, AgentForegroundService::class.java)
        startService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        setContent {
            AntigravityTheme {
                ChatScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onDestroy() {
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
        super.onDestroy()
    }
}
