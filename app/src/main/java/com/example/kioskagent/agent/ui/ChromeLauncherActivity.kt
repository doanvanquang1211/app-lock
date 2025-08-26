package com.example.kioskagent.agent.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.MediaStore
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.kioskagent.agent.AdminReceiver

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.GestureDetector.SimpleOnGestureListener
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import java.io.File
import kotlin.math.abs

class ChromeLauncherActivity : Activity() {

    private lateinit var dpm: DevicePolicyManager
    private lateinit var adminComponent: ComponentName
    private lateinit var webView: WebView
    private lateinit var gestureDetector: GestureDetector
    private lateinit var wakeLock: PowerManager.WakeLock

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, AdminReceiver::class.java)

        if (dpm.isDeviceOwnerApp(packageName)) {
            dpm.setLockTaskPackages(adminComponent, arrayOf(packageName))
            dpm.setKeyguardDisabled(adminComponent, true)
            dpm.setMaximumTimeToLock(adminComponent, 0)
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = pm.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "KioskApp::WakeLock"
            )
            wakeLock.acquire()
        } else {
            Log.e("WebKiosk", "App is NOT device owner, kiosk won't fully work!")
        }

        // Config WebView
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = object : WebChromeClient() {
            private var filePathCallback: ValueCallback<Array<Uri>>? = null
            private var imageUri: Uri? = null

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                this.filePathCallback = filePathCallback

                // Tạo file ảnh tạm trong cache (không lưu thư viện)
                val photoFile = File.createTempFile("capture_", ".jpg", cacheDir)
                imageUri = FileProvider.getUriForFile(
                    this@ChromeLauncherActivity,
                    "${packageName}.fileprovider",
                    photoFile
                )

                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                    addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }

                startActivityForResult(intent, 1001)
                return true
            }
        }
        webView.loadUrl("https://www.google.com/")

        // Khởi tạo gesture
        gestureDetector = GestureDetector(this, object : SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null || e2 == null) return false
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y
                return if (abs(diffX) > abs(diffY)
                    && abs(diffX) > SWIPE_THRESHOLD
                    && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD
                ) {
                    if (diffX > 0) {
                        // Vuốt phải → back
                        if (webView.canGoBack()) webView.goBack()
                    } else {
                        // Vuốt trái → forward
                        if (webView.canGoForward()) webView.goForward()
                    }
                    true
                } else {
                    false
                }
            }
        })

        // Gán touch listener cho WebView
        webView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.tag = event.x // lưu vị trí bắt đầu
                }
                MotionEvent.ACTION_UP -> {
                    val startX = v.tag as? Float ?: return@setOnTouchListener false
                    val endX = event.x
                    val diffX = endX - startX

                    val edgeSize = 100 * resources.displayMetrics.density // 100dp mép
                    if (Math.abs(diffX) > 200) { // vuốt đủ xa
                        if (startX < edgeSize && diffX > 0) {
                            // Vuốt từ mép trái → Back
                            if (webView.canGoBack()) webView.goBack()
                            return@setOnTouchListener true
                        } else if (startX > v.width - edgeSize && diffX < 0) {
                            // Vuốt từ mép phải → Forward
                            if (webView.canGoForward()) webView.goForward()
                            return@setOnTouchListener true
                        }
                    }
                }
            }
            false // cho WebView xử lý tiếp
        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1001) {
            val webChromeClient = webView.webChromeClient as? WebChromeClient
            if (webChromeClient != null) {
                val field = webChromeClient.javaClass.getDeclaredField("filePathCallback")
                field.isAccessible = true
                val callback = field.get(webChromeClient) as? ValueCallback<Array<Uri>>
                val imageUriField = webChromeClient.javaClass.getDeclaredField("imageUri")
                imageUriField.isAccessible = true
                val imageUri = imageUriField.get(webChromeClient) as? Uri

                if (resultCode == Activity.RESULT_OK && imageUri != null) {
                    callback?.onReceiveValue(arrayOf(imageUri))
                } else {
                    callback?.onReceiveValue(null)
                }
            }
        }
    }


    override fun onResume() {
        super.onResume()
        enterKioskMode()
    }


    override fun onDestroy() {
        super.onDestroy()
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }

    private fun enterKioskMode() {
        try {
            if (dpm.isLockTaskPermitted(packageName)) {
                Log.d("WebKiosk", "Entering lock task mode")
                startLockTask()
            } else {
                Log.e("WebKiosk", "LockTask not permitted for this package")
            }
        } catch (e: Exception) {
            Log.e("WebKiosk", "startLockTask failed: ${e.message}")
        }
    }



    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } // chặn thoát Activity
    }
}
