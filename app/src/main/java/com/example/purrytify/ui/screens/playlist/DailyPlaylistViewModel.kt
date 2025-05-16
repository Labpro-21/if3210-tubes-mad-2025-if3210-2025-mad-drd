package com.example.purrytify.ui.screens.playlist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.local.datastore.UserPreferences
import com.example.purrytify.data.repository.SongRepository
import com.example.purrytify.data.repository.TopSongsRepository
import com.example.purrytify.domain.model.PlaylistItem
import com.example.purrytify.domain.player.PlayerBridge
import com.example.purrytify.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Daily Playlist screen
 */
@HiltViewModel
class DailyPlaylistViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val topSongsRepository: TopSongsRepository,
    private val userPreferences: UserPreferences,
    private val playerBridge: PlayerBridge
) : ViewModel() {
    
    private val TAG = "DailyPlaylistViewModel"
    
    // UI state
    private val _uiState = MutableStateFlow<PlaylistUiState>(PlaylistUiState.Loading)
    val uiState = _uiState.asStateFlow()
    
    // Currently playing item
    private val _currentPlayingItem = MutableStateFlow<PlaylistItem?>(null)
    val currentPlayingItem = _currentPlayingItem.asStateFlow()
    
    // User ID
    private val _userId = MutableStateFlow<Int?>(null)
    
    init {
        // Get user ID
        viewModelScope.launch {
            userPreferences.userId.collect { userId ->
                if (userId != null) {
                    _userId.value = userId
                    loadDailyPlaylist()
                } else {
                    _uiState.value = PlaylistUiState.Error("User not found")
                }
            }
        }
    }
    
    /**
     * Load the daily playlist (mix of local and online songs)
     */
    fun loadDailyPlaylist() {
        viewModelScope.launch {
            try {
                _uiState.value = PlaylistUiState.Loading
                
                // Get user ID
                val userId = _userId.value ?: run {
                    _uiState.value = PlaylistUiState.Error("User not found")
                    return@launch
                }
                
                // Get recommended local songs
                val localSongs = getRecommendedLocalSongs(userId)
                
                // Get online songs
                val onlineSongs = getRandomOnlineSongs()
                
                // Mix and shuffle the songs
                val mixedItems = (localSongs + onlineSongs).shuffled()
                
                _uiState.value = if (mixedItems.isEmpty()) {
                    PlaylistUiState.Empty
                } else {
                    PlaylistUiState.Success(mixedItems)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading daily playlist: ${e.message}", e)
                _uiState.value = PlaylistUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * Get recommended local songs based on likes and play history
     */
    private suspend fun getRecommendedLocalSongs(userId: Int, limit: Int = 15): List<PlaylistItem> {
        try {
            // Get all songs first
            val allSongs = songRepository.getAllSongs(userId).first()
            
            // Priority: 1. Liked songs 2. Recently played 3. Random selection
            val likedSongs = allSongs.filter { it.isLiked }.take(limit / 2)
            val remainingCount = limit - likedSongs.size
            
            val recommendedSongs = if (remainingCount <= 0) {
                likedSongs
            } else {
                val playedSongs = allSongs
                    .filter { !it.isLiked && it.lastPlayedAt != null }
                    .sortedByDescending { it.lastPlayedAt }
                    .take(remainingCount)
                
                val finalCount = limit - likedSongs.size - playedSongs.size
                val randomSongs = if (finalCount > 0) {
                    allSongs
                        .filter { !it.isLiked && it.lastPlayedAt == null }
                        .shuffled()
                        .take(finalCount)
                } else {
                    emptyList()
                }
                
                likedSongs + playedSongs + randomSongs
            }
            
            // Convert to playlist items
            return recommendedSongs.map { PlaylistItem.fromLocalSong(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting recommended local songs: ${e.message}", e)
            return emptyList()
        }
    }
    
    /**
     * Get random online songs from top charts
     */
    private suspend fun getRandomOnlineSongs(limit: Int = 15): List<PlaylistItem> {
        try {
            val result = topSongsRepository.getGlobalTopSongs()
            
            return when (result) {
                is Result.Success -> {
                    // Shuffle and take a subset
                    result.data.shuffled().take(limit).map { PlaylistItem.fromOnlineSong(it) }
                }
                else -> {
                    Log.e(TAG, "Error fetching online songs: ${(result as? Result.Error)?.message ?: "Unknown"}")
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting random online songs: ${e.message}", e)
            return emptyList()
        }
    }
    
    /**
     * Handle playing a song from the playlist
     */
    fun playItem(item: PlaylistItem) {
        _currentPlayingItem.value = item
        
        // Use the player bridge to handle playback
        playerBridge.playItem(item)
        
        // Handle the play action based on item type
        viewModelScope.launch {
            try {
                when (item) {
                    is PlaylistItem.LocalSong -> {
                        // Get user ID
                        val userId = _userId.value ?: return@launch
                        
                        // Update last played for local song
                        songRepository.updateLastPlayed(item.id, userId)
                    }
                    // For online songs, we don't need to update play stats currently
                    is PlaylistItem.OnlineSong -> { /* No special action needed */ }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating play status: ${e.message}", e)
            }
        }
    }
    
    /**
     * Get the actual song ID to use for navigation
     */
    fun getNavigationId(item: PlaylistItem): String {
        return playerBridge.getNavigationId(item)
    }
}

/**
 * UI state for the playlist screen
 */
sealed class PlaylistUiState {
    data object Loading : PlaylistUiState()
    data object Empty : PlaylistUiState()
    data class Success(val items: List<PlaylistItem>) : PlaylistUiState()
    data class Error(val message: String) : PlaylistUiState()
}