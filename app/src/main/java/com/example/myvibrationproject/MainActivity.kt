package com.example.myvibrationproject

import android.content.Intent
import android.os.Bundle
import android.provider.Settings

class MainActivity : android.app.Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 通知アクセス権限がなければ設定画面へ誘導
        if (!isNotificationAccessGranted()) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    private fun isNotificationAccessGranted(): Boolean {
        val enabledListeners =
            Settings.Secure.getString(contentResolver,
                "enabled_notification_listeners") ?: return false
        return enabledListeners.contains(packageName)
    }
}