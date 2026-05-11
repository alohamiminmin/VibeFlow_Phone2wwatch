package com.example.myvibrationproject

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

object VibeSender {

    private const val TAG = "VibeFlow"

    suspend fun send(context: Context, pkg: String, pattern: VibePattern) {
        try {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            Log.d(TAG, "接続中のWatch数: ${nodes.size}")
            if (nodes.isEmpty()) {
                Log.w(TAG, "Watchが見つかりません")
                return
            }

            // 遅延時間を取得
            val delayMs = AppVibeSettings.getDelay(context, pkg)

            val path = when (pattern) {
                VibePattern.NONE   -> return
                VibePattern.CUSTOM -> {
                    val p = AppVibeSettings.getCustomPattern(context, pkg) ?: "0,300,150,300,150,600"
                    val a = AppVibeSettings.getCustomAmplitude(context, pkg) ?: "0,255,0,255,0,255"
                    "/vibe/custom|$p|$a?delay=$delayMs"
                }
                else -> "/vibe/${pattern.name.lowercase()}?delay=$delayMs"
            }

            nodes.forEach { node ->
                Log.d(TAG, "送信先: ${node.displayName} path=$path")
                Wearable.getMessageClient(context)
                    .sendMessage(node.id, path, byteArrayOf())
                    .await()
                Log.d(TAG, "送信完了: $path")
            }
        } catch (e: Exception) {
            Log.e(TAG, "送信エラー: ${e.message}")
        }
    }

    suspend fun stop(context: Context) {
        try {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            nodes.forEach { node ->
                Wearable.getMessageClient(context)
                    .sendMessage(node.id, "/vibe/stop", byteArrayOf())
                    .await()
                Log.d(TAG, "停止送信完了: ${node.displayName}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "停止送信エラー: ${e.message}")
        }
    }
}