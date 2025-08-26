package com.example.kioskagent.agent.service

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
import android.os.UserManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import com.example.kioskagent.agent.AdminReceiver

class KioskBackgroundService : Service() {

    private lateinit var dpm: DevicePolicyManager
    private lateinit var adminComponent: ComponentName
    private val allowedSSID = "TC"
    private val allowedPassword = "Tcom123$567*"

    private val handler = Handler()
    private val monitorRunnable = object : Runnable {
        override fun run() {
//            checkWifi()
            handler.postDelayed(this, 2000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, AdminReceiver::class.java)

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
        }

        // Kết nối WiFi
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            connectAllowedWifi()
        }

        // Mở Chrome ở kiosk mode
        openChrome()

        // Bắt đầu giám sát WiFi
//        handler.post(monitorRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun openChrome() {
        val url = "https://www.youtube.com/".toUri()
        val intent = Intent(Intent.ACTION_VIEW, url).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            setPackage("com.android.chrome")
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            // fallback nếu không có Chrome
            startActivity(
                Intent(Intent.ACTION_VIEW, url).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            )
        }
    }

//    @RequiresApi(Build.VERSION_CODES.Q)
//    private fun connectAllowedWifi() {
//        val specifier = WifiNetworkSpecifier.Builder()
//            .setSsid(allowedSSID)
//            .setWpa2Passphrase(allowedPassword)
//            .build()
//
//        val request = NetworkRequest.Builder()
//            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
//            .setNetworkSpecifier(specifier)
//            .build()
//
//        val connectivityManager =
//            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//
//        connectivityManager.requestNetwork(
//            request,
//            object : ConnectivityManager.NetworkCallback() {
//                override fun onAvailable(network: Network) {
//                    connectivityManager.bindProcessToNetwork(network)
//                }
//            })
//    }
//
//    private fun checkWifi() {
//        val wifiManager =
//            applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
//        val info = wifiManager.connectionInfo
//        val ssid = info.ssid?.replace("\"", "")
//        if (ssid != allowedSSID) {
//            Log.d("KioskService", "Sai WiFi: $ssid → reconnect...")
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                connectAllowedWifi()
//            }
//        }
//    }
}
