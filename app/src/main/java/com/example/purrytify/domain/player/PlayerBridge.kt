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
 * Bridge class to handle playback from both local and online sources
 * This will allow us to play mixed playlists with both local and online songs
 */
@Singleton
class PlayerBridge @Inject constructor() {
    
    private val TAG = "PlayerBridge"
    
    // Currently playing item
    private val _currentItem = MutableStateFlow<PlayerItem?>(null)
    val currentItem = _currentItem.asStateFlow()
    
    // Playing state
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()
    
    // Progress (0.0 to 1.0)
    private val _progress = MutableStateFlow(0f)
    val progress = _progress.asStateFlow()
    
    /**
     * Start or prepare to play a playlist item
     */
    fun playItem(item: PlaylistItem) {
        Log.d(TAG, "Playing ${item.title} by ${item.artist}")
        
        // Convert to PlayerItem
        val playerItem = when (item) {
            is PlaylistItem.LocalSong -> {
                PlayerItem.LocalSong(
                    id = item.id,
                    title = item.title,
                    artist = item.artist,
                    artworkPath = item.artworkPath,
                    filePath = item.filePath,
                    duration = item.durationMs,
                    isLiked = item.isLiked
                )
            }
            is PlaylistItem.OnlineSong -> {
                PlayerItem.OnlineSong(
                    id = item.id,
                    title = item.title,
                    artist = item.artist, 
                    artworkUrl = item.artworkUrl,
                    songUrl = item.songUrl,
                    duration = item.duration,
                    originalId = item.originalId
                )
            }
        }
        
        _currentItem.value = playerItem
        _isPlaying.value = true
    }
    
    /**
     * Play a local song
     */
    fun playSong(song: Song) {
        Log.d(TAG, "Playing local song: ${song.title} by ${song.artist}")
        
        val playerItem = PlayerItem.LocalSong(
            id = song.id,
            title = song.title,
            artist = song.artist,
            artworkPath = song.artworkPath,
            filePath = song.filePath,
            duration = song.duration,
            isLiked = song.isLiked
        )
        
        _currentItem.value = playerItem
        _isPlaying.value = true
    }
    
    /**
     * Play an online song
     */
    fun playOnlineSong(song: com.example.purrytify.domain.model.OnlineSong) {
        Log.d(TAG, "Playing online song: ${song.title} by ${song.artist}")
        
        val playerItem = PlayerItem.OnlineSong(
            id = "online_${song.id}",
            title = song.title,
            artist = song.artist,
            artworkUrl = song.artworkUrl,
            songUrl = song.songUrl,
            duration = song.duration,
            originalId = song.id.toString()
        )
        
        _currentItem.value = playerItem
        _isPlaying.value = true
    }
    
    /**
     * Check if the specified item is currently playing
     */
    fun isItemPlaying(itemId: String): Boolean {
        return _currentItem.value?.id == itemId && _isPlaying.value
    }
    
    /**
     * Update playback progress
     */
    fun updateProgress(progress: Float) {
        _progress.value = progress
    }
    
    /**
     * Toggle play/pause
     */
    fun togglePlayPause() {
        _isPlaying.value = !_isPlaying.value
    }
    
    /**
     * Get the actual song ID to use for navigation
     * This is needed because online songs have a prefix in the ID
     */
    fun getNavigationId(item: PlaylistItem): String {
        return when (item) {
            is PlaylistItem.LocalSong -> item.id
            is PlaylistItem.OnlineSong -> item.originalId
        }
    }
}

/**
 * Sealed class representing an item in the player
 */
sealed class PlayerItem {
    abstract val id: String
    abstract val title: String
    abstract val artist: String
    
    /**
     * Local song
     */
    data class LocalSong(
        override val id: String,
        override val title: String,
        override val artist: String,
        val artworkPath: String,
        val filePath: String,
        val duration: Long,
        val isLiked: Boolean
    ) : PlayerItem()
    
    /**
     * Online song
     */
    data class OnlineSong(
        override val id: String,
        override val title: String,
        override val artist: String,
        val artworkUrl: String,
        val songUrl: String,
        val duration: String,
        val originalId: String
    ) : PlayerItem()
}