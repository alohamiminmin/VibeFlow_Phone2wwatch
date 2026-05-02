package com.example.myvibrationproject

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotificationWatcherService : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val TAG = "VibeFlow"
    private val processedPkgs = mutableMapOf<String, Long>()

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "NotificationWatcherService: 接続OK")
    }

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

        // cancelなし・通知テキストはそのまま残す
        // カスタムバイブだけWatchに送信
        scope.launch(Dispatchers.IO) {
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