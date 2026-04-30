package com.example.customvibrationnotifier

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class WatchListenerService : WearableListenerService() {

    override fun onMessageReceived(event: MessageEvent) {
        super.onMessageReceived(event)

        if (event.path == "/vibration_command") {
            val message = String(event.data)
            Log.d("WatchReceiver", "message: $message")

            if (message == "vibrate_now") {
                vibrateStrong()
            }
        }
    }

    private fun vibrateStrong() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        val pattern = longArrayOf(
            0,    // すぐ開始
            1500  // 1.5秒振動
        )

        vibrator.vibrate(
            VibrationEffect.createWaveform(pattern, -1)
        )
    }
}
