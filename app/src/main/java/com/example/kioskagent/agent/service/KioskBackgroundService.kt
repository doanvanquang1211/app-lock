package com.example.kioskagent.agent.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.kioskagent.agent.ui.LockActivity
import com.example.kioskagent.agent.ForegroundAppDetector

class KioskBackgroundService : Service() {

    private lateinit var detector: ForegroundAppDetector
    private val unlockedApps = mutableSetOf<String>()

    private val handler = Handler(Looper.getMainLooper())
    private val intervalRealtime = 200L // 0.2s -> realtime check
    private val intervalBackup = 500L // 0.5s -> Alarm backup nếu service chết

    private val monitorRunnable = object : Runnable {
        override fun run() {
            runMonitor()
            handler.postDelayed(this, intervalRealtime)
        }
    }

    override fun onCreate() {
        super.onCreate()
        detector = ForegroundAppDetector(this)
        startAsForeground()

        // Bắt đầu realtime monitor
        handler.post(monitorRunnable)

        // Lên lịch backup để service được gọi lại nếu chết
        scheduleNextCheck()
    }

    private fun runMonitor() {
        val top = detector.getTopPackage()
        Log.d("mmmmm", "pppppp $top")
        val defaultLauncher = getDefaultLauncherPackage()

        if (top != null) {
            if (top == defaultLauncher || top == "com.android.chrome") {
                unlockedApps.clear()
            } else if (!unlockedApps.contains(top) && top != packageName) {
                showLockScreen(top)
            }
        }
    }

    private fun getDefaultLauncherPackage(): String? {
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName
    }

    private fun showLockScreen(targetPkg: String) {
        onUnlockSuccess(targetPkg)
        val i = Intent(this, LockActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            .putExtra("target_pkg", targetPkg)
        startActivity(i)
    }

    private fun onUnlockSuccess(pkg: String) {
        unlockedApps.add(pkg)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // mỗi lần Alarm gọi lại service -> handler sẽ chạy tiếp
        if (!handler.hasCallbacks(monitorRunnable)) {
            handler.post(monitorRunnable)
        }
        scheduleNextCheck()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(monitorRunnable)

        // cancel alarm
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(getPendingIntent())
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startAsForeground() {
        val chId = "applock_channel"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(chId, "App Locker", NotificationManager.IMPORTANCE_MIN)
            nm.createNotificationChannel(ch)
        }
        val notif = NotificationCompat.Builder(this, chId)
            .setContentTitle("App Locker đang bảo vệ")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build()
        startForeground(1001, notif)
    }

    /** Backup bằng AlarmManager */
    private fun scheduleNextCheck() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerTime = System.currentTimeMillis() + intervalBackup
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            getPendingIntent()
        )
    }

    private fun getPendingIntent(): PendingIntent {
        val intent = Intent(this, BootReceiver::class.java)
        return PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
