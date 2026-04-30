package com.example.myvibrationproject

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotificationWatcherService : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // 監視対象アプリとバイブパターンの対応表
    private val appPatternMap = mapOf(
        "com.tencent.mm"                    to "wechat",   // WeChat
        "com.android.phone"                 to "call",     // 電話（Samsung）
        "com.samsung.android.incallui"      to "call",     // 電話（Samsung UI）
        "com.google.android.dialer"         to "call",     // Google電話
        "com.google.android.gm"             to "message",  // Gmail
        "com.google.android.apps.messaging" to "message",  // Messages
        "jp.naver.line.android"             to "message",  // LINE
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pattern = appPatternMap[sbn.packageName] ?: return
        // マップにないアプリは無視（全通知に反応させたい場合は ?: "message" に変更）

        scope.launch {
            VibeSender.send(applicationContext, pattern)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // 電話通知が消えた＝通話終了 → ループバイブを止める
        if (appPatternMap[sbn.packageName] == "call") {
            scope.launch {
                VibeSender.stop(applicationContext)
            }
        }
    }
}