package com.example.kioskagent.agent

import android.content.Context
import android.os.Build
import androidx.annotation.WorkerThread
import androidx.core.content.edit
import java.util.concurrent.ConcurrentHashMap

class LockRepository(context: Context) {
    private val prefs = context.getSharedPreferences("locker_prefs", Context.MODE_PRIVATE)

    fun setPassword(pass: String) = prefs.edit { putString("pwd", pass) }
    fun getPassword(): String? = prefs.getString("pwd", null)

    fun getLockedPackages(): Set<String> =
        prefs.getStringSet("locked_pkgs", emptySet()) ?: emptySet()

    fun setLockedPackages(set: Set<String>) = prefs.edit { putStringSet("locked_pkgs", set) }

    // Kiểm tra lần đầu chạy
    fun isFirstRun(): Boolean {
        return prefs.getBoolean("first_run", true)
    }

    // Đánh dấu đã chạy lần đầu
    fun setFirstRunDone() {
        prefs.edit().putBoolean("first_run", false).apply()
    }

    fun addLockedPackage(pkg: String) {
        val cur = getLockedPackages().toMutableSet()
        cur.add(pkg)
        setLockedPackages(cur)
    }

    fun removeLockedPackage(pkg: String) {
        val cur = getLockedPackages().toMutableSet()
        cur.remove(pkg)
        setLockedPackages(cur)
    }

    // Cho phép “grace period” sau khi mở khóa 1 app để không hỏi lại ngay
    private val allowUntil = ConcurrentHashMap<String, Long>()

    fun allowPackageFor(pkg: String, millis: Long) {
        allowUntil[pkg] = System.currentTimeMillis() + millis
    }

    fun isTemporarilyAllowed(pkg: String): Boolean {
        val until = allowUntil[pkg] ?: return false
        if (System.currentTimeMillis() <= until) return true
        allowUntil.remove(pkg)
        return false
    }
}

