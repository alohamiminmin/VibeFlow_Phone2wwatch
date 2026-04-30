package com.example.myvibrationproject

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

    private val appPatternMap = mapOf(
        "com.tencent.mm"                    to "wechat",
        "com.android.phone"                 to "call",
        "com.samsung.android.incallui"      to "call",
        "com.google.android.dialer"         to "call",
        "com.google.android.gm"             to "message",
        "com.google.android.apps.messaging" to "message",
        "jp.naver.line.android"             to "message",
    )

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "NotificationWatcherService: 接続OK")  // ← 起動確認
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        Log.d(TAG, "通知受信: pkg=${sbn.packageName}")  // ← 全通知をログ出力

        val pattern = appPatternMap[sbn.packageName] ?: return

        Log.d(TAG, "パターン送信: $pattern → Watch")
        scope.launch {
            VibeSender.send(applicationContext, pattern)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (appPatternMap[sbn.packageName] == "call") {
            scope.launch { VibeSender.stop(applicationContext) }
        }
    }
}