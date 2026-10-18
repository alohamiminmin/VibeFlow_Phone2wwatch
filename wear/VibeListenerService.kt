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
                // /vibe/ を除去してパスを取得
                val fullPath = messageEvent.path.removePrefix("/vibe/")
                // ?delay=XXX のようなクエリパラメータを除去
                val patternName = fullPath.split("?")[0]
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

        val effect = when {
            // カスタムパターン: custom|timings|amplitudes
            patternName.startsWith("custom|") -> {
                val parts = patternName.removePrefix("custom|").split("|")
                if (parts.size == 2) {
                    val timings = parts[0].split(",").mapNotNull { it.trim().toLongOrNull() }.toLongArray()
                    val amplitudes = parts[1].split(",").mapNotNull { it.trim().toIntOrNull() }.toIntArray()
                    // timings と amplitudes の長さを揃える（短い方に合わせる）
                    val minSize = minOf(timings.size, amplitudes.size)
                    if (minSize > 0) {
                        val trimmedTimings = timings.copyOfRange(0, minSize)
                        val trimmedAmplitudes = amplitudes.copyOfRange(0, minSize)
                        VibrationEffect.createWaveform(trimmedTimings, trimmedAmplitudes, -1)
                    } else {
                        VibrationEffect.createOneShot(300, 180)
                    }
                } else {
                    VibrationEffect.createOneShot(300, 180)
                }
            }

            // 電話：長め・繰り返し（受信するまでループ）
            patternName == "call" -> VibrationEffect.createWaveform(
                longArrayOf(0, 500, 300, 500, 300, 500, 1000),
                intArrayOf(0, 255, 0,   255, 0,   255, 0),
                0  // ループあり
            )

            // WeChat：短く2回
            patternName == "wechat" -> VibrationEffect.createWaveform(
                longArrayOf(0, 120, 100, 120),
                intArrayOf(0, 200, 0,   200),
                -1  // ループなし
            )

            // 一般メッセージ：中程度1回
            patternName == "message" -> VibrationEffect.createWaveform(
                longArrayOf(0, 80, 60, 250),
                intArrayOf(0, 160, 0, 210),
                -1
            )

            // 緊急・アラート：強く速く3回
            patternName == "alert" -> VibrationEffect.createWaveform(
                longArrayOf(0, 100, 80, 100, 80, 100),
                intArrayOf(0, 255, 0,  255, 0,  255),
                -1
            )

            // SHORT
            patternName == "short" -> VibrationEffect.createOneShot(80, 160)

            // DOUBLE
            patternName == "double" -> VibrationEffect.createWaveform(
                longArrayOf(0, 80, 60, 80),
                intArrayOf(0, 200, 0, 200),
                -1
            )

            // LONG
            patternName == "long" -> VibrationEffect.createOneShot(500, 200)

            // STRONG
            patternName == "strong" -> VibrationEffect.createWaveform(
                longArrayOf(0, 100, 80, 100, 80, 100),
                intArrayOf(0, 255, 0, 255, 0, 255),
                -1
            )

            else -> VibrationEffect.createOneShot(300, 180)
        }

        vibrator.vibrate(effect)
    }
}
