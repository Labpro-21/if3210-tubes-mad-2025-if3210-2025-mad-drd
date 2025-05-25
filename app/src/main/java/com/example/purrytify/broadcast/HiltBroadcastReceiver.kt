package com.example.purrytify.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.CallSuper

/**
 * Base BroadcastReceiver for Hilt injection
 */
abstract class HiltBroadcastReceiver : BroadcastReceiver() {
    @CallSuper
    override fun onReceive(context: Context, intent: Intent) {
        // Nothing to do here, Hilt will take care of injection
    }
}