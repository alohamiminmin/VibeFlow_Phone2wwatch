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

    // パッケージ名→最終処理時刻（重複防止）
    private val processedPkgs = mutableMapOf<String, Long>()

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "NotificationWatcherService: 接続OK")
        setupNotificationChannel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        val key = sbn.key
        val now = System.currentTimeMillis()

        // 自分のアプリの通知は無視
        if (pkg == applicationContext.packageName) return

        // 同じアプリから3秒以内の通知は無視
        val lastTime = processedPkgs[pkg] ?: 0L
        if (now - lastTime < 3000L) {
            Log.d(TAG, "重複スキップ($pkg): ${now - lastTime}ms以内")
            return
        }

        val pattern = AppVibeSettings.getPattern(applicationContext, pkg) ?: run {
            Log.d(TAG, "対象外アプリ: $pkg")
            return
        }
        if (pattern == VibePattern.NONE) return

        // 処理時刻を記録
        processedPkgs[pkg] = now

        Log.d(TAG, "通知処理開始: pkg=$pkg pattern=${pattern.name} key=$key")

        scope.launch(Dispatchers.Main) {
            try {
                // 元通知をキャンセル（標準バイブを止める）
                cancelNotification(key)
                Log.d(TAG, "元通知キャンセル: $pkg")

                // バイブなし通知として再投稿
                repostWithoutVibration(sbn)

                // Watch側にカスタムバイブ送信
                scope.launch(Dispatchers.IO) {
                    VibeSender.send(applicationContext, pkg, pattern)
                    Log.d(TAG, "カスタムバイブ送信完了: ${pattern.name}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "エラー: ${e.message}")
            }
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

    private fun repostWithoutVibration(sbn: StatusBarNotification) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val extras = sbn.notification.extras
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

        val notifId = (sbn.id.toLong() + 900000L).toInt()
        nm.notify(notifId, newNotification)
        Log.d(TAG, "バイブなし再投稿完了: title=$title notifId=$notifId")
    }

    private fun setupNotificationChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID, "VibeFlow通知",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            enableVibration(false)
            vibrationPattern = longArrayOf(0L)
            setSound(null, null)
        }
        nm.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "vibeflow_silent"
    }
}