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
    
    // Service connection object
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "Service connected")
            musicService = (service as MusicService.MusicBinder).getService()
            _isConnected.value = true
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "Service disconnected")
            musicService = null
            _isConnected.value = false
        }
    }
    
    /**
     * Start and bind to the music service
     */
    fun connect() {
        Log.d(TAG, "Connecting to music service")
        try {
            val intent = Intent(context, MusicService::class.java)
            context.startService(intent)
            val bound = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            
            if (!bound) {
                Log.e(TAG, "Failed to bind to music service")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to music service: ${e.message}")
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
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting from music service: ${e.message}")
        }
    }
    
    /**
     * Get reference to music service (null if not connected)
     */
    fun getMusicService(): MusicService? {
        return musicService
    }
}