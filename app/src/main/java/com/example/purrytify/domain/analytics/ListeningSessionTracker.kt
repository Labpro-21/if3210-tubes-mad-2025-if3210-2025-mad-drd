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
    private var totalListeningTimeMs: Long = 0L // Accumulated listening time in MILLISECONDS
    private var isSessionActive: Boolean = false
    
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
        totalListeningTimeMs = 0L // Reset to 0 milliseconds
        isSessionActive = true
        
        Log.d(TAG, "Session started at: $currentSessionStartTime (timestamp)")
    }
    
    /**
     * Pause the current session (accumulate listening time but don't end)
     */
    fun pauseSession() {
        if (isSessionActive && currentSessionStartTime > 0) {
            val currentTime = System.currentTimeMillis()
            val segmentDurationMs = currentTime - currentSessionStartTime
            totalListeningTimeMs += segmentDurationMs
            
            Log.d(TAG, "Session paused. Segment duration: ${segmentDurationMs}ms, total accumulated: ${totalListeningTimeMs}ms")
            
            // Mark as paused
            isSessionActive = false
            currentSessionStartTime = 0L
        }
    }
    
    /**
     * Resume the current session
     */
    fun resumeSession() {
        if (currentSessionItem != null && !isSessionActive) {
            currentSessionStartTime = System.currentTimeMillis()
            isSessionActive = true
            Log.d(TAG, "Session resumed at: $currentSessionStartTime")
        }
    }
    
    /**
     * End the current session and record analytics
     */
    fun endCurrentSession() {
        val item = currentSessionItem ?: return
        val userId = currentUserId ?: return
        
        // Add any remaining listening time if session is active
        if (isSessionActive && currentSessionStartTime > 0) {
            val currentTime = System.currentTimeMillis()
            val segmentDurationMs = currentTime - currentSessionStartTime
            totalListeningTimeMs += segmentDurationMs
            
            Log.d(TAG, "Adding final segment duration: ${segmentDurationMs}ms")
        }
        
        // Record the session if there was actual listening time (minimum 1 second to avoid noise)
        if (totalListeningTimeMs >= 1000) { // At least 1 second = 1000 milliseconds
            Log.d(TAG, "Ending session for: ${item.title}")
            Log.d(TAG, "Total listening time: ${totalListeningTimeMs}ms (${totalListeningTimeMs/1000.0} seconds, ${totalListeningTimeMs/60000.0} minutes)")
            
            externalScope.launch {
                try {
                    analyticsRepository.recordListeningSession(
                        userId = userId,
                        songId = when (item) {
                            is PlaylistItem.LocalSong -> item.id
                            is PlaylistItem.OnlineSong -> item.originalId
                        },
                        songTitle = item.title,
                        artistName = item.artist,
                        listeningDurationMs = totalListeningTimeMs
                    )
                    Log.d(TAG, "Successfully recorded listening session with duration: ${totalListeningTimeMs}ms")
                } catch (e: Exception) {
                    Log.e(TAG, "Error recording listening session: ${e.message}", e)
                }
            }
        } else {
            Log.d(TAG, "Session too short (${totalListeningTimeMs}ms), not recording")
        }
        
        // Reset session data
        currentSessionItem = null
        currentUserId = null
        currentSessionStartTime = 0L
        totalListeningTimeMs = 0L
        isSessionActive = false
    }
    
    /**
     * Handle song completion (song finished playing)
     */
    fun onSongCompleted() {
        Log.d(TAG, "Song completed - ending session")
        endCurrentSession()
    }
    
    /**
     * Handle song skip/change (next/previous button)
     */
    fun onSongSkipped() {
        Log.d(TAG, "Song skipped - ending session")
        endCurrentSession()
    }
    
    /**
     * Handle playback stop
     */
    fun onPlaybackStopped() {
        Log.d(TAG, "Playback stopped - ending session")
        endCurrentSession()
    }
    
    /**
     * Handle seek operation (scrubbing through the song)
     * This should NOT end the session, just log it
     */
    fun onSeek(fromPosition: Long, toPosition: Long) {
        Log.d(TAG, "Seek operation: from ${fromPosition}ms to ${toPosition}ms")
        // Don't end session on seek - user is still listening to the same song
    }
    
    /**
     * Get current session info for debugging
     */
    fun getCurrentSessionInfo(): String {
        val item = currentSessionItem
        return if (item != null) {
            val currentSegmentTime = if (isSessionActive && currentSessionStartTime > 0) {
                System.currentTimeMillis() - currentSessionStartTime
            } else 0L
            
            "Current: ${item.title} - Total: ${totalListeningTimeMs}ms, Current segment: ${currentSegmentTime}ms, Active: $isSessionActive"
        } else {
            "No active session"
        }
    }
}