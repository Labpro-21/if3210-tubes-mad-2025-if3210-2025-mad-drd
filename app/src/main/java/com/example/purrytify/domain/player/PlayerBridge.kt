package com.example.purrytify.domain.player

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.purrytify.data.repository.PlayerRepository
import com.example.purrytify.domain.model.PlaylistItem
import com.example.purrytify.domain.model.Song
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
 * Enhanced PlayerBridge to handle playback from both local and online sources with queue management
 * Now connected to PlayerRepository for actual audio playback
 */
@Singleton
class PlayerBridge @Inject constructor(
    private val playerRepository: PlayerRepository,
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
    
    // Internal scope for state synchronization
    private val bridgeScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    init {
        // Set up callbacks from PlayerRepository
        playerRepository.onPlaybackEnded = {
            Log.d(TAG, "Playback ended, playing next song")
            next()
        }
        
        playerRepository.onPlaybackError = { error ->
            Log.e(TAG, "Playback error: $error")
            // Could emit error state here if needed
        }
        
        playerRepository.onPositionChanged = { position ->
            updateProgress(position)
        }
        
        // Start observing PlayerRepository state and sync with PlayerBridge state
        startStateSynchronization()
    }
    
    private fun startStateSynchronization() {
        // Sync playing state
        bridgeScope.launch {
            playerRepository.isPlaying.collect { isPlaying ->
                _isPlaying.value = isPlaying
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
        
        _currentItem.value = item
        
        // Use PlayerRepository for actual playback
        playerRepository.playItem(item)
        
        // Reset progress
        _progress.value = 0f
        _currentPosition.value = 0L
        
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
     * Play next song in queue
     */
    fun next() {
        val currentQueue = _queue.value
        val currentIdx = _currentIndex.value
        
        if (currentQueue.isNotEmpty()) {
            val nextIndex = (currentIdx + 1) % currentQueue.size
            _currentIndex.value = nextIndex
            Log.d(TAG, "Playing next song: index $nextIndex")
            playItem(currentQueue[nextIndex])
        }
    }
    
    /**
     * Play previous song in queue
     */
    fun previous() {
        val currentQueue = _queue.value
        val currentIdx = _currentIndex.value
        
        if (currentQueue.isNotEmpty()) {
            val prevIndex = if (currentIdx == 0) currentQueue.size - 1 else currentIdx - 1
            _currentIndex.value = prevIndex
            Log.d(TAG, "Playing previous song: index $prevIndex")
            playItem(currentQueue[prevIndex])
        }
    }
    
    /**
     * Toggle play/pause
     */
    fun togglePlayPause() {
        playerRepository.togglePlayPause()
    }
    
    /**
     * Seek to a specific position
     */
    fun seekTo(position: Long) {
        playerRepository.seekTo(position)
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
        playerRepository.stop()
        _currentItem.value = null
        _queue.value = emptyList()
        _currentIndex.value = 0
        _progress.value = 0f
        _currentPosition.value = 0L
        _duration.value = 0L
        _playbackContext.value = PlaybackContext.None
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