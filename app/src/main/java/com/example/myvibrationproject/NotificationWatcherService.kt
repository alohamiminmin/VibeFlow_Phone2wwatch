package com.example.myvibrationproject

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotificationWatcherService : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val TAG = "VibeFlow"
    private val processedPkgs = mutableMapOf<String, Long>()

    // iPhone通知のBroadcast受信
    private val iphoneReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val type = intent.getStringExtra("type") ?: "message"
            val pattern = when (type) {
                "call"   -> VibePattern.CALL
                "wechat" -> VibePattern.WECHAT
                else     -> VibePattern.DOUBLE
            }
            Log.d(TAG, "iPhone Broadcast受信: type=$type pattern=${pattern.name}")
            scope.launch {
                VibeSender.send(applicationContext, "iphone", pattern)
                Log.d(TAG, "Watch送信完了: ${pattern.name}")
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "NotificationWatcherService: 接続OK")

        // iPhone通知のBroadcast受信を登録
        val filter = IntentFilter("com.example.myvibrationproject.IPHONE_NOTIFY")
        registerReceiver(iphoneReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        try {
            unregisterReceiver(iphoneReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "unregisterReceiver失敗: ${e.message}")
        }
    }
0
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        val now = System.currentTimeMillis()

        if (pkg == applicationContext.packageName) return

        val lastTime = processedPkgs[pkg] ?: 0L
        if (now - lastTime < 3000L) {
            Log.d(TAG, "重複スキップ($pkg): ${now - lastTime}ms以内")
            return
        }

        val pattern = AppVibeSettings.getPattern(applicationContext, pkg)

        if (pattern == null) {
            AppVibeSettings.addCandidate(applicationContext, pkg)
            Log.d(TAG, "候補追加: $pkg")
            return
        }

        if (pattern == VibePattern.NONE) return

        processedPkgs[pkg] = now

        Log.d(TAG, "通知処理開始: pkg=$pkg pattern=${pattern.name}")

        scope.launch {
            VibeSender.send(applicationContext, pkg, pattern)
            Log.d(TAG, "カスタムバイブ送信完了: ${pattern.name}")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        val pattern = AppVibeSettings.getPattern(applicationContext, sbn.packageName)
        if (pattern == VibePattern.CALL) {
            scope.launch(Dispatchers.IO) {
                VibeSender.stop(applicationContext)
            }
        }
    }
}