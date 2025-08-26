package com.example.kioskagent.agent.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.kioskagent.agent.AdminReceiver

class ChromeLauncherActivity : Activity() {

    private lateinit var dpm: DevicePolicyManager
    private lateinit var adminComponent: ComponentName
    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, AdminReceiver::class.java)

        // Cho phép package này chạy LockTask
        if (dpm.isDeviceOwnerApp(packageName)) {
            dpm.setLockTaskPackages(adminComponent, arrayOf(packageName))
        } else {
            Log.e("WebKiosk", "App is NOT device owner, kiosk won't fully work!")
        }

        // Cấu hình WebView
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()
        webView.loadUrl("https://www.youtube.com/") // web app của bạn
    }

    override fun onResume() {
        super.onResume()
        enterKioskMode()
    }

    private fun enterKioskMode() {
        try {
            if (dpm.isLockTaskPermitted(packageName)) {
                Log.d("WebKiosk", "Entering lock task mode")
                startLockTask()
            } else {
                Log.e("WebKiosk", "LockTask not permitted for this package")
            }
        } catch (e: Exception) {
            Log.e("WebKiosk", "startLockTask failed: ${e.message}")
        }
    }

    override fun onBackPressed() {
        // Không cho thoát ra ngoài
    }
}
