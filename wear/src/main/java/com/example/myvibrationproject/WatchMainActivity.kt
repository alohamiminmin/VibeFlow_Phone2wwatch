package com.example.myvibrationproject

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class WatchMainActivity : Activity() {

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(16, 16, 16, 16)
        }

        layout.addView(TextView(this).apply {
            text = "バイブテスト"
            textSize = 14f
            gravity = android.view.Gravity.CENTER
        })

        // ボタン1：シンプルな1秒バイブ
        layout.addView(Button(this).apply {
            text = "1秒バイブ"
            setOnClickListener {
                Log.d(TAG, "テストバイブ開始")
                vibrator.cancel()
                val effect = VibrationEffect.createOneShot(1000, 255)
                vibrator.vibrate(effect)
                Log.d(TAG, "テストバイブ実行完了: hasVibrator=${vibrator.hasVibrator()}")
            }
        })

        // ボタン2：カスタムパターン
        layout.addView(Button(this).apply {
            text = "パターン"
            setOnClickListener {
                vibrator.cancel()
                val effect = VibrationEffect.createWaveform(
                    longArrayOf(0, 500, 200, 500, 200, 500),
                    intArrayOf(0, 255, 0, 255, 0, 255),
                    -1
                )
                vibrator.vibrate(effect)
            }
        })

        setContentView(layout)

        // 起動時にhasVibratorをログ出力
        Log.d(TAG, "hasVibrator: ${vibrator.hasVibrator()}")
        Log.d(TAG, "hasAmplitudeControl: ${vibrator.hasAmplitudeControl()}")
    }
}