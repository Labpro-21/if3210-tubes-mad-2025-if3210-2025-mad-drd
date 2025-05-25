package com.example.purrytify.service

import android.app.Notification
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.example.purrytify.domain.model.PlaylistItem
import com.example.purrytify.domain.player.PlayerBridge
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service that handles music playback in the background and displays
 * a notification with playback controls.
 */
@AndroidEntryPoint
class MusicService : Service() {
    
    companion object {
        private const val TAG = "MusicService"
    }
    
    @Inject
    lateinit var playerBridge: PlayerBridge
    
    // Service binder for activity connection
    private val binder = MusicBinder()
    
    // Media session for interacting with media controllers and notifications
    private lateinit var mediaSession: MediaSessionCompat
    
    // Notification manager
    private lateinit var notificationManager: MediaNotificationManager
    
    // Service scope for managing coroutines tied to service lifecycle
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // Current playback values
    private var currentItem: PlaylistItem? = null
    private var isPlaying: Boolean = false
    private var currentPosition: Long = 0
    private var duration: Long = 0
    
    // Broadcast receiver for notification actions
    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                MediaNotificationManager.ACTION_PLAY -> playerBridge.togglePlayPause()
                MediaNotificationManager.ACTION_PAUSE -> playerBridge.togglePlayPause()
                MediaNotificationManager.ACTION_NEXT -> playerBridge.next()
                MediaNotificationManager.ACTION_PREVIOUS -> playerBridge.previous()
                MediaNotificationManager.ACTION_STOP -> {
                    playerBridge.stop()
                    stopForeground(true)
                    stopSelf()
                }
            }
        }
    }
    
    /**
     * Media session callback to handle media button events
     */
    private val mediaSessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            playerBridge.togglePlayPause()
        }
        
        override fun onPause() {
            playerBridge.togglePlayPause()
        }
        
        override fun onSkipToNext() {
            playerBridge.next()
        }
        
        override fun onSkipToPrevious() {
            playerBridge.previous()
        }
        
        override fun onStop() {
            playerBridge.stop()
            stopForeground(true)
            stopSelf()
        }
        
        override fun onSeekTo(pos: Long) {
            playerBridge.seekTo(pos)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Music service created")
        
        // Initialize notification manager
        notificationManager = MediaNotificationManager(this)
        
        // Initialize media session
        initMediaSession()
        
        // Register receiver for notification actions
        registerNotificationReceiver()
        
        // Start observing player state
        observePlayerState()
    }
    
    /**
     * Initialize the media session
     */
    private fun initMediaSession() {
        mediaSession = MediaSessionCompat(this, "MusicService").apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            
            // Set the session's token so that client activities can communicate with it
            setCallback(mediaSessionCallback)
            
            // Set the initial PlaybackState
            setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setState(
                        PlaybackStateCompat.STATE_PAUSED,
                        0L,
                        1.0f
                    )
                    .setActions(
                        PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                    )
                    .build()
            )
            
            // Activate the session
            isActive = true
        }
    }
    
    /**
     * Register broadcast receiver for notification actions
     */
    private fun registerNotificationReceiver() {
        try {
            val intentFilter = IntentFilter().apply {
                addAction(MediaNotificationManager.ACTION_PLAY)
                addAction(MediaNotificationManager.ACTION_PAUSE)
                addAction(MediaNotificationManager.ACTION_NEXT)
                addAction(MediaNotificationManager.ACTION_PREVIOUS)
                addAction(MediaNotificationManager.ACTION_STOP)
            }
            
            registerReceiver(notificationReceiver, intentFilter)
            Log.d(TAG, "Successfully registered notification receiver")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register notification receiver: ${e.message}", e)
        }
    }
    
    /**
     * Observe player state changes to update notification
     */
    private fun observePlayerState() {
        serviceScope.launch {
            // Observe current item
            playerBridge.currentItem.collectLatest { item ->
                currentItem = item
                updateMediaSession()
                updateNotification()
            }
        }
        
        serviceScope.launch {
            // Observe playback state
            playerBridge.isPlaying.collectLatest { playing ->
                isPlaying = playing
                updateMediaSession()
                updateNotification()
            }
        }
        
        serviceScope.launch {
            // Observe current position
            playerBridge.currentPosition.collectLatest { position ->
                currentPosition = position
                updateNotification()
            }
        }
        
        serviceScope.launch {
            // Observe duration
            playerBridge.duration.collectLatest { dur ->
                duration = dur
                updateMediaSession()
                updateNotification()
            }
        }
    }
    
    /**
     * Update media session metadata and playback state
     */
    private fun updateMediaSession() {
        val item = currentItem ?: return
        
        // Update media metadata
        val metadataBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, item.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, item.artist)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
        
        // Update playback state
        val stateBuilder = PlaybackStateCompat.Builder()
            .setState(
                if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                currentPosition,
                1.0f
            )
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )
        
        mediaSession.setMetadata(metadataBuilder.build())
        mediaSession.setPlaybackState(stateBuilder.build())
    }
    
    /**
     * Update notification with current playback state
     */
    private fun updateNotification() {
        // If there's no current item, no need for notification
        if (currentItem == null) {
            stopForeground(true)
            return
        }
        
        // Update notification with current state
        notificationManager.updateNotification(
            mediaSession.sessionToken,
            isPlaying,
            currentItem,
            currentPosition,
            duration
        )
        
        // Make sure service is in foreground when playing
        if (isPlaying) {
            startForeground()
        } else {
            // When paused, we can either stay in foreground with the notification
            // or move to background. Here we stay in foreground for better UX.
            startForeground()
        }
    }
    
    /**
     * Start foreground service with notification
     */
    private fun startForeground() {
        val notification = notificationManager.getCurrentNotification() ?: return
        startForeground(1138, notification)
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle service start command
        return START_STICKY
    }
    
    override fun onBind(intent: Intent): IBinder {
        return binder
    }
    
    override fun onDestroy() {
        Log.d(TAG, "Music service destroyed")
        
        // Release resources
        mediaSession.release()
        try {
            unregisterReceiver(notificationReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
            Log.e(TAG, "Error unregistering receiver: ${e.message}")
        }
        
        // Cancel all coroutines
        serviceScope.cancel()
        
        super.onDestroy()
    }
    
    /**
     * Binder class for clients to access the service
     */
    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }
}