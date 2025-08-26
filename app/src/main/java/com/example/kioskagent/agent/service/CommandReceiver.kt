package com.example.kioskagent.agent.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.kioskagent.agent.PolicyManager


/**
 * Allows remote control via broadcast intents (ADB, your MDM, or server->FCM->Broadcast).
 * Actions:
 * - com.yourcompany.youractivity.agent.LOCK_NOW
 * - com.yourcompany.youractivity.agent.WIPE_NOW
 */
class CommandReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val policy = PolicyManager(context)
        when (action) {
            ACTION_LOCK -> {
                Log.i(TAG, "Received ACTION_LOCK")
                policy.lockNow()
            }
            ACTION_WIPE -> {
                Log.i(TAG, "Received ACTION_WIPE")
                policy.wipeNow()
            }
        }
    }


    companion object {
        private const val TAG = "CommandReceiver"
        const val ACTION_LOCK = "com.example.kioskagent.ACTION_LOCK_NOW"
        const val ACTION_WIPE = "com.example.kioskagent.ACTION_WIPE_NOW"
    }
}