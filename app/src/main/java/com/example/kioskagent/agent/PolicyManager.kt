package com.example.kioskagent.agent


import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.ContentValues.TAG
import android.content.Context
import android.os.UserManager
import android.util.Log


class PolicyManager(private val context: Context) {
    private val dpm: DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val admin = ComponentName(context, AdminReceiver::class.java)


    private fun isDeviceOwner(): Boolean = dpm.isDeviceOwnerApp(context.packageName)


    fun applyKioskPolicies(allowChrome: Boolean = true) {
        if (!isDeviceOwner()) return


// 1) LockTask whitelist: our app + Chrome
        val pkgs = mutableListOf(context.packageName)
        if (allowChrome) pkgs += "com.android.chrome"
        dpm.setLockTaskPackages(admin, pkgs.toTypedArray())


// 2) Optional: user restrictions for tighter kiosk
        dpm.addUserRestriction(admin, UserManager.DISALLOW_ADD_USER)
        dpm.addUserRestriction(admin, UserManager.DISALLOW_INSTALL_APPS)
        dpm.addUserRestriction(admin, UserManager.DISALLOW_SAFE_BOOT)
        dpm.addUserRestriction(admin, UserManager.DISALLOW_FACTORY_RESET)
        dpm.addUserRestriction(admin, UserManager.DISALLOW_USB_FILE_TRANSFER)


// 3) Optional (where supported): disable keyguard & status bar
        try { dpm.setKeyguardDisabled(admin, true) } catch (_: Throwable) {}
        try { dpm.setStatusBarDisabled(admin, true) } catch (_: Throwable) {}


// 4) Pre-grant Chrome camera/mic (requires DO)
        try {
            val GRANT = DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED
            dpm.setPermissionGrantState(admin, "com.android.chrome", android.Manifest.permission.CAMERA, GRANT)
            dpm.setPermissionGrantState(admin, "com.android.chrome", android.Manifest.permission.RECORD_AUDIO, GRANT)
        } catch (_: Throwable) {}
    }

    fun lockNow() {
        try {
            dpm.lockNow()
            Log.i(TAG, "Device locked by policy")
        } catch (e: SecurityException) {
            Log.e(TAG, "lockNow() failed: ${e.message}")
        }
    }

    fun wipeNow() {
        try {
            dpm.wipeData(0) // Xóa toàn bộ dữ liệu, factory reset
            Log.i(TAG, "Device wiped by policy")
        } catch (e: SecurityException) {
            Log.e(TAG, "wipeNow() failed: ${e.message}")
        }
    }
}