package com.example.kioskagent.agent

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.SystemClock

/**
 * Dò app foreground bằng UsageEvents (độ chính xác cao hơn queryUsageStats sort lastTimeUsed).
 */
class ForegroundAppDetector(private val context: Context) {
    fun getTopPackage(): String? {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val begin = end - 1000 * 60 // 10s gần nhất
        val events = usm.queryEvents(begin, end) ?: return null
        val event = UsageEvents.Event()
        var lastPkg: String? = null
        var lastTime = 0L

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
            ) {
                if (event.timeStamp >= lastTime) {
                    lastTime = event.timeStamp
                    lastPkg = event.packageName
                }
            }
        }
        return lastPkg
    }
}

