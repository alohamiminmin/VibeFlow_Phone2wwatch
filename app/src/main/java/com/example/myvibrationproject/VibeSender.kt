package com.example.myvibrationproject

import android.content.Context
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

object VibeSender {

    suspend fun send(context: Context, patternName: String) {
        try {
            val nodes = Wearable.getNodeClient(context)
                .connectedNodes.await()

            if (nodes.isEmpty()) return  // Watchが未接続なら何もしない

            nodes.forEach { node ->
                Wearable.getMessageClient(context)
                    .sendMessage(node.id, "/vibe/$patternName", byteArrayOf())
                    .await()
            }
        } catch (e: Exception) {
            // 接続エラーは無視（Watchが範囲外など）
        }
    }

    suspend fun stop(context: Context) {
        send(context, "stop")
    }
}