package com.example.kioskagent.agent.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.*
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.UserManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.net.toUri
import com.example.kioskagent.agent.AdminReceiver
import com.example.kioskagent.agent.ForegroundAppDetector
import com.example.kioskagent.agent.ui.LockActivity

class KioskBackgroundService : Service() {

    private val allowedSSID = "TC"
    private val allowedPassword = "Tcom123$567*"

    private lateinit var detector: ForegroundAppDetector
    private val handler = Handler(Looper.getMainLooper())
    private val interval = 500L // ms

    // lưu danh sách app đã nhập đúng pass (unlock tạm thời)
    private val unlockedApps = mutableSetOf<String>()

    private val monitorRunnable = object : Runnable {
        override fun run() {
            val top = detector.getTopPackage()
            Log.d("mmmmm", "pppppp $top")
            val defaultLauncher = getDefaultLauncherPackage()

            if (top != null) {
                if (top == defaultLauncher || top == "com.android.chrome") {
                    // Nếu là launcher hoặc Chrome → reset danh sách unlock
                    unlockedApps.clear()
                } else {
                    if (!unlockedApps.contains(top) && top != packageName) {
                        showLockScreen(top)
                    }
                }
            }
            handler.postDelayed(this, interval)
        }
    }


    private fun getDefaultLauncherPackage(): String? {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        detector = ForegroundAppDetector(this)
        startAsForeground()

        // Bắt đầu giám sát WiFi
        handler.post(monitorRunnable)
    }


    private fun showLockScreen(targetPkg: String) {
        onUnlockSuccess(targetPkg)
        val i = Intent(this, LockActivity::class.java)
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            )
            .putExtra("target_pkg", targetPkg)
        startActivity(i)
    }

    private fun onUnlockSuccess(pkg: String) {
        unlockedApps.add(pkg)
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
        handler.removeCallbacks(monitorRunnable)
    }

    override fun onBind(intent: Intent?): IBinder? = null


    private fun checkWifi() {
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


    @RequiresApi(Build.VERSION_CODES.O)
    private fun startAsForeground() {
        val chId = "applock_channel"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val ch = NotificationChannel(chId, "App Locker", NotificationManager.IMPORTANCE_MIN)
        ch.setShowBadge(false)
        nm.createNotificationChannel(ch)
        val notif = Notification.Builder(this, chId)
            .setContentTitle("App Locker đang bảo vệ")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build()
        startForeground(1001, notif)
    }
}
