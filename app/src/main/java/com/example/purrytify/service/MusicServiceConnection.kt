package com.example.purrytify.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages connection to the MusicService and provides a safe way for activities/fragments
 * to interact with the service.
 */
@Singleton
class MusicServiceConnection @Inject constructor(
    private val context: Context
) {
    private val TAG = "MusicServiceConnection"
    
    // Service connection state
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    // Reference to the bound service
    private var musicService: MusicService? = null
    
    // Track if we're currently trying to connect
    private var isConnecting = false
    
    // Service connection object
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "Service connected")
            musicService = (service as MusicService.MusicBinder).getService()
            _isConnected.value = true
            isConnecting = false
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "Service disconnected")
            musicService = null
            _isConnected.value = false
            isConnecting = false
        }
    }
    
    /**
     * Start and bind to the music service
     */
    fun connect() {
        if (_isConnected.value || isConnecting) {
            Log.d(TAG, "Already connected or connecting to service")
            return
        }
        
        Log.d(TAG, "Connecting to music service")
        isConnecting = true
        
        try {
            val intent = Intent(context, MusicService::class.java)
            context.startService(intent)
            val bound = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            
            if (!bound) {
                Log.e(TAG, "Failed to bind to music service")
                isConnecting = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to music service: ${e.message}")
            isConnecting = false
        }
    }
    
    /**
     * Unbind from the music service
     */
    fun disconnect() {
        Log.d(TAG, "Disconnecting from music service")
        try {
            if (_isConnected.value) {
                context.unbindService(serviceConnection)
                _isConnected.value = false
                musicService = null
            }
            isConnecting = false
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting from music service: ${e.message}")
        }
    }
    
    /**
     * Stop the service and disconnect forcefully
     * This method is called when the app is closing from memory
     */
    fun stopAndDisconnect() {
        Log.d(TAG, "Stopping and disconnecting music service due to app closure")
        
        try {
            // First, tell the service to stop foreground and clean up
            musicService?.let { service ->
                try {
                    // Stop foreground service
                    service.stopForeground(true)
                    
                    // Stop the service itself
                    service.stopSelf()
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping service through binder: ${e.message}")
                }
            }
            
            // Unbind from service
            if (_isConnected.value) {
                try {
                    context.unbindService(serviceConnection)
                } catch (e: Exception) {
                    Log.e(TAG, "Error unbinding service: ${e.message}")
                }
            }
            
            // Stop service using context
            try {
                val serviceIntent = Intent(context, MusicService::class.java)
                context.stopService(serviceIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping service with context: ${e.message}")
            }
            
            // Clear state
            _isConnected.value = false
            musicService = null
            isConnecting = false
            
            Log.d(TAG, "Music service stopped and disconnected successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error in stopAndDisconnect: ${e.message}", e)
            
            // Force clear state even if there were errors
            _isConnected.value = false
            musicService = null
            isConnecting = false
        }
    }
    
    /**
     * Get reference to music service (null if not connected)
     */
    fun getMusicService(): MusicService? {
        return musicService
    }
    
    /**
     * Check if the service is currently connected
     */
    fun isServiceConnected(): Boolean {
        return _isConnected.value && musicService != null
    }
}