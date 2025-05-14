package com.example.purrytify.ui.screens.library

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.local.datastore.UserPreferences
import com.example.purrytify.data.repository.SongRepository
import com.example.purrytify.domain.model.Song
import com.example.purrytify.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {
    
    private val TAG = "LibraryViewModel"
    
    // UI State
    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val uiState: StateFlow<LibraryUiState> = _uiState
    
    // Active tab (All, Liked, Downloaded)
    private val _activeTab = MutableStateFlow(LibraryTab.ALL)
    val activeTab: StateFlow<LibraryTab> = _activeTab
    
    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery
    
    // Add song dialog visibility
    private val _showAddSongDialog = MutableStateFlow(false)
    val showAddSongDialog: StateFlow<Boolean> = _showAddSongDialog
    
    // Loading state for adding a song
    private val _isAddingLoading = MutableStateFlow(false)
    val isAddingLoading: StateFlow<Boolean> = _isAddingLoading
    
    // Error message for adding a song
    private val _addSongError = MutableStateFlow<String?>(null)
    val addSongError: StateFlow<String?> = _addSongError
    
    // Currently playing song
    private val _currentPlayingSong = MutableStateFlow<Song?>(null)
    val currentPlayingSong: StateFlow<Song?> = _currentPlayingSong
    
    // Playback state
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying
    
    // Playback progress (0.0 to 1.0)
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress
    
    // Current user ID
    private val _currentUserId = MutableStateFlow<Int?>(null)
    
    init {
        // Load user ID
        viewModelScope.launch {
            userPreferences.userId.collect { userId ->
                if (userId != null) {
                    _currentUserId.value = userId
                    Log.d(TAG, "User ID: $userId")
                    refreshSongs()
                } else {
                    _uiState.value = LibraryUiState.Error("User not found")
                }
            }
        }
        
        // Setup songs collection based on active tab and search
        setupSongsCollection()
    }
    
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun setupSongsCollection() {
        viewModelScope.launch {
            // Combine active tab, search query, and user ID
            combine(
                _activeTab,
                _searchQuery,
                _currentUserId.filterNotNull()
            ) { tab, query, userId ->
                Triple(tab, query, userId)
            }.flatMapLatest { (tab, query, userId) ->
                // Get songs based on the active tab
                when (tab) {
                    LibraryTab.ALL -> songRepository.getAllSongs(userId)
                    LibraryTab.LIKED -> songRepository.getLikedSongs(userId)
                    LibraryTab.DOWNLOADED -> songRepository.getDownloadedSongs(userId)
                }
            }.map { songs ->
                // Filter songs based on search query
                if (_searchQuery.value.isBlank()) {
                    songs
                } else {
                    val searchLower = _searchQuery.value.lowercase()
                    songs.filter { song ->
                        song.title.lowercase().contains(searchLower) ||
                        song.artist.lowercase().contains(searchLower)
                    }
                }
            }.collect { filteredSongs ->
                // Update UI state based on filtered songs
                _uiState.value = if (filteredSongs.isEmpty()) {
                    LibraryUiState.Empty
                } else {
                    LibraryUiState.Success(filteredSongs)
                }
            }
        }
    }
    
    /**
     * Switch to a different tab
     */
    fun switchTab(tab: LibraryTab) {
        _activeTab.value = tab
    }
    
    /**
     * Set the search query
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    /**
     * Show the add song dialog
     */
    fun showAddSongDialog() {
        _showAddSongDialog.value = true
        _addSongError.value = null
    }
    
    /**
     * Dismiss the add song dialog
     */
    fun dismissAddSongDialog() {
        _showAddSongDialog.value = false
        _addSongError.value = null
    }
    
    /**
     * Add a new song
     */
    fun addSong(audioUri: Uri, title: String, artist: String, artworkUri: Uri?) {
        viewModelScope.launch {
            _isAddingLoading.value = true
            _addSongError.value = null
            
            val userId = _currentUserId.value
            if (userId == null) {
                _addSongError.value = "User not found"
                _isAddingLoading.value = false
                return@launch
            }
            
            try {
                when (val result = songRepository.addSong(userId, title, artist, audioUri, artworkUri)) {
                    is Result.Success -> {
                        Log.d(TAG, "Song added successfully: ${result.data.title}")
                        _showAddSongDialog.value = false
                        refreshSongs()
                    }
                    is Result.Error -> {
                        Log.e(TAG, "Error adding song: ${result.message}")
                        _addSongError.value = result.message
                    }
                    is Result.Loading -> {
                        // No-op
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception adding song: ${e.message}", e)
                _addSongError.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isAddingLoading.value = false
            }
        }
    }
    
    /**
     * Refresh the songs list
     */
    private fun refreshSongs() {
        _uiState.value = LibraryUiState.Loading
        // The collection flow will automatically refresh
    }
    
    /**
     * Play a song
     */
    fun playSong(song: Song) {
        Log.d(TAG, "Playing song: ${song.title}")
        _currentPlayingSong.value = song
        _isPlaying.value = true
        
        // Update the last played timestamp
        viewModelScope.launch {
            val userId = _currentUserId.value ?: return@launch
            songRepository.updateLastPlayed(song.id, userId)
        }
    }
    
    /**
     * Toggle like status for a song
     */
    fun toggleLike(song: Song) {
        viewModelScope.launch {
            val userId = _currentUserId.value ?: return@launch
            songRepository.toggleLike(song.id, userId, !song.isLiked)
        }
    }
}