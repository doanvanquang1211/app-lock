package com.example.kioskagent.agent.service

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Intent
import androidx.core.content.ContextCompat

class RestartServiceJob : JobService() {
    override fun onStartJob(params: JobParameters?): Boolean {
        val serviceIntent = Intent(this, KioskBackgroundService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        return false // job không còn chạy sau khi start service
    }

    override fun onStopJob(params: JobParameters?): Boolean = true
}
