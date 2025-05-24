package com.example.purrytify.domain.analytics

import android.util.Log
import com.example.purrytify.data.repository.AnalyticsRepository
import com.example.purrytify.domain.model.PlaylistItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks listening sessions for analytics purposes
 * Records actual listening time when songs are finished, skipped, or paused
 */
@Singleton
class ListeningSessionTracker @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val externalScope: CoroutineScope
) {
    private val TAG = "ListeningSessionTracker"
    
    // Current session data
    private var currentSessionStartTime: Long = 0L
    private var currentSessionItem: PlaylistItem? = null
    private var currentUserId: Int? = null
    private var totalListeningTime: Long = 0L // Accumulated listening time for current song
    
    /**
     * Start tracking a new listening session
     */
    fun startSession(item: PlaylistItem, userId: Int) {
        Log.d(TAG, "Starting session for: ${item.title} by ${item.artist}")
        
        // If there was a previous session, end it first
        endCurrentSession()
        
        currentSessionItem = item
        currentUserId = userId
        currentSessionStartTime = System.currentTimeMillis()
        totalListeningTime = 0L
    }
    
    /**
     * Pause the current session (accumulate listening time but don't end)
     */
    fun pauseSession() {
        if (currentSessionStartTime > 0) {
            val sessionDuration = System.currentTimeMillis() - currentSessionStartTime
            totalListeningTime += sessionDuration
            currentSessionStartTime = 0L // Mark as paused
            
            Log.d(TAG, "Session paused, accumulated time: ${totalListeningTime}ms")
        }
    }
    
    /**
     * Resume the current session
     */
    fun resumeSession() {
        if (currentSessionItem != null && currentSessionStartTime == 0L) {
            currentSessionStartTime = System.currentTimeMillis()
            Log.d(TAG, "Session resumed")
        }
    }
    
    /**
     * End the current session and record analytics
     */
    fun endCurrentSession() {
        val item = currentSessionItem ?: return
        val userId = currentUserId ?: return
        
        // Add any remaining listening time
        if (currentSessionStartTime > 0) {
            val sessionDuration = System.currentTimeMillis() - currentSessionStartTime
            totalListeningTime += sessionDuration
        }
        
        // Record the session if there was actual listening time
        if (totalListeningTime > 0) {
            Log.d(TAG, "Ending session for: ${item.title}, total time: ${totalListeningTime}ms")
            
            externalScope.launch {
                analyticsRepository.recordListeningSession(
                    userId = userId,
                    songId = when (item) {
                        is PlaylistItem.LocalSong -> item.id
                        is PlaylistItem.OnlineSong -> item.originalId
                    },
                    songTitle = item.title,
                    artistName = item.artist,
                    listeningDurationMs = totalListeningTime
                )
            }
        }
        
        // Reset session data
        currentSessionItem = null
        currentUserId = null
        currentSessionStartTime = 0L
        totalListeningTime = 0L
    }
    
    /**
     * Handle song completion (song finished playing)
     */
    fun onSongCompleted() {
        Log.d(TAG, "Song completed")
        endCurrentSession()
    }
    
    /**
     * Handle song skip/change
     */
    fun onSongSkipped() {
        Log.d(TAG, "Song skipped")
        endCurrentSession()
    }
    
    /**
     * Handle playback stop
     */
    fun onPlaybackStopped() {
        Log.d(TAG, "Playback stopped")
        endCurrentSession()
    }
    
    /**
     * Get current session info for debugging
     */
    fun getCurrentSessionInfo(): String {
        val item = currentSessionItem
        return if (item != null) {
            val currentTime = if (currentSessionStartTime > 0) {
                System.currentTimeMillis() - currentSessionStartTime
            } else 0L
            
            "Current: ${item.title} - Total: ${totalListeningTime}ms, Current: ${currentTime}ms"
        } else {
            "No active session"
        }
    }
}