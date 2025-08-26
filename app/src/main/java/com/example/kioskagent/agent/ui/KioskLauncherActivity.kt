package com.example.kioskagent.agent.ui

import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.kioskagent.agent.AdminReceiver
import android.net.wifi.WifiNetworkSpecifier

class KioskLauncherActivity : Service() {

    private lateinit var dpm: DevicePolicyManager
    private lateinit var adminComponent: ComponentName
    private val allowedSSID = "TC"
    private val allowedPassword = "Tcom123$567*"

    private val handler = Handler()
    private val monitorRunnable = object : Runnable {
        override fun run() {
            checkWifi()
            handler.postDelayed(this, 2000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, AdminReceiver::class.java)

        // Kiosk mode trên app nền (ẩn Settings)
        if (dpm.isDeviceOwnerApp(packageName)) {
            dpm.addUserRestriction(adminComponent, android.os.UserManager.DISALLOW_INSTALL_APPS)
            dpm.addUserRestriction(adminComponent, android.os.UserManager.DISALLOW_UNINSTALL_APPS)
            try {
                dpm.setApplicationHidden(adminComponent, "com.android.settings", true)
            } catch (e: SecurityException) { e.printStackTrace() }
        }

        // Kết nối WiFi
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            connectAllowedWifi()
        }

        // Mở Chrome LockTask bằng Activity trung gian
        val intent = Intent(this, ChromeLauncherActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)

        // Start giám sát WiFi
        handler.post(monitorRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun connectAllowedWifi() {
        val specifier = WifiNetworkSpecifier.Builder()
            .setSsid(allowedSSID)
            .setWpa2Passphrase(allowedPassword)
            .build()

        val request = android.net.NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .setNetworkSpecifier(specifier)
            .build()

        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.requestNetwork(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                connectivityManager.bindProcessToNetwork(network)
            }
        })
    }

    private var isWifiAlertShowing = false

    private fun checkWifi() {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val info = wifiManager.connectionInfo
        Log.d("KioskService", "Không đúng WiFi: $info,")

        val ssid = info.ssid?.replace("\"", "")
        Log.d("KioskService", "Không đúng WiFi: $ssid, reconnect... $allowedSSID")

        if (ssid != allowedSSID) {
            Log.d("KioskService", "Không đúng WiFi: $ssid, reconnect...")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                connectAllowedWifi()
            }

            if (!isWifiAlertShowing) {
                isWifiAlertShowing = true
                val intent = Intent(this, WifiAlertActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        } else {
            isWifiAlertShowing = false // Reset khi kết nối đúng WiFi
        }
    }

}
