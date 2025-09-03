package com.example.kioskagent

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.kioskagent.agent.service.KioskBackgroundService
import com.example.kioskagent.agent.ui.ChromeLauncherActivity


class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Khởi động Service nền
        startService(Intent(this, KioskBackgroundService::class.java))

        // Khởi động Chrome trong LockTask
        startActivity(Intent(this, ChromeLauncherActivity::class.java))

        finish() //
    }
}

