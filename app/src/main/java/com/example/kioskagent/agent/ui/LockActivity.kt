package com.example.kioskagent.agent.ui

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import com.example.kioskagent.R
import com.example.kioskagent.agent.LockRepository

class LockActivity : ComponentActivity() {
    private lateinit var repo: LockRepository
    private var targetPkg: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = LockRepository(this)
        setContentView(R.layout.activity_lock)

        targetPkg = intent.getStringExtra("target_pkg")

        val tvTitle = findViewById<TextView>(R.id.tvTitle)
        val et = findViewById<EditText>(R.id.etPassword)
        val btn = findViewById<Button>(R.id.btnUnlock)
        val tvErr = findViewById<TextView>(R.id.tvError)

        tvTitle.text = "Đã khóa: ${targetPkg ?: ""}"

        btn.setOnClickListener {
            val pwd = repo.getPassword()
            Log.d("dđ", "xxxxxx$pwd")
            if (pwd.isNullOrEmpty()) {
                // Chưa đặt mật khẩu → coi như cho qua và nhắc
                Toast.makeText(this, "Chưa đặt mật khẩu – vào Main để đặt.", Toast.LENGTH_SHORT).show()
                allowAndFinish()
                return@setOnClickListener
            }
            if (et.text.toString() == pwd) {
                allowAndFinish()
            } else {
                tvErr.visibility = View.VISIBLE
                tvErr.text = "Sai mật khẩu!"
                et.text.clear()
            }
        }
    }

    private fun allowAndFinish() {
        targetPkg?.let { pkg ->
            // Cho phép tạm 30s (tùy đưa về 0 nếu muốn hỏi mỗi lần)
            repo.allowPackageFor(pkg, 30_000)
        }
        finish()
    }

    // Chặn back / recent…
    override fun onBackPressed() { /* no-op */ }
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_APP_SWITCH ||
            event.keyCode == KeyEvent.KEYCODE_HOME) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}
