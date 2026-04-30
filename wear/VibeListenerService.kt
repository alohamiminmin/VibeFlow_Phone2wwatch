package myvibrationproject

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class VibeListenerService : WearableListenerService() {

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= 31) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager)
                .defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when {
            messageEvent.path.startsWith("/vibe/") -> {
                val patternName = messageEvent.path.removePrefix("/vibe/")
                triggerVibration(patternName)
            }
            messageEvent.path == "/vibe/stop" -> {
                vibrator.cancel()
            }
        }
    }

    private fun triggerVibration(patternName: String) {
        // 前のバイブが残っていたら止める
        vibrator.cancel()

        val effect = when (patternName) {

            // 電話：長め・繰り返し（受話するまでループ）
            "call" -> VibrationEffect.createWaveform(
                longArrayOf(0, 500, 300, 500, 300, 500, 1000),
                intArrayOf(0, 255, 0,   255, 0,   255, 0),
                0  // ループあり
            )

            // WeChat：短く2回
            "wechat" -> VibrationEffect.createWaveform(
                longArrayOf(0, 120, 100, 120),
                intArrayOf(0, 200, 0,   200),
                -1  // ループなし
            )

            // 一般メッセージ：中程度1回
            "message" -> VibrationEffect.createWaveform(
                longArrayOf(0, 80, 60, 250),
                intArrayOf(0, 160, 0, 210),
                -1
            )

            // 緊急・アラート：強く速く3回
            "alert" -> VibrationEffect.createWaveform(
                longArrayOf(0, 100, 80, 100, 80, 100),
                intArrayOf(0, 255, 0,  255, 0,  255),
                -1
            )

            else -> VibrationEffect.createOneShot(300, 180)
        }

        vibrator.vibrate(effect)
    }
}