package com.example.purrytify.ui.screens.library

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.local.datastore.UserPreferences
import com.example.purrytify.data.repository.SongRepository
import com.example.purrytify.domain.model.PlaylistItem
import com.example.purrytify.domain.model.Song
import com.example.purrytify.domain.player.PlaybackContext
import com.example.purrytify.domain.player.PlayerBridge
import com.example.purrytify.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val userPreferences: UserPreferences,
    private val playerBridge: PlayerBridge
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
        
        // Setup songs collection based on active tab and search
        setupSongsCollection()
    }
    
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun setupSongsCollection() {
        viewModelScope.launch {
            // Start with a loading state
            _uiState.value = LibraryUiState.Loading
            
            // Create a trigger flow that emits immediately on initialization and then when tab or search changes
            val initialLoadTrigger = MutableStateFlow(Unit)
            
            // Combine trigger with tab and search to ensure initial load works
            val changeFlow = combine(
                initialLoadTrigger,
                _activeTab,
                _searchQuery,
                _currentUserId.filterNotNull()
            ) { _, tab, query, userId -> Triple(tab, query, userId) }
            
            // Use this flow to handle loading state and data fetching
            changeFlow
                .onEach { (tab, _, _) ->
                    // Set loading state when tab or search changes
                    Log.d(TAG, "Loading songs for tab: $tab")
                    _uiState.value = LibraryUiState.Loading
                }
                .collectLatest { (tab, query, userId) ->
                    Log.d(TAG, "Fetching songs for tab: $tab, query: '$query', userId: $userId")
                    
                    try {
                        // Get songs flow based on the tab
                        val songsFlow = when (tab) {
                            LibraryTab.ALL -> songRepository.getAllSongs(userId)
                            LibraryTab.LIKED -> songRepository.getLikedSongs(userId)
                            LibraryTab.DOWNLOADED -> songRepository.getDownloadedSongs(userId)
                        }
                        
                        // Collect the songs flow and update UI state
                        songsFlow.collect { songs ->
                            // Filter songs based on search query
                            val filteredSongs = if (query.isBlank()) {
                                songs
                            } else {
                                val searchLower = query.lowercase()
                                songs.filter { song ->
                                    song.title.lowercase().contains(searchLower) ||
                                    song.artist.lowercase().contains(searchLower)
                                }
                            }
                            
                            Log.d(TAG, "Fetched ${filteredSongs.size} songs for tab: $tab")
                            
                            // Update UI state
                            _uiState.value = if (filteredSongs.isEmpty()) {
                                LibraryUiState.Empty
                            } else {
                                LibraryUiState.Success(filteredSongs)
                            }
                        }
                    } catch (e: Exception) {
                        // Only set error state for non-cancellation exceptions
                        if (e is kotlinx.coroutines.CancellationException || 
                            e.cause is kotlinx.coroutines.CancellationException ||
                            e.message?.contains("cancelled", ignoreCase = true) == true) {
                            // This is expected when switching tabs, just log it
                            Log.d(TAG, "Flow collection cancelled (expected): ${e.message}")
                        } else {
                            // Real error, update UI
                            Log.e(TAG, "Error fetching songs: ${e.message}", e)
                            _uiState.value = LibraryUiState.Error("Failed to load songs: ${e.message}")
                        }
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
     * Play a song from the library
     */
    fun playSong(song: Song) {
        Log.d(TAG, "Playing song from library: ${song.title}")
        
        // Get the current filtered songs list for the queue
        val currentSongs = when (val state = _uiState.value) {
            is LibraryUiState.Success -> state.songs
            else -> listOf(song) // Fallback to single song
        }
        
        // Create a queue from the current view (filtered songs)
        val queue = currentSongs.map { PlaylistItem.fromLocalSong(it) }
        val startIndex = queue.indexOfFirst { it.id == song.id }
        
        if (startIndex >= 0) {
            val context = when (_activeTab.value) {
                LibraryTab.ALL -> PlaybackContext.Library
                LibraryTab.LIKED -> PlaybackContext.Library
                LibraryTab.DOWNLOADED -> PlaybackContext.Library
            }
            playerBridge.playQueue(queue, startIndex, context)
            
            // Update the last played timestamp
            updateLastPlayed(song)
        }
    }
    
    /**
     * Update the song's last played timestamp
     */
    private fun updateLastPlayed(song: Song) {
        viewModelScope.launch {
            try {
                val userId = _currentUserId.value ?: return@launch
                songRepository.updateLastPlayed(song.id, userId)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating last played: ${e.message}", e)
            }
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
    
    /**
     * Reset the filter state when navigating away from the Library screen
     */
    fun resetState() {
        _activeTab.value = LibraryTab.ALL
        _searchQuery.value = ""
    }
}