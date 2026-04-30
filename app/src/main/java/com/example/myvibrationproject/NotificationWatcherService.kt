package com.example.myvibrationproject

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotificationWatcherService : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val TAG = "VibeFlow"

    // 再投稿した通知のIDを記録（無限ループ防止）
    private val repostedKeys = mutableSetOf<String>()

    // 重複防止用
    private val recentKeys = mutableSetOf<String>()

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
        Log.d(TAG, "NotificationWatcherService: 接続OK")
        setupNotificationChannel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        Log.d(TAG, "通知受信: pkg=$pkg key=${sbn.key}")

        // 自分のアプリの通知は無視
        if (pkg == applicationContext.packageName) return

        // 重複防止：同じキーは1秒以内に再処理しない
        val key = sbn.key
        if (recentKeys.contains(key)) return
        recentKeys.add(key)
        scope.launch {
            kotlinx.coroutines.delay(1000L)
            recentKeys.remove(key)
        }

        // 再投稿した通知は無視
        if (repostedKeys.contains(key)) {
            repostedKeys.remove(key)
            return
        }

        val pattern = appPatternMap[pkg] ?: return

        scope.launch(Dispatchers.Main) {
            try {
                // 元の通知をキャンセル
                cancelNotification(sbn.key)
                Log.d(TAG, "元通知キャンセル: $pkg")

                // バイブなし通知として再投稿
                repostWithoutVibration(sbn)

                // Watch側にカスタムバイブ送信
                scope.launch(Dispatchers.IO) {
                    VibeSender.send(applicationContext, pattern)
                    Log.d(TAG, "パターン送信: $pattern")
                }

            } catch (e: Exception) {
                Log.e(TAG, "エラー: ${e.message}")
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (appPatternMap[sbn.packageName] == "call") {
            scope.launch(Dispatchers.IO) {
                VibeSender.stop(applicationContext)
            }
        }
    }

    private fun repostWithoutVibration(sbn: StatusBarNotification) {
        val original = sbn.notification
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val extras = original.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        val newNotification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setVibrate(longArrayOf(0L))
            .setSilent(true)
            .setAutoCancel(true)
            .build()

        val notifId = sbn.id + 10000
        repostedKeys.add("$packageName|$notifId|${sbn.uid}")
        nm.notify(notifId, newNotification)
        Log.d(TAG, "バイブなし再投稿完了: $title")
    }

    private fun setupNotificationChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "VibeFlow通知",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            enableVibration(false)
            vibrationPattern = longArrayOf(0L)
        }
        nm.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "vibeflow_silent"
    }
}