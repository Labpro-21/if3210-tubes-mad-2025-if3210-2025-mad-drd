package com.example.purrytify.ui.screens.topsongs

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.local.datastore.UserPreferences
import com.example.purrytify.data.repository.SongRepository
import com.example.purrytify.data.repository.TopSongsRepository
import com.example.purrytify.domain.model.OnlineSong
import com.example.purrytify.domain.util.Result
import com.example.purrytify.util.CountryUtils
import com.example.purrytify.util.NetworkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Top Songs screen (Global and Country)
 */
@HiltViewModel
class TopSongsViewModel @Inject constructor(
    private val topSongsRepository: TopSongsRepository,
    private val songRepository: SongRepository,
    private val userPreferences: UserPreferences,
    private val networkManager: NetworkManager
) : ViewModel() {

    private val TAG = "TopSongsViewModel"

    // UI state
    private val _uiState = MutableStateFlow<TopSongsUiState>(TopSongsUiState.Loading)
    val uiState = _uiState.asStateFlow()
    
    // Current playing song
    private val _currentPlayingSong = MutableStateFlow<OnlineSong?>(null)
    val currentPlayingSong = _currentPlayingSong.asStateFlow()
    
    // User country code
    private val _userCountry = MutableStateFlow("") // Default to empty string
    val userCountry = _userCountry.asStateFlow()
    
    // Whether user's country has top songs available
    private val _isCountrySongsAvailable = MutableStateFlow(true)
    val isCountrySongsAvailable = _isCountrySongsAvailable.asStateFlow()
    
    // Flag to check if all songs are downloaded
    private val _areAllSongsDownloaded = MutableStateFlow(false)
    val areAllSongsDownloaded = _areAllSongsDownloaded.asStateFlow()
    
    // Downloading state
    private val _downloadingState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadingState = _downloadingState.asStateFlow()
    
    // Network status
    private val _isNetworkAvailable = MutableStateFlow(true)
    
    // User ID
    private val _userId = MutableStateFlow<Int?>(null)

    init {
        // Get user country
        viewModelScope.launch {
            userPreferences.userLocation.collect { location ->
                _userCountry.value = location ?: ""
                _isCountrySongsAvailable.value = CountryUtils.isTopSongsAvailableForCountry(_userCountry.value)
            }
        }
        
        // Get user ID
        viewModelScope.launch {
            userPreferences.userId.collect { userId ->
                _userId.value = userId
            }
        }
        
        // Monitor network status
        viewModelScope.launch {
            networkManager.isNetworkAvailable.collectLatest { isAvailable ->
                _isNetworkAvailable.value = isAvailable
                
                // If we have a success state and network becomes unavailable, don't change state
                // Otherwise, if we're loading or in error state, update to NoInternet
                val currentState = _uiState.value
                if (currentState !is TopSongsUiState.Success && !isAvailable) {
                    _uiState.value = TopSongsUiState.NoInternet
                }
            }
        }
    }

    /**
     * Load global top songs from the API
     */
    fun loadGlobalTopSongs() {
        viewModelScope.launch {
            // Check network connectivity first
            if (!_isNetworkAvailable.value) {
                _uiState.value = TopSongsUiState.NoInternet
                return@launch
            }
            
            // Set loading state
            _uiState.value = TopSongsUiState.Loading
            
            try {
                when (val result = topSongsRepository.getGlobalTopSongs()) {
                    is Result.Success -> {
                        val songs = result.data
                        _uiState.value = if (songs.isEmpty()) {
                            TopSongsUiState.Empty
                        } else {
                            TopSongsUiState.Success(songs)
                            
                            // Check if all songs are downloaded
                            checkIfAllSongsDownloaded(songs)
                            
                            TopSongsUiState.Success(songs)
                        }
                    }
                    is Result.Error -> {
                        Log.e(TAG, "Error loading global top songs: ${result.message}")
                        _uiState.value = TopSongsUiState.Error(result.message)
                    }
                    is Result.Loading -> {
                        // Already set loading state above
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading global top songs: ${e.message}", e)
                _uiState.value = TopSongsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Load country top songs from the API
     */
    fun loadCountryTopSongs() {
        viewModelScope.launch {
            // Check network connectivity first
            if (!_isNetworkAvailable.value) {
                _uiState.value = TopSongsUiState.NoInternet
                return@launch
            }
            
            // Check if user's country has top songs available
            if (!_isCountrySongsAvailable.value) {
                _uiState.value = TopSongsUiState.UnavailableForCountry(_userCountry.value)
                return@launch
            }
            
            // Set loading state
            _uiState.value = TopSongsUiState.Loading
            
            try {
                when (val result = topSongsRepository.getCountryTopSongs(_userCountry.value)) {
                    is Result.Success -> {
                        val songs = result.data
                        _uiState.value = if (songs.isEmpty()) {
                            TopSongsUiState.Empty
                        } else {
                            // Check if all songs are downloaded
                            checkIfAllSongsDownloaded(songs)
                            
                            TopSongsUiState.Success(songs)
                        }
                    }
                    is Result.Error -> {
                        Log.e(TAG, "Error loading country top songs: ${result.message}")
                        _uiState.value = TopSongsUiState.Error(result.message)
                    }
                    is Result.Loading -> {
                        // Already set loading state above
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading country top songs: ${e.message}", e)
                _uiState.value = TopSongsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Play an online song
     */
    fun playSong(song: OnlineSong) {
        _currentPlayingSong.value = song
    }

    /**
     * Check if all songs in the list are downloaded
     */
    private suspend fun checkIfAllSongsDownloaded(songs: List<OnlineSong>) {
        val userId = _userId.value ?: return
        
        try {
            // Get the online IDs of all songs
            val onlineIds = songs.map { it.id.toString() }
            
            // Check if all songs are downloaded
            val allDownloaded = songRepository.areAllOnlineSongsDownloaded(onlineIds, userId)
            
            // Update the state
            _areAllSongsDownloaded.value = allDownloaded
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if songs are downloaded: ${e.message}", e)
            _areAllSongsDownloaded.value = false
        }
    }

    /**
     * Download all songs from the current list
     */
    fun downloadAllSongs() {
        viewModelScope.launch {
            val userId = _userId.value ?: return@launch
            
            // Get the current song list
            val songs = when (val state = _uiState.value) {
                is TopSongsUiState.Success -> state.songs
                else -> {
                    Log.e(TAG, "Cannot download songs: No songs available")
                    return@launch
                }
            }
            
            // Set downloading state to in progress
            _downloadingState.value = DownloadState.InProgress(0, songs.size)
            
            try {
                // Download each song individually to handle progress updates
                var successCount = 0
                var failureCount = 0
                
                for (i in songs.indices) {
                    val song = songs[i]
                    
                    // Update progress
                    _downloadingState.value = DownloadState.InProgress(i + 1, songs.size)
                    
                    // Skip if already downloaded
                    if (songRepository.isOnlineSongDownloaded(song.id.toString(), userId)) {
                        successCount++
                        continue
                    }
                    
                    // Download the song
                    when (val result = topSongsRepository.downloadSong(song, userId)) {
                        is Result.Success -> {
                            successCount++
                        }
                        is Result.Error -> {
                            failureCount++
                            Log.e(TAG, "Error downloading song ${song.title}: ${result.message}")
                        }
                        is Result.Loading -> { /* Ignore */ }
                    }
                }
                
                // Update state based on download results
                if (failureCount == 0) {
                    // All songs downloaded successfully
                    _downloadingState.value = DownloadState.Success(successCount)
                    _areAllSongsDownloaded.value = true
                } else if (successCount == 0) {
                    // All downloads failed
                    _downloadingState.value = DownloadState.Error("Failed to download any songs")
                } else {
                    // Some succeeded, some failed
                    _downloadingState.value = DownloadState.Error(
                        "Downloaded $successCount songs, but $failureCount failed"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception downloading songs: ${e.message}", e)
                _downloadingState.value = DownloadState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Reset the downloading state
     */
    fun resetDownloadingState() {
        _downloadingState.value = DownloadState.Idle
    }
}

/**
 * UI state for the Top Songs screen
 */
sealed class TopSongsUiState {
    data object Loading : TopSongsUiState()
    data object Empty : TopSongsUiState()
    data class Success(val songs: List<OnlineSong>) : TopSongsUiState()
    data class Error(val message: String) : TopSongsUiState()
    data object NoInternet : TopSongsUiState()
    data class UnavailableForCountry(val countryCode: String) : TopSongsUiState()
}

/**
 * Download state for the Top Songs screen
 */
sealed class DownloadState {
    data object Idle : DownloadState()
    data class InProgress(val progress: Int, val total: Int) : DownloadState()
    data class Success(val count: Int) : DownloadState()
    data class Error(val message: String) : DownloadState()
}