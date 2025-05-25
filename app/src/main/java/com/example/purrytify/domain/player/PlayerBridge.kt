package com.example.purrytify.domain.player

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.purrytify.data.repository.PlayerRepository
import com.example.purrytify.data.repository.SongRepository
import com.example.purrytify.domain.analytics.ListeningSessionTracker
import com.example.purrytify.domain.model.PlaylistItem
import com.example.purrytify.domain.model.Song
import com.example.purrytify.domain.util.Result
import com.example.purrytify.service.MusicService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enhanced PlayerBridge to handle playbook from both local and online sources with queue management
 */
@Singleton
class PlayerBridge @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val songRepository: SongRepository,
    private val listeningSessionTracker: ListeningSessionTracker,
    @ApplicationContext private val context: Context
) {
    
    private val TAG = "PlayerBridge"
    
    // Current queue
    private val _queue = MutableStateFlow<List<PlaylistItem>>(emptyList())
    val queue: StateFlow<List<PlaylistItem>> = _queue.asStateFlow()
    
    // Current index in queue
    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()
    
    // Currently playing item
    private val _currentItem = MutableStateFlow<PlaylistItem?>(null)
    val currentItem: StateFlow<PlaylistItem?> = _currentItem.asStateFlow()
    
    // Playing state (synced from PlayerRepository)
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    // Progress (0.0 to 1.0)
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()
    
    // Current position in milliseconds (synced from PlayerRepository)
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()
    
    // Duration in milliseconds (synced from PlayerRepository)
    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()
    
    // Playback context (for knowing the source)
    private val _playbackContext = MutableStateFlow<PlaybackContext>(PlaybackContext.None)
    val playbackContext: StateFlow<PlaybackContext> = _playbackContext.asStateFlow()
    
    // Current user ID for analytics
    private var currentUserId: Int? = null
    
    // Internal scope for state synchronization
    private val bridgeScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // Track last playing state for analytics
    private var lastIsPlaying = false
    
    // Track last position for seek detection
    private var lastPosition = 0L
    
    init {
        // Set up callbacks from PlayerRepository
        playerRepository.onPlaybackEnded = {
            Log.d(TAG, "Playback ended, notifying analytics and moving to next song")
            listeningSessionTracker.onSongCompleted()
            nextSongNaturally()
        }
        
        playerRepository.onPlaybackError = { error ->
            Log.e(TAG, "Playback error: $error")
            listeningSessionTracker.onPlaybackStopped()
        }
        
        playerRepository.onPositionChanged = { position ->
            handlePositionChanged(position)
        }
        
        // Start observing PlayerRepository state and sync with PlayerBridge state
        startStateSynchronization()
    }
    
    /**
     * Handle position changes and detect seeks
     */
    private fun handlePositionChanged(position: Long) {
        // Detect if this is a seek operation (large position jump)
        val positionDiff = kotlin.math.abs(position - lastPosition)
        val isLikelySeek = positionDiff > 2000 // More than 2 seconds difference
        
        if (isLikelySeek && lastPosition > 0) {
            Log.d(TAG, "Seek detected: from ${lastPosition}ms to ${position}ms")
            listeningSessionTracker.onSeek(lastPosition, position)
        }
        
        lastPosition = position
        updateProgress(position)
    }
    
    private fun startStateSynchronization() {
        // Sync playing state
        bridgeScope.launch {
            playerRepository.isPlaying.collect { isPlaying ->
                _isPlaying.value = isPlaying
                
                // Handle analytics tracking for play/pause
                if (lastIsPlaying != isPlaying) {
                    if (isPlaying) {
                        Log.d(TAG, "Playback resumed - resuming analytics session")
                        listeningSessionTracker.resumeSession()
                    } else {
                        Log.d(TAG, "Playback paused - pausing analytics session")
                        listeningSessionTracker.pauseSession()
                    }
                    lastIsPlaying = isPlaying
                }
            }
        }
        
        // Sync current position
        bridgeScope.launch {
            playerRepository.currentPosition.collect { position ->
                _currentPosition.value = position
                updateProgress(position)
            }
        }
        
        // Sync duration
        bridgeScope.launch {
            playerRepository.duration.collect { duration ->
                _duration.value = duration
                updateProgress(_currentPosition.value)
            }
        }
    }
    
    /**
     * Set current user ID for analytics tracking
     */
    fun setCurrentUserId(userId: Int) {
        currentUserId = userId
        Log.d(TAG, "Set current user ID for analytics: $userId")
    }
    
    /**
     * Play a queue of items starting from a specific index
     */
    fun playQueue(queue: List<PlaylistItem>, startIndex: Int = 0, context: PlaybackContext) {
        Log.d(TAG, "Playing queue with ${queue.size} items, starting at index $startIndex, context: $context")
        _queue.value = queue
        _currentIndex.value = startIndex
        _playbackContext.value = context
        
        if (queue.isNotEmpty() && startIndex < queue.size) {
            playItem(queue[startIndex])
            
            // Start music service for background playback
            startMusicService()
        }
    }
    
    /**
     * Start or prepare to play a playlist item
     */
    fun playItem(item: PlaylistItem) {
        Log.d(TAG, "Playing ${item.title} by ${item.artist}")
        
        // End previous session and start new one
        val userId = currentUserId
        if (userId != null) {
            Log.d(TAG, "Starting analytics session for: ${item.title}")
            listeningSessionTracker.startSession(item, userId)
            
            // Update recently played timestamp for local songs
            if (item is PlaylistItem.LocalSong) {
                bridgeScope.launch {
                    try {
                        Log.d(TAG, "Updating last played timestamp for local song: ${item.title}")
                        when (val result = songRepository.updateLastPlayed(item.id, userId)) {
                            is Result.Success -> {
                                Log.d(TAG, "Successfully updated last played timestamp for: ${item.title}")
                            }
                            is Result.Error -> {
                                Log.e(TAG, "Error updating last played timestamp: ${result.message}")
                            }
                            is Result.Loading -> {
                                // No-op
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception updating last played timestamp: ${e.message}", e)
                    }
                }
            }
        } else {
            Log.w(TAG, "No user ID available for analytics tracking and recently played update")
        }
        
        _currentItem.value = item
        
        // Use PlayerRepository for actual playback
        playerRepository.playItem(item)
        
        // Reset progress and position tracking
        _progress.value = 0f
        _currentPosition.value = 0L
        lastPosition = 0L
        
        // Start music service for background playback
        startMusicService()
    }
    
    /**
     * Start the music service
     */
    private fun startMusicService() {
        try {
            // Start the music service
            val serviceIntent = Intent(context, MusicService::class.java)
            context.startService(serviceIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting music service: ${e.message}", e)
        }
    }
    
    /**
     * Play a local song (creates a single-item queue)
     */
    fun playSong(song: Song) {
        Log.d(TAG, "Playing local song: ${song.title} by ${song.artist}")
        
        val playlistItem = PlaylistItem.fromLocalSong(song)
        playQueue(listOf(playlistItem), 0, PlaybackContext.Library)
    }
    
    /**
     * Play an online song (creates a single-item queue)
     */
    fun playOnlineSong(song: com.example.purrytify.domain.model.OnlineSong) {
        Log.d(TAG, "Playing online song: ${song.title} by ${song.artist}")
        
        val playlistItem = PlaylistItem.fromOnlineSong(song)
        playQueue(listOf(playlistItem), 0, PlaybackContext.TopSongs)
    }
    
    /**
     * Move to next song naturally (after current song completes)
     * This does NOT end the current session since it was already ended by onSongCompleted()
     */
    private fun nextSongNaturally() {
        Log.d(TAG, "Moving to next song naturally (no session management)")
        
        val currentQueue = _queue.value
        val currentIdx = _currentIndex.value
        
        if (currentQueue.isNotEmpty()) {
            val nextIndex = (currentIdx + 1) % currentQueue.size
            _currentIndex.value = nextIndex
            Log.d(TAG, "Playing next song naturally: index $nextIndex")
            
            // Play the next item (this will start a new session automatically)
            playItem(currentQueue[nextIndex])
        }
    }
    
    /**
     * Play next song (user-initiated)
     * This DOES end the current session first since it's a user skip
     */
    fun next() {
        Log.d(TAG, "Next button pressed - ending current session and switching to next song")
        
        // End current session before switching (this will record the listening time)
        listeningSessionTracker.onSongSkipped()
        
        val currentQueue = _queue.value
        val currentIdx = _currentIndex.value
        
        if (currentQueue.isNotEmpty()) {
            val nextIndex = (currentIdx + 1) % currentQueue.size
            _currentIndex.value = nextIndex
            Log.d(TAG, "Playing next song: index $nextIndex")
            
            // Play the next item (this will start a new session)
            playItem(currentQueue[nextIndex])
        }
    }
    
    /**
     * Play previous song (user-initiated)
     * This DOES end the current session first since it's a user skip
     */
    fun previous() {
        Log.d(TAG, "Previous button pressed - ending current session and switching to previous song")
        
        // End current session before switching (this will record the listening time)
        listeningSessionTracker.onSongSkipped()
        
        val currentQueue = _queue.value
        val currentIdx = _currentIndex.value
        
        if (currentQueue.isNotEmpty()) {
            val prevIndex = if (currentIdx == 0) currentQueue.size - 1 else currentIdx - 1
            _currentIndex.value = prevIndex
            Log.d(TAG, "Playing previous song: index $prevIndex")
            
            // Play the previous item (this will start a new session)
            playItem(currentQueue[prevIndex])
        }
    }
    
    /**
     * Toggle play/pause
     */
    fun togglePlayPause() {
        Log.d(TAG, "Play/pause toggled")
        playerRepository.togglePlayPause()
        // Analytics tracking will be handled automatically by state synchronization
    }
    
    /**
     * Seek to a specific position
     */
    fun seekTo(position: Long) {
        Log.d(TAG, "Seeking to position: ${position}ms")
        val oldPosition = _currentPosition.value
        playerRepository.seekTo(position)
        // The position change will be detected in handlePositionChanged()
        // which will call listeningSessionTracker.onSeek()
    }
    
    /**
     * Update playback progress (called by position tracking)
     */
    fun updateProgress(position: Long) {
        _currentPosition.value = position
        _progress.value = if (_duration.value > 0) position.toFloat() / _duration.value else 0f
    }
    
    /**
     * Update the current item without restarting playback (for like status changes, etc.)
     */
    fun updateCurrentItem(updatedItem: PlaylistItem) {
        if (_currentItem.value?.id == updatedItem.id) {
            Log.d(TAG, "Updating current item: ${updatedItem.title}")
            _currentItem.value = updatedItem
            
            // Also update the item in the queue
            val currentQueue = _queue.value.toMutableList()
            val currentIdx = _currentIndex.value
            if (currentIdx < currentQueue.size && currentQueue[currentIdx].id == updatedItem.id) {
                currentQueue[currentIdx] = updatedItem
                _queue.value = currentQueue
            }
        }
    }
    
    /**
     * Stop playback and clear queue
     */
    fun stop() {
        Log.d(TAG, "Stopping playback")
        
        // End analytics session
        listeningSessionTracker.onPlaybackStopped()
        
        playerRepository.stop()
        _currentItem.value = null
        _queue.value = emptyList()
        _currentIndex.value = 0
        _progress.value = 0f
        _currentPosition.value = 0L
        _duration.value = 0L
        _playbackContext.value = PlaybackContext.None
        lastIsPlaying = false
        lastPosition = 0L
    }
    
    /**
     * Stop playback specifically when app is closing from memory
     * This method is called when the app is being terminated
     */
    fun stopForAppClosure() {
        Log.d(TAG, "Stopping playback due to app closure")
        
        try {
            // End analytics session synchronously to ensure it's recorded before app termination
            listeningSessionTracker.endCurrentSessionSynchronously()
            
            // Stop player repository
            playerRepository.stop()
            
            // Clear all state
            _currentItem.value = null
            _queue.value = emptyList()
            _currentIndex.value = 0
            _progress.value = 0f
            _currentPosition.value = 0L
            _duration.value = 0L
            _playbackContext.value = PlaybackContext.None
            lastIsPlaying = false
            lastPosition = 0L
            
            // Stop music service
            try {
                val serviceIntent = Intent(context, MusicService::class.java)
                context.stopService(serviceIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping music service: ${e.message}", e)
            }
            
            Log.d(TAG, "Playback stopped successfully due to app closure")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping playback for app closure: ${e.message}", e)
        }
    }
    
    /**
     * Check if the specified item is currently playing
     */
    fun isItemPlaying(itemId: String): Boolean {
        return _currentItem.value?.id == itemId && _isPlaying.value
    }
    
    /**
     * Get the actual song ID to use for navigation
     */
    fun getNavigationId(item: PlaylistItem): String {
        return when (item) {
            is PlaylistItem.LocalSong -> item.id
            is PlaylistItem.OnlineSong -> item.originalId
        }
    }
    
    /**
     * Check if there are items in the queue
     */
    fun hasQueue(): Boolean {
        return _currentItem.value != null
    }
    
    /**
     * Get formatted current position
     */
    fun getFormattedCurrentPosition(): String {
        val totalSeconds = _currentPosition.value / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }
    
    /**
     * Get formatted duration
     */
    fun getFormattedDuration(): String {
        val totalSeconds = _duration.value / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }
    
    /**
     * Get current session debugging info from the listening session tracker
     */
    fun getSessionDebugInfo(): String {
        return listeningSessionTracker.getCurrentSessionInfo()
    }
}

/**
 * Playback context to track where the music is being played from
 */
sealed class PlaybackContext {
    data object None : PlaybackContext()
    data object Library : PlaybackContext()
    data object RecentlyPlayed : PlaybackContext()
    data object NewSongs : PlaybackContext()
    data object TopSongs : PlaybackContext()
    data object Recommendation : PlaybackContext()
}