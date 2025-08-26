package com.example.kioskagent.agent

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class CommandPollWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val policy = PolicyManager(applicationContext)
        try {
            val response = httpGet("https://your-server.example.com/device/commands?deviceId=DEVICE_ID")
            if (response != null) {
                val j = JSONObject(response)
                val cmd = j.optString("command", "NONE")
                Log.i(TAG, "Polled command = $cmd")
                when (cmd.uppercase()) {
                    "LOCK" -> policy.lockNow()
                    "WIPE" -> policy.wipeNow()
                    else -> Unit
                }
            }
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "poll failed: ${e.message}")
            return Result.retry()
        }
    }


    private fun httpGet(urlStr: String): String? {
        var conn: HttpURLConnection? = null
        try {
            val url = URL(urlStr)
            conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                requestMethod = "GET"
            }
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) return null
            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) sb.append(line)
            reader.close()
            return sb.toString()
        } finally {
            conn?.disconnect()
        }
    }


    companion object { private const val TAG = "CommandPollWorker" }
}