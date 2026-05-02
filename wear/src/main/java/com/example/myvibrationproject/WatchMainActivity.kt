package com.example.myvibrationproject

import android.app.Activity
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class WatchMainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(0xFF000000.toInt())
        }

        // ノックアイコン
        layout.addView(TextView(this).apply {
            text = "🤜"
            textSize = 36f
            gravity = Gravity.CENTER
        })

        layout.addView(TextView(this).apply {
            text = "VibeFlow"
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 4)
        })

        layout.addView(TextView(this).apply {
            text = "待機中..."
            textSize = 12f
            setTextColor(0xFF888888.toInt())
            gravity = Gravity.CENTER
        })

        setContentView(layout)
    }
}