package com.example.kioskagent.agent.ui

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.kioskagent.agent.AdminReceiver
import com.google.androidbrowserhelper.trusted.TwaLauncher
import androidx.core.net.toUri

class ChromeLauncherActivity : AppCompatActivity() {

    private lateinit var dpm: DevicePolicyManager
    private lateinit var admin: ComponentName
    private lateinit var twaLauncher: TwaLauncher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        admin = ComponentName(this, AdminReceiver::class.java)
        twaLauncher = TwaLauncher(this)

        if (dpm.isDeviceOwnerApp(packageName)) {
            Log.d("KioskAgent", "App is Device Owner")

            // chỉ cho phép lock chính app này
            dpm.setLockTaskPackages(admin, arrayOf(packageName,"com.android.chrome"))

            // mở TWA
            twaLauncher.launch("https://www.youtube.com/".toUri())

            // bật kiosk lock task (sau khi TWA chạy)
            window.decorView.postDelayed({
                try {
                    startLockTask()
                } catch (e: Exception) {
                    Log.e("KioskAgent", "LockTask failed: ${e.message}")
                }
            }, 2000)

        } else {
            Log.e("KioskAgent", "App is NOT Device Owner")
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        twaLauncher.destroy()
    }
}
