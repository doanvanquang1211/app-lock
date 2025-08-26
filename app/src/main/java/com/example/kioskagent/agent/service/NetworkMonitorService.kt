package com.example.kioskagent.agent.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class NetworkMonitorService : Service() {

    private val channelId = "network_monitor_service"

    override fun onCreate() {
        super.onCreate()
        Log.d("NetworkMonitorService", "Service started")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Network Monitor Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)

            val notification: Notification = NotificationCompat.Builder(this, channelId)
                .setContentTitle("Kiosk Agent")
                .setContentText("Monitoring network / Anti-Theft")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .build()

            startForeground(1, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // TODO: Thêm logic theo dõi mạng, anti-theft, lock/wipe nếu cần
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
