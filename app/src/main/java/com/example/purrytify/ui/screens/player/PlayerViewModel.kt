package com.example.purrytify.ui.screens.player

import android.net.Uri
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
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Player screen and MiniPlayer
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerBridge: PlayerBridge,
    private val songRepository: SongRepository,
    private val topSongsRepository: TopSongsRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {
    
    private val TAG = "PlayerViewModel"
    
    // Player state from bridge
    val currentItem = playerBridge.currentItem
    val isPlaying = playerBridge.isPlaying
    val progress = playerBridge.progress
    val currentPosition = playerBridge.currentPosition
    val duration = playerBridge.duration
    val queue = playerBridge.queue
    val currentIndex = playerBridge.currentIndex
    val playbackContext = playerBridge.playbackContext
    
    // Dialog states
    private val _showEditDialog = MutableStateFlow(false)
    val showEditDialog: StateFlow<Boolean> = _showEditDialog.asStateFlow()
    
    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog.asStateFlow()
    
    // Loading states
    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()
    
    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating.asStateFlow()
    
    // Download status for online songs
    private val _isCurrentSongDownloaded = MutableStateFlow(false)
    val isCurrentSongDownloaded: StateFlow<Boolean> = _isCurrentSongDownloaded.asStateFlow()
    
    // Error states
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // User ID
    private val _userId = MutableStateFlow<Int?>(null)
    
    init {
        viewModelScope.launch {
            userPreferences.userId.collect { userId ->
                _userId.value = userId
            }
        }
        
        // Monitor current item changes to check download status
        viewModelScope.launch {
            currentItem.collect { item ->
                checkDownloadStatus(item)
            }
        }
    }
    
    /**
     * Check if the current online song is downloaded
     */
    private suspend fun checkDownloadStatus(item: PlaylistItem?) {
        if (item is PlaylistItem.OnlineSong) {
            val userId = _userId.value ?: return
            try {
                val isDownloaded = songRepository.isOnlineSongDownloaded(item.originalId, userId)
                _isCurrentSongDownloaded.value = isDownloaded
                Log.d(TAG, "Download status for ${item.title}: $isDownloaded")
            } catch (e: Exception) {
                Log.e(TAG, "Error checking download status: ${e.message}", e)
                _isCurrentSongDownloaded.value = false
            }
        } else {
            _isCurrentSongDownloaded.value = false
        }
    }
    
    /**
     * Play/pause toggle
     */
    fun playPause() {
        playerBridge.togglePlayPause()
    }
    
    /**
     * Play next song
     */
    fun next() {
        playerBridge.next()
    }
    
    /**
     * Play previous song
     */
    fun previous() {
        playerBridge.previous()
    }
    
    /**
     * Seek to specific position
     */
    fun seekTo(position: Long) {
        playerBridge.seekTo(position)
    }
    
    /**
     * Toggle like status for local songs
     */
    fun toggleLike() {
        val item = currentItem.value
        if (item is PlaylistItem.LocalSong) {
            viewModelScope.launch {
                val userId = _userId.value ?: return@launch
                try {
                    Log.d(TAG, "Toggling like for song: ${item.title}, current state: ${item.isLiked}")
                    
                    // Toggle like in database
                    val result = songRepository.toggleLike(item.id, userId, !item.isLiked)
                    
                    when (result) {
                        is Result.Success -> {
                            // Refresh the song data to get updated like status
                            refreshCurrentSong()
                        }
                        is Result.Error -> {
                            Log.e(TAG, "Error toggling like: ${result.message}")
                            _errorMessage.value = "Failed to update like status"
                        }
                        is Result.Loading -> {
                            // No-op
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error toggling like: ${e.message}", e)
                    _errorMessage.value = "Failed to update like status"
                }
            }
        }
    }
    
    /**
     * Download online song to local storage
     */
    fun downloadSong() {
        val item = currentItem.value
        if (item is PlaylistItem.OnlineSong && !_isCurrentSongDownloaded.value) {
            viewModelScope.launch {
                val userId = _userId.value ?: return@launch
                _isDownloading.value = true
                
                try {
                    Log.d(TAG, "Downloading song: ${item.title}")
                    
                    // Convert to OnlineSong for repository
                    val onlineSong = com.example.purrytify.domain.model.OnlineSong(
                        id = item.originalId.toLong(),
                        title = item.title,
                        artist = item.artist,
                        artworkUrl = item.artworkUrl,
                        songUrl = item.songUrl,
                        duration = item.duration,
                        country = "GLOBAL",
                        rank = 0,
                        createdAt = "",
                        updatedAt = ""
                    )
                    
                    when (val result = topSongsRepository.downloadSong(onlineSong, userId)) {
                        is Result.Success -> {
                            Log.d(TAG, "Song downloaded successfully")
                            
                            // Update download status
                            _isCurrentSongDownloaded.value = true
                            _isDownloading.value = false
                        }
                        is Result.Error -> {
                            Log.e(TAG, "Error downloading song: ${result.message}")
                            _errorMessage.value = result.message
                            _isDownloading.value = false
                        }
                        is Result.Loading -> {
                            // Keep loading state
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error downloading song: ${e.message}", e)
                    _errorMessage.value = "Failed to download song"
                    _isDownloading.value = false
                }
            }
        }
    }
    
    /**
     * Show edit dialog for local songs
     */
    fun showEditDialog() {
        if (currentItem.value is PlaylistItem.LocalSong) {
            _showEditDialog.value = true
        }
    }
    
    /**
     * Hide edit dialog
     */
    fun hideEditDialog() {
        _showEditDialog.value = false
        _errorMessage.value = null
    }
    
    /**
     * Show delete confirmation dialog for local songs
     */
    fun showDeleteDialog() {
        if (currentItem.value is PlaylistItem.LocalSong) {
            _showDeleteDialog.value = true
        }
    }
    
    /**
     * Hide delete confirmation dialog
     */
    fun hideDeleteDialog() {
        _showDeleteDialog.value = false
    }
    
    /**
     * Update song metadata
     */
    fun updateSong(title: String, artist: String, audioUri: Uri?, artworkUri: Uri?) {
        val item = currentItem.value
        if (item is PlaylistItem.LocalSong) {
            viewModelScope.launch {
                _isUpdating.value = true
                
                try {
                    Log.d(TAG, "Updating song: ${item.title}")
                    
                    when (val result = songRepository.updateSong(item.id, title, artist, artworkUri)) {
                        is Result.Success -> {
                            Log.d(TAG, "Song updated successfully")
                            
                            // Refresh current song data
                            refreshCurrentSong()
                            
                            _isUpdating.value = false
                            _showEditDialog.value = false
                        }
                        is Result.Error -> {
                            Log.e(TAG, "Error updating song: ${result.message}")
                            _errorMessage.value = result.message
                            _isUpdating.value = false
                        }
                        is Result.Loading -> {
                            // Keep loading state
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating song: ${e.message}", e)
                    _errorMessage.value = "Failed to update song"
                    _isUpdating.value = false
                }
            }
        }
    }
    
    /**
     * Delete song from local storage
     */
    fun deleteSong() {
        val item = currentItem.value
        if (item is PlaylistItem.LocalSong) {
            viewModelScope.launch {
                try {
                    Log.d(TAG, "Deleting song: ${item.title}")
                    
                    when (val result = songRepository.deleteSong(item.id)) {
                        is Result.Success -> {
                            Log.d(TAG, "Song deleted successfully")
                            // Song deleted, move to next or stop
                            if (queue.value.size > 1) {
                                next()
                            } else {
                                playerBridge.stop()
                            }
                            _showDeleteDialog.value = false
                        }
                        is Result.Error -> {
                            Log.e(TAG, "Error deleting song: ${result.message}")
                            _errorMessage.value = result.message
                        }
                        is Result.Loading -> {
                            // Keep loading state
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting song: ${e.message}", e)
                    _errorMessage.value = "Failed to delete song"
                }
            }
        }
    }
    
    /**
     * Refresh current song data from database (for local songs)
     */
    private suspend fun refreshCurrentSong() {
        val item = currentItem.value
        if (item is PlaylistItem.LocalSong) {
            try {
                when (val result = songRepository.getSongById(item.id)) {
                    is Result.Success -> {
                        val updatedSong = result.data
                        val updatedPlaylistItem = PlaylistItem.fromLocalSong(updatedSong)
                        playerBridge.updateCurrentItem(updatedPlaylistItem)
                        Log.d(TAG, "Refreshed current song data")
                    }
                    is Result.Error -> {
                        Log.e(TAG, "Error refreshing song data: ${result.message}")
                    }
                    is Result.Loading -> {
                        // No-op
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing song data: ${e.message}", e)
            }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * Stop playback (for logout)
     */
    fun stopPlayback() {
        playerBridge.stop()
    }
    
    /**
     * Get formatted current position
     */
    fun getFormattedCurrentPosition(): String {
        return playerBridge.getFormattedCurrentPosition()
    }
    
    /**
     * Get formatted duration
     */
    fun getFormattedDuration(): String {
        return playerBridge.getFormattedDuration()
    }
    
    /**
     * Check if current item is a local song
     */
    fun isCurrentItemLocal(): Boolean {
        return currentItem.value is PlaylistItem.LocalSong
    }
    
    /**
     * Check if current item is an online song
     */
    fun isCurrentItemOnline(): Boolean {
        return currentItem.value is PlaylistItem.OnlineSong
    }
    
    /**
     * Check if current local song is liked
     */
    fun isCurrentSongLiked(): Boolean {
        val item = currentItem.value
        return if (item is PlaylistItem.LocalSong) item.isLiked else false
    }

    fun getNavigationId(item: PlaylistItem): String {
        return when (item) {
            is PlaylistItem.LocalSong -> item.id
            is PlaylistItem.OnlineSong -> item.originalId
        }
    }
}