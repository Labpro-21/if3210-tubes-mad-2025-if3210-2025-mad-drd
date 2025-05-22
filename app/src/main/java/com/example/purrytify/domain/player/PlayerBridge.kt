package com.example.purrytify.domain.player

import android.util.Log
import com.example.purrytify.domain.model.PlaylistItem
import com.example.purrytify.domain.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enhanced PlayerBridge to handle playback from both local and online sources with queue management
 */
@Singleton
class PlayerBridge @Inject constructor() {
    
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
    
    // Playing state
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    // Progress (0.0 to 1.0)
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()
    
    // Current position in milliseconds
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()
    
    // Duration in milliseconds
    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()
    
    // Playback context (for knowing the source)
    private val _playbackContext = MutableStateFlow<PlaybackContext>(PlaybackContext.None)
    val playbackContext: StateFlow<PlaybackContext> = _playbackContext.asStateFlow()
    
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
        }
    }
    
    /**
     * Start or prepare to play a playlist item
     */
    fun playItem(item: PlaylistItem) {
        Log.d(TAG, "Playing ${item.title} by ${item.artist}")
        
        _currentItem.value = item
        _isPlaying.value = true
        _progress.value = 0f
        _currentPosition.value = 0L
        
        // Set duration based on item type
        when (item) {
            is PlaylistItem.LocalSong -> {
                _duration.value = item.durationMs
            }
            is PlaylistItem.OnlineSong -> {
                // Parse duration string (mm:ss) to milliseconds
                val parts = item.duration.split(":")
                val minutes = parts.getOrNull(0)?.toLongOrNull() ?: 0
                val seconds = parts.getOrNull(1)?.toLongOrNull() ?: 0
                _duration.value = (minutes * 60 + seconds) * 1000
            }
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
            playItem(currentQueue[prevIndex])
        }
    }
    
    /**
     * Toggle play/pause
     */
    fun togglePlayPause() {
        _isPlaying.value = !_isPlaying.value
    }
    
    /**
     * Seek to a specific position
     */
    fun seekTo(position: Long) {
        _currentPosition.value = position
        _progress.value = if (_duration.value > 0) position.toFloat() / _duration.value else 0f
    }
    
    /**
     * Update playback progress (called by media player)
     */
    fun updateProgress(position: Long) {
        _currentPosition.value = position
        _progress.value = if (_duration.value > 0) position.toFloat() / _duration.value else 0f
    }
    
    /**
     * Stop playback and clear queue
     */
    fun stop() {
        Log.d(TAG, "Stopping playback")
        _isPlaying.value = false
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