package com.example.kioskagent.agent.ui

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.UserManager
import android.util.Log
import com.example.kioskagent.agent.AdminReceiver

class ChromeLauncherActivity : Activity() {

    private lateinit var dpm: DevicePolicyManager
    private lateinit var adminComponent: ComponentName
    private val chromePkg = "com.android.chrome"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, AdminReceiver::class.java)

        if (dpm.isDeviceOwnerApp(packageName)) {
            Log.d("Kiosk", "App là Device Owner")
            setupKiosk()
            openChromeAndLockTask()
            finish()
        } else {
            Log.e("Kiosk", "❌ App chưa được set Device Owner")
        }
    }

    private fun setupKiosk() {
        // Chỉ cho phép chạy Chrome trong LockTask
        dpm.setLockTaskPackages(adminComponent, arrayOf(chromePkg))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            dpm.setLockTaskFeatures(adminComponent, DevicePolicyManager.LOCK_TASK_FEATURE_NONE)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_CONFIG_WIFI)
            Log.d("KioskService", "✅ Đã chặn thay đổi Wi-Fi trong Settings")
        }

        // Xoá tất cả HOME intent đã được set
        clearAllHomeActivities()

        // (Tùy chọn) đặt Chrome làm launcher mặc định duy nhất
        val filter = IntentFilter(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addCategory(Intent.CATEGORY_DEFAULT)
        }
        val chromeHome = ComponentName(chromePkg, "com.google.android.apps.chrome.Main")
        dpm.addPersistentPreferredActivity(adminComponent, filter, chromeHome)

        Log.d("Kiosk", "✅ Đã cấu hình kiosk chỉ chạy Chrome")
    }

    override fun onBackPressed() {
        // Không làm gì → chặn nút back
    }

    private fun clearAllHomeActivities() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addCategory(Intent.CATEGORY_DEFAULT)
        }
        val resolveInfos = packageManager.queryIntentActivities(intent, 0)
        for (resolve in resolveInfos) {
            val pkg = resolve.activityInfo.packageName
            Log.d("Kiosk", "Clear HOME for $pkg")
            dpm.clearPackagePersistentPreferredActivities(adminComponent, pkg)
        }
    }

    private fun openChromeAndLockTask() {
        try {
            val intent = packageManager.getLaunchIntentForPackage(chromePkg)
                ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
                    `package` = chromePkg
                }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)

            // 🔒 Ép Chrome chạy lock task
            if (dpm.isLockTaskPermitted(chromePkg)) {
                Log.d("Kiosk", "🚀 LockTask cho Chrome")
                startLockTask()
            } else {
                Log.e("Kiosk", "❌ Chrome chưa được phép LockTask")
            }
        } catch (e: Exception) {
            Log.e("Kiosk", "Không thể mở Chrome: ${e.message}")
        }
    }
}
