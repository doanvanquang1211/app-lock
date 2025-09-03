package com.example.kioskagent.agent.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.*
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.PowerManager
import android.os.UserManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.example.kioskagent.MainActivity
import com.example.kioskagent.agent.AdminReceiver

class KioskBackgroundService : Service() {

    private lateinit var dpm: DevicePolicyManager
    private lateinit var adminComponent: ComponentName
    private val allowedSSID = "TC"
    private val allowedPassword = "Tcom123$567*"
    private lateinit var detector: TopAppDetector

    private val handler = Handler()
    private val checkRunnable = object : Runnable {
        override fun run() {
            try {
                val top = detector.getTopPackage()

                if (top == "com.android.launcher") {
                    val intent = Intent(applicationContext, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    applicationContext.startActivity(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            enforceWifi()
            handler.postDelayed(this, 10_000) // kiểm tra mỗi 10s
        }
    }

    override fun onCreate() {
        super.onCreate()
        detector = TopAppDetector(applicationContext)

        dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, AdminReceiver::class.java)

        Log.d("KioskService", "isDeviceOwner = ${dpm.isDeviceOwnerApp(packageName)}")

        // Bắt đầu giám sát WiFi
        handler.post(checkRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "kiosk_service_channel"
            val channel = NotificationChannel(
                channelId,
                "Kiosk Background Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)

            val notification = NotificationCompat.Builder(this, channelId)
                .setContentTitle("Kiosk Mode")
                .setContentText("Đang chạy chế độ kiosk...")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .build()

            startForeground(1, notification)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(checkRunnable)

    }
    override fun onBind(intent: Intent?): IBinder? = null

    private fun enforceWifi() {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val currentSSID = wifiManager.connectionInfo?.ssid?.replace("\"", "")

        if (currentSSID != allowedSSID) {
            Log.w("WifiEnforcer", "Sai WiFi: $currentSSID, chuyển về $allowedSSID")

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                // ✅ Android 9 trở xuống → dùng WifiConfiguration
                val wifiConfig = WifiConfiguration().apply {
                    SSID = "\"$allowedSSID\""
                    preSharedKey = "\"$allowedPassword\""
                    allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
                }

                val netId = wifiManager.addNetwork(wifiConfig)
                if (netId != -1) {
                    wifiManager.disconnect()
                    wifiManager.enableNetwork(netId, true)
                    wifiManager.reconnect()
                } else {
                    Log.e("WifiEnforcer", "Không add được cấu hình WiFi $allowedSSID")
                }
            } else {
                // ✅ Android 10 trở lên → dùng WifiNetworkSuggestion
                val suggestion = WifiNetworkSuggestion.Builder()
                    .setSsid(allowedSSID)
                    .setWpa2Passphrase(allowedPassword)
                    .setIsAppInteractionRequired(false) // auto connect, không cần user bấm
                    .build()

                val suggestionsList = listOf(suggestion)
                val status = wifiManager.addNetworkSuggestions(suggestionsList)
                if (status != WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
                    Log.e("WifiEnforcer", "Thêm WiFi suggestion thất bại: $status")
                } else {
                    Log.d("WifiEnforcer", "Đã add suggestion WiFi $allowedSSID")
                }
            }
        }
    }

}
