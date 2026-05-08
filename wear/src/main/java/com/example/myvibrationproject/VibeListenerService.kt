package com.example.myvibrationproject

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class VibeListenerService : WearableListenerService() {

    private val TAG = "VibeFlow"
    private val handler = Handler(Looper.getMainLooper())

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= 31) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
                .defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(TAG, "Watch受信: path=${messageEvent.path}")
        when {
            messageEvent.path == "/vibe/stop" -> {
                handler.removeCallbacksAndMessages(null)
                vibrator.cancel()
            }
            messageEvent.path.startsWith("/vibe/") -> {
                val payload = messageEvent.path.removePrefix("/vibe/")
                // 標準バイブが終わる800ms後にカスタムバイブ実行
                handler.postDelayed({
                    triggerVibration(payload)
                }, 800L)
            }
        }
    }

    private fun triggerVibration(payload: String) {
        vibrator.cancel()

        val effect = when {
            payload.startsWith("custom|") -> buildCustomEffect(payload)
            payload == "none"   -> return
            payload == "short"  -> VibrationEffect.createOneShot(150, 255)
            payload == "double" -> VibrationEffect.createWaveform(
                longArrayOf(0, 150, 120, 150),
                intArrayOf(0, 255, 0, 255), -1)
            payload == "long"   -> VibrationEffect.createOneShot(800, 255)
            payload == "strong" -> VibrationEffect.createWaveform(
                longArrayOf(0, 200, 100, 200, 100, 200),
                intArrayOf(0, 255, 0, 255, 0, 255), -1)
            payload == "call"   -> VibrationEffect.createWaveform(
                longArrayOf(0, 800, 400, 800, 400, 800),
                intArrayOf(0, 255, 0, 255, 0, 255), 0)

            payload == "wechat" -> VibrationEffect.createWaveform(
                longArrayOf(0, 120, 100, 120),
                intArrayOf(0, 200, 0, 200), -1)

            else -> VibrationEffect.createOneShot(300, 255)
        }

        vibrator.vibrate(effect)
        Log.d(TAG, "バイブ完了: $payload")
    }

    private fun buildCustomEffect(payload: String): VibrationEffect {
        return try {
            val parts = payload.split("|")
            val timings = parts[1].split(",").map { it.trim().toLong() }.toLongArray()
            val amplitudes = parts[2].split(",").map { it.trim().toInt() }.toIntArray()

            Log.d(TAG, "カスタム: timings=${timings.toList()} amp=${amplitudes.toList()}")

            if (timings.size != amplitudes.size) {
                Log.e(TAG, "パターンと強度の数が不一致")
                return VibrationEffect.createOneShot(300, 255)
            }

            VibrationEffect.createWaveform(timings, amplitudes, -1)
        } catch (e: Exception) {
            Log.e(TAG, "カスタムパターン解析エラー: ${e.message}")
            VibrationEffect.createOneShot(300, 255)
        }
    }
}