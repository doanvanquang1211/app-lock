package com.example.kioskagent.agent.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, KioskBackgroundService::class.java)
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
