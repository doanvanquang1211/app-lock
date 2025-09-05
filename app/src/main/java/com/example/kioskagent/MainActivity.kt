package com.example.kioskagent


import android.app.usage.UsageStatsManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import com.example.kioskagent.agent.LockRepository
import com.example.kioskagent.agent.service.KioskBackgroundService

class MainActivity : ComponentActivity() {
    private lateinit var repo: LockRepository
    private val launchablePkgs = mutableListOf<String>()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = LockRepository(this)
        setContentView(R.layout.activity_main)

        val etPwd = findViewById<EditText>(R.id.etPwd)
        val btnSave = findViewById<Button>(R.id.btnSavePwd)

        // load app launchable
        loadLaunchableApps()

        // lần đầu mở app → mặc định lock tất cả trừ Chrome
        val locked = repo.getLockedPackages()
        if (locked.isEmpty()) {
            val defaultLocked = launchablePkgs
                .filter { it != "com.android.chrome" } // ⚡ luôn bỏ Chrome ra
                .toMutableSet()
            repo.setLockedPackages(defaultLocked)
        }
        // Save password
        etPwd.setText(repo.getPassword() ?: "")
        btnSave.setOnClickListener {
            val pwd = etPwd.text.toString()
            if (pwd.isBlank()) {
                Toast.makeText(this, "Mật khẩu không được trống", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            repo.setPassword(pwd)
            Toast.makeText(this, "Đã lưu mật khẩu", Toast.LENGTH_SHORT).show()

            // Sau khi lưu → kiểm tra quyền ngay lần đầu
            checkPermissions()
        }
    }

    override fun onResume() {
        super.onResume()
        // mỗi lần quay lại màn hình → kiểm tra quyền tiếp
        if (repo.getPassword() != null) {
            checkPermissions()
        }
    }

    private fun checkPermissions() {
        when {
            !hasUsageAccess() -> {
                Toast.makeText(this, "Vui lòng cấp quyền Usage Access", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
            !Settings.canDrawOverlays(this) -> {
                Toast.makeText(this, "Vui lòng cấp quyền Overlay", Toast.LENGTH_LONG).show()
                val i = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(i)
            }
            else -> {
                Toast.makeText(this, "Đã có đủ quyền", Toast.LENGTH_SHORT).show()
                startForegroundService(Intent(this, KioskBackgroundService::class.java))
            }
        }
    }

    private fun hasUsageAccess(): Boolean {
        return try {
            val usm = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
            val end = System.currentTimeMillis()
            val begin = end - 1000 * 60
            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, begin, end)
            stats != null && stats.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    private fun loadLaunchableApps() {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val resolves = pm.queryIntentActivities(intent, 0)
        launchablePkgs.clear()
        resolves.map { it.activityInfo.packageName }
            .filter { it != packageName } // bỏ chính mình
            .distinct()
            .sorted()
            .forEach { launchablePkgs.add(it) }
    }
}
