package com.example.kioskagent.agent.service

import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.net.toUri

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            // Start service
            val s = Intent(context, KioskBackgroundService::class.java)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(s) else context.startService(s)

            // Launch Chrome to URL
            val url = "https://www.youtube.com/" // 🔁 THAY
            val i = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                setPackage("com.android.chrome")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            // Chỉ startActivity nếu có Chrome
            val pm: PackageManager = context.packageManager
            i.resolveActivity(pm)?.let { context.startActivity(i) }
        }
    }
}
