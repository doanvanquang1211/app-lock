package com.example.kioskagent.agent.ui

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.kioskagent.R

class WifiAlertActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wifi_alert)

        val btnReconnect: Button = findViewById(R.id.btnReconnect)
        btnReconnect.setOnClickListener {
            finish() // đóng activity, KioskService sẽ reconnect tự động
        }
    }

    override fun onBackPressed() { /* chặn back */ }
}
