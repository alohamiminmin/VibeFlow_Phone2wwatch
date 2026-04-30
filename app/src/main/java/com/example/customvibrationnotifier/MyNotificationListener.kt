package com.example.customvibrationnotifier

import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class MyNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        Log.d("NotifyTest", "通知を検知: ${sbn.packageName}")

        // すべての通知に対して 3秒後にカスタムバイブ指示を送る
        Handler(Looper.getMainLooper()).postDelayed({
            WatchMessenger.sendToWatch(this, "vibrate_now")
        }, 3000)
    }
}
