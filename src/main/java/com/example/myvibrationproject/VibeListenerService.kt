package com.example.myvibrationproject

import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A service that listens for incoming messages and triggers a delayed vibration.
 * Implements modern Android practices using Coroutines and ensuring lifecycle safety.
 */
class VibeListenerService : android.app.Service() {

    private val TAG = "VibeListenerService"
    
    // Default delay in milliseconds
    private val DEFAULT_VIBRATION_DELAY = 1000L
    
    // Scope tied to the service lifecycle to ensure coroutines are cancelled when the service is destroyed
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "VibeListenerService created.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "VibeListenerService started with intent: ${intent?.action}")
        // Handle service start logic if necessary
        return android.app.Service.START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Handle service binding logic if necessary
        return null
    }

    /**
     * Handles incoming messages and triggers a delayed vibration if the message path matches.
     */
    fun onMessageReceived(message: String) {
        // 3. Check if the message path is the target trigger
        if (message.startsWith("/vibe/trigger_vibration")) {
            
            // 4. Parse delay from message (e.g., "/vibe/trigger_vibration delay:2500")
            val delayMs = parseDelayFromMessage(message)
            
            Log.i(TAG, "Received vibration trigger. Delay set to: ${delayMs}ms")

            // 1. Use Kotlin Coroutines to handle timing and execution off the main thread
            serviceScope.launch {
                try {
                    // 2. Use coroutines' delay() for non-blocking suspension
                    delay(delayMs)

                    // 3. Execute the vibration service call
                    vibrateDevice()
                    Log.d(TAG, "Vibration sequence completed.")
                } catch (e: Exception) {
                    // Log any exceptions during the delay or vibration process
                    Log.e(TAG, "Error executing delayed vibration sequence", e)
                }
            }
        } else {
            Log.d(TAG, "Ignoring message: $message. Not a vibration trigger.")
        }
    }

    /**
     * Parses the delay from the message string. 
     * Expected format: "/vibe/trigger_vibration [any data] delay:1000 [rest of message]"
     */
    private fun parseDelayFromMessage(message: String): Long {
        return try {
            val parts = message.split(" ")
            for (part in parts) {
                if (part.startsWith("delay:")) {
                    // Extract the number following "delay:"
                    return part.substring(6).toLongOrNull() ?: DEFAULT_VIBRATION_DELAY
                }
            }
            DEFAULT_VIBRATION_DELAY
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing delay from message: $message", e)
            DEFAULT_VIBRATION_DELAY
        }
    }

    /**
     * Triggers the physical vibration using the system Vibrator service.
     * Handles API version compatibility for vibration implementation.
     */
    private fun vibrateDevice() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        
        // Using the system service to vibrate
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // For Android O and above, we can use VibrationEffect for more granular control
            // Using a one-shot vibration for simplicity, assuming the user wants a single pulse after delay.
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            // For older API levels
            vibrator.vibrate(200)
        }
    }
}