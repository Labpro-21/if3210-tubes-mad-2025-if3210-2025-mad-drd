package com.example.purrytify.broadcast

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.purrytify.domain.player.PlayerBridge
import com.example.purrytify.service.MediaNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * BroadcastReceiver that handles media notification actions.
 */
@AndroidEntryPoint
class MediaNotificationReceiver : HiltBroadcastReceiver() {
    
    @Inject
    lateinit var playerBridge: PlayerBridge
    
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        Log.d(TAG, "Received action: ${intent.action}")
        
        when (intent.action) {
            MediaNotificationManager.ACTION_PLAY -> {
                playerBridge.togglePlayPause()
            }
            MediaNotificationManager.ACTION_PAUSE -> {
                playerBridge.togglePlayPause()
            }
            MediaNotificationManager.ACTION_NEXT -> {
                playerBridge.next()
            }
            MediaNotificationManager.ACTION_PREVIOUS -> {
                playerBridge.previous()
            }
            MediaNotificationManager.ACTION_STOP -> {
                playerBridge.stop()
            }
        }
    }
    
    companion object {
        private const val TAG = "MediaNotificationReceiver"
    }
}