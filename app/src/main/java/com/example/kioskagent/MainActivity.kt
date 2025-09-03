package com.example.kioskagent

import android.app.usage.UsageStatsManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.activity.ComponentActivity
import com.example.kioskagent.agent.LockRepository
import com.example.kioskagent.agent.service.KioskBackgroundService

class MainActivity : ComponentActivity() {
    private lateinit var repo: LockRepository
    private lateinit var listView: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private val launchablePkgs = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = LockRepository(this)
        setContentView(R.layout.activity_main)

        val etPwd = findViewById<EditText>(R.id.etPwd)
        val btnSave = findViewById<Button>(R.id.btnSavePwd)
        val tvUsage = findViewById<TextView>(R.id.tvUsageStatus)
        val tvOverlay = findViewById<TextView>(R.id.tvOverlayStatus)
        val btnReqUsage = findViewById<Button>(R.id.btnReqUsage)
        val btnReqOverlay = findViewById<Button>(R.id.btnReqOverlay)
        val btnStart = findViewById<Button>(R.id.btnStartService)
        val btnStop = findViewById<Button>(R.id.btnStopService)

        listView = findViewById(R.id.listView)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_multiple_choice, launchablePkgs)
        listView.adapter = adapter
        listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE

        // load app launchable
        loadLaunchableApps()

        // Sync checked from repo
        val locked = repo.getLockedPackages()
        for (i in launchablePkgs.indices) {
            if (locked.contains(launchablePkgs[i])) listView.setItemChecked(i, true)
        }

        // Save password
        etPwd.setText(repo.getPassword() ?: "")
        btnSave.setOnClickListener {
            repo.setPassword(etPwd.text.toString())
            Toast.makeText(this, "Đã lưu mật khẩu", Toast.LENGTH_SHORT).show()
        }

        // Save locked list on click
        listView.setOnItemClickListener { _, _, position, _ ->
            val pkg = launchablePkgs[position]
            val newSet = repo.getLockedPackages().toMutableSet().apply {
                if (listView.isItemChecked(position)) add(pkg) else remove(pkg)
            }
            repo.setLockedPackages(newSet)
        }

        // Permission status
        fun refreshStatus() {
            tvUsage.text = if (hasUsageAccess()) "Usage Access: OK" else "Usage Access: CHƯA CẤP"
            tvOverlay.text = if (Settings.canDrawOverlays(this)) "Overlay: OK" else "Overlay: CHƯA CẤP"
        }
        refreshStatus()

        btnReqUsage.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        btnReqOverlay.setOnClickListener {
            val i = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(i)
        }

        btnStart.setOnClickListener {
            startForegroundService(Intent(this, KioskBackgroundService::class.java))
            Toast.makeText(this, "Đã bật bảo vệ", Toast.LENGTH_SHORT).show()
        }
        btnStop.setOnClickListener {
            stopService(Intent(this, KioskBackgroundService::class.java))
            Toast.makeText(this, "Đã tắt bảo vệ", Toast.LENGTH_SHORT).show()
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
        adapter.notifyDataSetChanged()
    }
}
