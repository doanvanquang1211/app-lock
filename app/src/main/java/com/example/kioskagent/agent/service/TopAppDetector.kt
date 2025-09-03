package com.example.kioskagent.agent.service

import android.app.usage.UsageStatsManager
import android.content.Context

class TopAppDetector(private val context: Context) {

    fun getTopPackage(): String? {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 1000 * 10 // 10s trước

        val stats = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            beginTime,
            endTime
        )

        if (stats == null || stats.isEmpty()) return null

        val recent = stats.maxByOrNull { it.lastTimeUsed }
        return recent?.packageName
    }
}
