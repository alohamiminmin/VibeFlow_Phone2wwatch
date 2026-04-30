package com.example.myvibrationproject

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class VibeListenerService : WearableListenerService() {

    private val TAG = "VibeFlow"

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
            messageEvent.path.startsWith("/vibe/") -> {
                val patternName = messageEvent.path.removePrefix("/vibe/")
                Log.d(TAG, "バイブ実行: $patternName")

                // 標準バイブが終わるのを待ってから実行
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    triggerVibration(patternName)
                    Log.d(TAG, "バイブ完了: $patternName")
                }, 500L)  // 500ms待つ
            }

            messageEvent.path == "/vibe/stop" -> {
                vibrator.cancel()
            }
        }
    }

    private fun triggerVibration(patternName: String) {
        vibrator.cancel()

        val effect = when (patternName) {

            // longArrayOf(待機ms, 振動ms, 待機ms, 振動ms, ...)
            // intArrayOf(  0,    強度,    0,    強度, ...)  ※強度は0〜255

            "call" -> VibrationEffect.createWaveform(
                longArrayOf(0, 800, 400, 800, 400, 800),
                intArrayOf(0, 255, 0, 255, 0, 255),
                0   // ループ
            )

            "wechat" -> VibrationEffect.createWaveform(
                longArrayOf(0, 200, 150, 200),
                intArrayOf(0, 255, 0, 255),
                -1
            )

            "message" -> VibrationEffect.createWaveform(
                //        待機  振動  待機  振動  待機  振動
                longArrayOf(0, 300, 150, 300, 150, 600),
                intArrayOf(0, 255, 0, 255, 0, 255),
                -1
            )

            "alert" -> VibrationEffect.createWaveform(
                longArrayOf(0, 150, 100, 150, 100, 150, 100, 800),
                intArrayOf(0, 255, 0, 255, 0, 255, 0, 255),
                -1
            )

            else -> VibrationEffect.createOneShot(500, 255)
        }

        vibrator.vibrate(effect)
        Log.d(TAG, "バイブ完了: $patternName")
    }
}