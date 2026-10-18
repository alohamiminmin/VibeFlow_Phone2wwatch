package com.example.myvibrationproject

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.Build
import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import android.service.wear.WearableMessageListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.core.app.NotificationCompat

/**
 * WearableMessageListenerを実装し、メッセージを受信した際に遅延バイブレーションをトリガーするフォアグラウンドサービス。
 * サービスがバックグラウンドに移行しても強制終了されないように設計されています。
 */
class VibeListenerService : Service(), WearableMessageListener {

    private val TAG = "VibeListenerService"

    companion object {
        private const val CHANNEL_ID = "VibrationServiceChannel"
        private const val NOTIFICATION_ID = 101 // ユニークな通知ID
    }

    private val DEFAULT_VIBRATION_DELAY = 1000L
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "VibeListenerService created.")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // サービス開始時にフォアグラウンドサービスとして起動し、通知を表示
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        Log.d(TAG, "VibeListenerService started as Foreground Service.")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    // アプリがサービスを停止させる際に実行
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "VibeListenerService destroyed.")
        stopForeground(true) // 通知を解除してサービスを停止させる
    }

    override fun onMessageReceived(message: MessageEvent) {
        Log.d(TAG, "--- VIBRATION CHECK START ---")
        val messageString = message.data.toString()
        Log.d(TAG, "Raw received message: $messageString")

        if (!messageString.isEmpty()) {
            Log.i(TAG, "Message received. Attempting to process.")

            try {
                // メッセージから遅延時間を解析
                val delayMs = parseDelayFromMessage(messageString)
                Log.d(TAG, "Parsed delayMs: $delayMs ms")

                // Coroutinesを使用して非同期で遅延とバイブレーションを実行
                serviceScope.launch {
                    try {
                        // 指定された時間だけ処理を一時停止
                        delay(delayMs)

                        // バイブレーションを実行
                        vibrateDevice()
                        Log.d(TAG, "Vibration sequence completed successfully.")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error executing delayed vibration sequence", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "FATAL ERROR in message handling logic: ${e.message}", e)
            }
        }
        Log.d(TAG, "--- VIBRATION CHECK END ---")
    }

    /**
     * メッセージ文字列から遅延時間を抽出します。
     */
    private fun parseDelayFromMessage(message: String): Long {
        var delayMs = DEFAULT_VIBRATION_DELAY

        // URLクエリ形式 (?delay=1000) から遅延時間を抽出するロジックを維持
        val queryIndex = message.indexOf("?")
        if (queryIndex != -1) {
            val queryString = message.substring(queryIndex + 1)
            val delayIndex = queryString.indexOf("delay=")
            if (delayIndex != -1) {
                val delayStr = queryString.substring(delayIndex + 6)
                delayMs = delayStr.toLongOrNull() ?: DEFAULT_VIBRATION_DELAY
                Log.d(TAG, "Delay successfully parsed from query string: $delayMs ms")
            }
        }
        Log.d(TAG, "Final delay set to: $delayMs ms")
        return delayMs
    }

    private fun vibrateDevice() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(200)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Vibration Service",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Service to handle delayed vibration messages"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Vibration Service Active")
            .setContentText("Waiting for delayed vibration messages.")
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setPriority(Notification.PRIORITY_LOW)
        return notificationBuilder.build()
    }
}