package com.example.purrytify.ui.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.local.datastore.UserPreferences
import com.example.purrytify.data.repository.SongRepository
import com.example.purrytify.domain.model.PlaylistItem
import com.example.purrytify.domain.model.Song
import com.example.purrytify.domain.player.PlaybackContext
import com.example.purrytify.domain.player.PlayerBridge
import com.example.purrytify.util.CountryUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the home screen
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val userPreferences: UserPreferences,
    private val playerBridge: PlayerBridge
) : ViewModel() {

    private val TAG = "HomeViewModel"

    // UI states
    private val _loadingState = MutableStateFlow(true)
    val loadingState = _loadingState.asStateFlow()

    // New songs section (recent uploads)
    private val _newSongs = MutableStateFlow<List<Song>>(emptyList())
    val newSongs = _newSongs.asStateFlow()

    // Recently played songs
    private val _recentlyPlayedSongs = MutableStateFlow<List<Song>>(emptyList())
    val recentlyPlayedSongs = _recentlyPlayedSongs.asStateFlow()

    // Currently playing song
    private val _currentPlayingSong = MutableStateFlow<Song?>(null)
    val currentPlayingSong = _currentPlayingSong.asStateFlow()

    // User country
    private val _userCountry = MutableStateFlow("") // Default to empty string
    val userCountry = _userCountry.asStateFlow()
    
    // Whether user's country has top songs available
    private val _isCountrySongsAvailable = MutableStateFlow(true)
    val isCountrySongsAvailable = _isCountrySongsAvailable.asStateFlow()
    
    // User ID
    private val _userId = MutableStateFlow<Int?>(null)

    init {
        // Load user country
        viewModelScope.launch {
            userPreferences.userLocation.collect { location ->
                _userCountry.value = location ?: "" // Default to empty string
                _isCountrySongsAvailable.value = CountryUtils.isTopSongsAvailableForCountry(_userCountry.value)
            }
        }
        
        // Load user ID
        viewModelScope.launch {
            userPreferences.userId.collect { userId ->
                if (userId != null) {
                    _userId.value = userId
                    loadSongs(userId)
                } else {
                    _loadingState.value = false
                    Log.e(TAG, "User ID is null")
                }
            }
        }
        
        // Monitor current playing item from player bridge
        viewModelScope.launch {
            playerBridge.currentItem.collect { currentItem ->
                // Update current playing song if it's a local song
                _currentPlayingSong.value = when (currentItem) {
                    is PlaylistItem.LocalSong -> {
                        Song(
                            id = currentItem.id,
                            title = currentItem.title,
                            artist = currentItem.artist,
                            artworkPath = currentItem.artworkPath,
                            filePath = currentItem.filePath,
                            duration = currentItem.durationMs,
                            userId = 0, // Will be set by repository
                            isLiked = currentItem.isLiked,
                            createdAt = java.time.LocalDateTime.now(),
                            updatedAt = java.time.LocalDateTime.now()
                        )
                    }
                    else -> null
                }
            }
        }
    }

    /**
     * Load all songs for the home screen
     */
    private fun loadSongs(userId: Int) {
        viewModelScope.launch {
            try {
                // Start loading
                _loadingState.value = true
                
                // Combine the flows for new songs and recently played
                val allSongsFlow = songRepository.getAllSongs(userId)
                
                // New songs - sort by creation date (newest first) and take first 10
                val newSongsFlow = allSongsFlow.map { songs ->
                    songs.sortedByDescending { it.createdAt }.take(10)
                }
                
                // Recently played songs - filter non-null lastPlayedAt, sort and take 10
                val recentlyPlayedFlow = allSongsFlow.map { songs ->
                    songs.filter { it.lastPlayedAt != null }
                        .sortedByDescending { it.lastPlayedAt }
                        .take(10)
                }
                
                // Collect both flows into one
                combine(newSongsFlow, recentlyPlayedFlow) { newSongs, recentlyPlayed ->
                    _newSongs.value = newSongs
                    _recentlyPlayedSongs.value = recentlyPlayed
                    _loadingState.value = false
                }.collectLatest { /* Just collect to trigger the combine */ }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading songs: ${e.message}", e)
                _loadingState.value = false
            }
        }
    }

    /**
     * Play a song from the new songs section
     */
    fun playFromNewSongs(song: Song) {
        Log.d(TAG, "Playing from new songs: ${song.title}")
        
        // Create a queue from all new songs
        val queue = _newSongs.value.map { PlaylistItem.fromLocalSong(it) }
        val startIndex = queue.indexOfFirst { it.id == song.id }
        
        if (startIndex >= 0) {
            playerBridge.playQueue(queue, startIndex, PlaybackContext.NewSongs)
            updateLastPlayed(song)
        }
    }
    
    /**
     * Play a song from the recently played section
     */
    fun playFromRecentlyPlayed(song: Song) {
        Log.d(TAG, "Playing from recently played: ${song.title}")
        
        // Create a queue from all recently played songs
        val queue = _recentlyPlayedSongs.value.map { PlaylistItem.fromLocalSong(it) }
        val startIndex = queue.indexOfFirst { it.id == song.id }
        
        if (startIndex >= 0) {
            playerBridge.playQueue(queue, startIndex, PlaybackContext.RecentlyPlayed)
            updateLastPlayed(song)
        }
    }

    /**
     * Handle song play action (legacy method for compatibility)
     */
    fun playSong(song: Song) {
        Log.d(TAG, "Playing song: ${song.title}")
        
        // Default to playing as single song
        playerBridge.playSong(song)
        updateLastPlayed(song)
    }
    
    /**
     * Update the song's last played timestamp
     */
    private fun updateLastPlayed(song: Song) {
        viewModelScope.launch {
            try {
                val userId = _userId.value ?: return@launch
                songRepository.updateLastPlayed(song.id, userId)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating last played: ${e.message}", e)
            }
        }
    }

    /**
     * Refresh the songs data
     */
    fun refreshSongs() {
        viewModelScope.launch {
            _userId.value?.let { loadSongs(it) }
        }
    }
}