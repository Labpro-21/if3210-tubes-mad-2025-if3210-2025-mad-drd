package com.example.purrytify.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.purrytify.MainActivity
import com.example.purrytify.R
import com.example.purrytify.domain.model.PlaylistItem
import java.io.File
import java.util.concurrent.Executors

/**
 * Manager for media playback notifications that handles creating the notification and updating it
 * based on the current playback state.
 */
class MediaNotificationManager(private val context: Context) {

    companion object {
        private const val NOTIFICATION_ID = 1138
        private const val CHANNEL_ID = "com.example.purrytify.PLAYBACK_CHANNEL"
        private const val REQUEST_CODE = 100

        // Action constants for notification control
        const val ACTION_PLAY = "com.example.purrytify.PLAY"
        const val ACTION_PAUSE = "com.example.purrytify.PAUSE"
        const val ACTION_NEXT = "com.example.purrytify.NEXT"
        const val ACTION_PREVIOUS = "com.example.purrytify.PREVIOUS"
        const val ACTION_STOP = "com.example.purrytify.STOP"
    }

    private val notificationManager: NotificationManagerCompat = NotificationManagerCompat.from(context)
    private val executor = Executors.newSingleThreadExecutor()
    private var currentNotification: Notification? = null

    init {
        // Create the notification channel for Android O and above
        createNotificationChannel()
    }

    /**
     * Create notification channel for Android O and above
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Playback Controls",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Playback controls and current song information"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Get an Intent for notification actions
     */
    private fun getActionIntent(action: String): PendingIntent {
        val intent = Intent(action).apply {
            setPackage(context.packageName)
        }
        
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Get a PendingIntent to launch the activity when the notification is tapped
     */
    private fun getContentIntent(currentItem: PlaylistItem?): PendingIntent {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            
            // Add extras to open the player screen for this song
            currentItem?.let { item ->
                putExtra("OPEN_PLAYER", true)
                putExtra("SONG_ID", when (item) {
                    is PlaylistItem.LocalSong -> item.id
                    is PlaylistItem.OnlineSong -> item.originalId
                })
            }
        }
        
        return PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Get artwork bitmap for the notification
     */
    private fun getArtworkBitmap(currentItem: PlaylistItem, callback: (Bitmap?) -> Unit) {
        // Use a default bitmap if no artwork is available
        val defaultBitmap = BitmapFactory.decodeResource(
            context.resources,
            R.drawable.default_artwork
        )
        
        when (currentItem) {
            is PlaylistItem.LocalSong -> {
                if (currentItem.artworkPath.isNotEmpty() && File(currentItem.artworkPath).exists()) {
                    // Load local artwork
                    Glide.with(context)
                        .asBitmap()
                        .load(File(currentItem.artworkPath))
                        .into(object : CustomTarget<Bitmap>() {
                            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                callback(resource)
                            }
                            
                            override fun onLoadFailed(errorDrawable: Drawable?) {
                                callback(defaultBitmap)
                            }
                            
                            override fun onLoadCleared(placeholder: Drawable?) {
                                // Not used
                            }
                        })
                } else {
                    callback(defaultBitmap)
                }
            }
            is PlaylistItem.OnlineSong -> {
                // Load remote artwork
                Glide.with(context)
                    .asBitmap()
                    .load(currentItem.artworkUrl)
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            callback(resource)
                        }
                        
                        override fun onLoadFailed(errorDrawable: Drawable?) {
                            callback(defaultBitmap)
                        }
                        
                        override fun onLoadCleared(placeholder: Drawable?) {
                            // Not used
                        }
                    })
            }
        }
    }

    /**
     * Build the media style notification
     */
    private fun buildNotification(
        sessionToken: MediaSessionCompat.Token,
        isPlaying: Boolean,
        currentItem: PlaylistItem?,
        currentPosition: Long,
        duration: Long
    ): Notification {
        if (currentItem == null) {
            // Return a minimal notification if no song is playing
            return NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Purrytify")
                .setContentText("No song playing")
                .setDeleteIntent(getActionIntent(ACTION_STOP))
                .build()
        }
        
        // Start building notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID).apply {
            // Set the notification style to MediaStyle
            setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(sessionToken)
                    .setShowActionsInCompactView(0, 1, 2) // Show previous, play/pause, next in compact view
            )
            
            // Set basic notification properties
            setSmallIcon(R.drawable.ic_music_note)
            setContentTitle(currentItem.title)
            setContentText(currentItem.artist)
            setContentIntent(getContentIntent(currentItem))
            setDeleteIntent(getActionIntent(ACTION_STOP))
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            setShowWhen(false)
            
            // Add action buttons
            addAction(
                R.drawable.ic_skip_previous,
                "Previous",
                getActionIntent(ACTION_PREVIOUS)
            )
            
            // Play or pause button based on current state
            if (isPlaying) {
                addAction(
                    R.drawable.ic_pause,
                    "Pause",
                    getActionIntent(ACTION_PAUSE)
                )
            } else {
                addAction(
                    R.drawable.ic_play,
                    "Play",
                    getActionIntent(ACTION_PLAY)
                )
            }
            
            addAction(
                R.drawable.ic_skip_next,
                "Next",
                getActionIntent(ACTION_NEXT)
            )
            
            // Add progress information if available
            if (duration > 0) {
                val formattedDuration = formatDuration(duration)
                val formattedPosition = formatDuration(currentPosition)
                setSubText("$formattedPosition / $formattedDuration")
            }
        }
        
        return builder.build()
    }

    /**
     * Format duration to mm:ss format
     */
    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    /**
     * Update the notification with current playback state
     */
    fun updateNotification(
        sessionToken: MediaSessionCompat.Token,
        isPlaying: Boolean,
        currentItem: PlaylistItem?,
        currentPosition: Long,
        duration: Long
    ) {
        if (currentItem == null) {
            notificationManager.cancel(NOTIFICATION_ID)
            return
        }
        
        executor.execute {
            // Build notification with placeholder artwork first
            var notification = buildNotification(
                sessionToken, 
                isPlaying, 
                currentItem,
                currentPosition,
                duration
            )
            
            // Post notification with placeholder artwork
            updateNotificationNow(notification)
            
            // Then load actual artwork asynchronously
            getArtworkBitmap(currentItem) { bitmap ->
                if (bitmap != null) {
                    // Rebuild notification with actual artwork
                    val updatedBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                        .setLargeIcon(bitmap)
                        .setStyle(
                            androidx.media.app.NotificationCompat.MediaStyle()
                                .setMediaSession(sessionToken)
                                .setShowActionsInCompactView(0, 1, 2)
                        )
                    
                    // Update notification with artwork
                    updateNotificationNow(notification)
                }
            }
        }
    }

    /**
     * Update the notification immediately
     */
    private fun updateNotificationNow(notification: Notification) {
        currentNotification = notification
        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            // Log error but don't crash - notification permissions might be denied
            e.printStackTrace()
        }
    }

    /**
     * Get the current notification for service foreground state
     */
    fun getCurrentNotification(): Notification? {
        return currentNotification
    }

    /**
     * Cancel notification when playback stops
     */
    fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
        currentNotification = null
    }
}