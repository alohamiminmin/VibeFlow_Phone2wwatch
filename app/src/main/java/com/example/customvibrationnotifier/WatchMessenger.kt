package com.example.customvibrationnotifier

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Wearable

object WatchMessenger {

    private const val PATH = "/vibration_command"

    fun sendToWatch(context: Context, message: String) {
        Thread {
            try {
                val nodeClient = Wearable.getNodeClient(context)
                val nodes = Tasks.await(nodeClient.connectedNodes)

                if (nodes.isEmpty()) {
                    Log.d("WatchMessenger", "時計が見つかりません")
                    return@Thread
                }

                for (node in nodes) {
                    Wearable.getMessageClient(context).sendMessage(
                        node.id,
                        PATH,
                        message.toByteArray()
                    )
                    Log.d("WatchMessenger", "送信成功: ${node.displayName}")
                }

            } catch (e: Exception) {
                Log.e("WatchMessenger", "送信エラー: ${e.message}")
            }
        }.start()
    }
}
