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
            "call" -> VibrationEffect.createWaveform(
                longArrayOf(0, 500, 300, 500, 300, 500, 1000),
                intArrayOf(0, 255, 0, 255, 0, 255, 0),
                0
            )
            "wechat" -> VibrationEffect.createWaveform(
                longArrayOf(0, 120, 100, 120),
                intArrayOf(0, 200, 0, 200),
                -1
            )
            "message" -> VibrationEffect.createWaveform(
                longArrayOf(0, 80, 60, 250),
                intArrayOf(0, 160, 0, 210),
                -1
            )
            "alert" -> VibrationEffect.createWaveform(
                longArrayOf(0, 100, 80, 100, 80, 100),
                intArrayOf(0, 255, 0, 255, 0, 255),
                -1
            )
            else -> VibrationEffect.createOneShot(300, 180)
        }
        vibrator.vibrate(effect)
        Log.d(TAG, "バイブ完了: $patternName")
    }
}