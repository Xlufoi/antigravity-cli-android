package com.google.antigravity.data.ipc

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.antigravity.R

class AgentForegroundService : Service() {

    private val binder = LocalBinder()
    var processManager: NativeProcessManager? = null
        private set

    inner class LocalBinder : Binder() {
        fun getService(): AgentForegroundService = this@AgentForegroundService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    fun initProcessManager(model: String, oauthToken: String? = null) {
        if (processManager == null) {
            processManager = NativeProcessManager(
                context = applicationContext,
                model = model,
                oauthToken = oauthToken
            )
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.agent_service_channel),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.agent_running_notification))
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        processManager?.stopEngine()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "antigravity_agent_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
