package com.example.purrytify.ui.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.local.datastore.UserPreferences
import com.example.purrytify.data.repository.SongRepository
import com.example.purrytify.domain.model.Song
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
    private val userPreferences: UserPreferences
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
    private val _userCountry = MutableStateFlow("ID") // Default to Indonesia
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
                _userCountry.value = location ?: "ID" // Default to Indonesia
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
     * Handle song play action
     */
    fun playSong(song: Song) {
        _currentPlayingSong.value = song
        
        // Update the song's last played timestamp
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