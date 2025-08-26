package com.example.kioskagent.agent.service


import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.util.Log
import com.example.kioskagent.agent.AdminReceiver

class KioskBackgroundService : Service() {

    private lateinit var dpm: DevicePolicyManager
    private lateinit var admin: ComponentName
    private val handler = Handler()
    private val checkInterval = 2000L

    override fun onCreate() {
        super.onCreate()
        dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        admin = ComponentName(this, AdminReceiver::class.java)

        handler.post(checkTask)
    }

    private val checkTask = object : Runnable {
        override fun run() {
            try {
                val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val task = am.appTasks.firstOrNull()?.taskInfo?.topActivity?.packageName
                Log.d("KioskService", "Foreground app: $task")

                if (task == "com.android.chrome") {
                    if (!dpm.isLockTaskPermitted(task)) {
                        dpm.setLockTaskPackages(admin, arrayOf("com.android.chrome"))
                    }
                }
            } catch (e: Exception) {
                Log.e("KioskService", "Error: ${e.message}")
            }

            handler.postDelayed(this, checkInterval)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
