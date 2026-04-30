package com.example.myvibrationproject

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "Custom Vibration Notifier")
                    }
                }
            }
        }

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
