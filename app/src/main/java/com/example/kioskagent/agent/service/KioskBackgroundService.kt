package com.example.kioskagent.agent.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.*
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.UserManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.example.kioskagent.agent.AdminReceiver
import com.example.kioskagent.agent.ForegroundAppDetector
import com.example.kioskagent.agent.ui.LockActivity

class KioskBackgroundService : Service() {

    private lateinit var dpm: DevicePolicyManager
    private lateinit var adminComponent: ComponentName
    private val allowedSSID = "TC"
    private val allowedPassword = "Tcom123$567*"

    private lateinit var detector: ForegroundAppDetector
    private val handler = Handler(Looper.getMainLooper())
    private val interval = 500L // ms

    // lưu danh sách app đã nhập đúng pass (unlock tạm thời)
    private val unlockedApps = mutableSetOf<String>()

    private val monitorRunnable = object : Runnable {
        override fun run() {
//            checkWifi()
            val top = detector.getTopPackage()
            if (top != null) {
                if (top == "com.android.launcher") {
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

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, AdminReceiver::class.java)
        detector = ForegroundAppDetector(this)
        startAsForeground()
        Log.d("KioskService", "isDeviceOwner = ${dpm.isDeviceOwnerApp(packageName)}")

        // Kiosk restrictions
        if (dpm.isDeviceOwnerApp(packageName)) {
            // Chặn cài/xóa app
            dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_INSTALL_APPS)
            dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_UNINSTALL_APPS)
            dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_CONFIG_WIFI)
            dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_CONFIG_BLUETOOTH)
            // Chỉ cho phép Chrome chạy trong LockTask
            dpm.setLockTaskPackages(adminComponent, arrayOf("com.android.chrome"))

            dpm.setKeyguardDisabled(adminComponent, true)
            dpm.setMaximumTimeToLock(adminComponent, 0)

        }

        // Kết nối WiFi
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            connectAllowedWifi()
        }

        // Mở Chrome ở kiosk mode
//        openChrome()

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


    @RequiresApi(Build.VERSION_CODES.Q)
    private fun connectAllowedWifi() {
        val specifier = WifiNetworkSpecifier.Builder()
            .setSsid(allowedSSID)
            .setWpa2Passphrase(allowedPassword)
            .build()

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .setNetworkSpecifier(specifier)
            .build()

        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        connectivityManager.requestNetwork(
            request,
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    connectivityManager.bindProcessToNetwork(network)
                }
            })
    }

    private fun checkWifi() {
        val wifiManager =
            applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val info = wifiManager.connectionInfo
        val ssid = info.ssid?.replace("\"", "")
        if (ssid != allowedSSID) {
            Log.d("KioskService", "Sai WiFi: $ssid → reconnect...")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                connectAllowedWifi()
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
