// First, let's update the LibraryViewModel.kt to handle search functionality

package com.example.purrytify.ui.screens.library

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.repository.SongRepository
import com.example.purrytify.domain.model.Song
import com.example.purrytify.domain.util.Resource
import com.example.purrytify.service.MusicPlayerService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val songRepository: SongRepository,
    application: Application
) : AndroidViewModel(application) {
    // UI States
    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val uiState: StateFlow<LibraryUiState> = _uiState
    private val _activeTab = MutableStateFlow(LibraryTab.ALL)
    val activeTab: StateFlow<LibraryTab> = _activeTab
    private val _showAddSongDialog = MutableStateFlow(false)
    val showAddSongDialog: StateFlow<Boolean> = _showAddSongDialog
    private val _showEditSongDialog = MutableStateFlow(false)
    val showEditSongDialog: StateFlow<Boolean> = _showEditSongDialog
    private val _showDeleteSongDialog = MutableStateFlow(false)
    val showDeleteSongDialog: StateFlow<Boolean> = _showDeleteSongDialog
    private val _isAddingLoading = MutableStateFlow(false)
    val isAddingLoading: StateFlow<Boolean> = _isAddingLoading
    private val _addSongError = MutableStateFlow<String?>(null)
    val addSongError: StateFlow<String?> = _addSongError
    private val _currentEditSong = MutableStateFlow<Song?>(null)
    val currentEditSong: StateFlow<Song?> = _currentEditSong

    // Search functionality
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // Currently playing song
    private val _currentPlayingSong = MutableStateFlow<Song?>(null)
    val currentPlayingSong: StateFlow<Song?> = _currentPlayingSong
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition

    // Song lists
    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())
    private val _likedSongs = MutableStateFlow<List<Song>>(emptyList())

    // Music player service
    private var musicService: MusicPlayerService? = null
    private var serviceBound = false

    // Service connection
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val musicBinder = binder as MusicPlayerService.MusicPlayerBinder
            musicService = musicBinder.getService()
            serviceBound = true

            // Setup auto-play next song feature
            musicService?.onSongCompletion = {
                viewModelScope.launch {
                    playNextSong()
                }
            }

            // Observe service state
            viewModelScope.launch {
                musicService?.isPlaying?.collect { playing ->
                    _isPlaying.value = playing
                }
            }

            viewModelScope.launch {
                musicService?.progress?.collect { p ->
                    _progress.value = p
                }
            }

            viewModelScope.launch {
                musicService?.currentPosition?.collect { pos ->
                    _currentPosition.value = pos
                }
            }

            viewModelScope.launch {
                musicService?.currentSong?.collect { song ->
                    song?.let { _currentPlayingSong.value = it }
                }
            }

            Log.d("LibraryViewModel", "Service connected")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
            serviceBound = false
            Log.d("LibraryViewModel", "Service disconnected")
        }
    }

    init {
        loadSongs()
        bindMusicService()
    }

    override fun onCleared() {
        super.onCleared()
        unbindMusicService()
    }

    private fun bindMusicService() {
        val context = getApplication<Application>()
        val intent = Intent(context, MusicPlayerService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun unbindMusicService() {
        if (serviceBound) {
            getApplication<Application>().unbindService(serviceConnection)
            serviceBound = false
        }
    }

    private fun loadSongs() {
        viewModelScope.launch {
            _uiState.value = LibraryUiState.Loading

            // Combine all songs and liked songs flows
            combine(
                songRepository.getAllSongs(),
                songRepository.getLikedSongs(),
                _searchQuery
            ) { allSongs, likedSongs, query ->
                _allSongs.value = allSongs
                _likedSongs.value = likedSongs

                // Get current tab songs
                val tabSongs = when (_activeTab.value) {
                    LibraryTab.ALL -> allSongs
                    LibraryTab.LIKED -> likedSongs
                }

                // Apply search filter if query is not empty
                val filteredSongs = if (query.isNotEmpty()) {
                    tabSongs.filter { song ->
                        song.title.contains(query, ignoreCase = true) ||
                                song.artist.contains(query, ignoreCase = true)
                    }
                } else {
                    tabSongs
                }

                // Update UI state
                _uiState.value = if (filteredSongs.isEmpty()) {
                    if (query.isNotEmpty()) {
                        LibraryUiState.Empty
                    } else {
                        LibraryUiState.Empty
                    }
                } else {
                    LibraryUiState.Success(filteredSongs)
                }
            }.catch { e ->
                _uiState.value = LibraryUiState.Error(e.message ?: "Failed to load songs")
            }.collect()
        }
    }

    // Set search query
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun switchTab(tab: LibraryTab) {
        _activeTab.value = tab

        // Update song list based on selected tab
        val songs = when (tab) {
            LibraryTab.ALL -> _allSongs.value
            LibraryTab.LIKED -> _likedSongs.value
        }

        _uiState.value = if (songs.isEmpty()) {
            LibraryUiState.Empty
        } else {
            LibraryUiState.Success(songs)
        }
    }

    fun showAddSongDialog() {
        _addSongError.value = null
        _isAddingLoading.value = false
        _showAddSongDialog.value = true
    }

    fun dismissAddSongDialog() {
        _showAddSongDialog.value = false
        _addSongError.value = null
    }

    fun showEditSongDialog(song: Song) {
        _currentEditSong.value = song
        _addSongError.value = null
        _isAddingLoading.value = false
        _showEditSongDialog.value = true
    }

    fun dismissEditSongDialog() {
        _showEditSongDialog.value = false
        _currentEditSong.value = null
        _addSongError.value = null
    }

    fun showDeleteSongDialog(song: Song) {
        _currentEditSong.value = song
        _showDeleteSongDialog.value = true
    }

    fun dismissDeleteSongDialog() {
        _showDeleteSongDialog.value = false
        _currentEditSong.value = null
    }

    fun addSong(audioUri: Uri, title: String, artist: String, artworkUri: Uri? = null) {
        if (artworkUri == null) {
            _addSongError.value = "Album artwork is required"
            return
        }

        viewModelScope.launch {
            _isAddingLoading.value = true
            _addSongError.value = null
            try {
                val songId = songRepository.addSong(audioUri, title, artist, artworkUri)
                if (songId == null) {
                    _addSongError.value = "Failed to add song"
                } else {
                    dismissAddSongDialog()
                    // The song list will be automatically updated via the flow
                }
            } catch (e: Exception) {
                _addSongError.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isAddingLoading.value = false
            }
        }
    }

    fun updateSong(song: Song, title: String, artist: String, artworkUri: Uri? = null) {
        viewModelScope.launch {
            _isAddingLoading.value = true
            _addSongError.value = null
            try {
                // First create an updated song
                val updatedSong = song.copy(
                    title = title,
                    artist = artist
                )

                // Update the song
                songRepository.updateSong(updatedSong)

                // Handle artwork update if provided
                if (artworkUri != null) {
                    // This would need a method to handle updating artwork
                    // For simplicity, assuming the repository handles this internally
                }

                // If this is the currently playing song, update it immediately
                _currentPlayingSong.value?.let { currentSong ->
                    if (currentSong.id == song.id) {
                        _currentPlayingSong.value = updatedSong.copy(isLiked = currentSong.isLiked)
                    }
                }

                dismissEditSongDialog()
                // The song list will be automatically updated via the flow
            } catch (e: Exception) {
                _addSongError.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isAddingLoading.value = false
            }
        }
    }

    fun deleteSong(song: Song) {
        viewModelScope.launch {
            try {
                songRepository.deleteSong(song)
                dismissDeleteSongDialog()

                // If we're deleting the currently playing song, stop playback
                if (_currentPlayingSong.value?.id == song.id) {
                    stopPlayback()
                }

                // The song list will be automatically updated via the flow
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun toggleLikeSong(song: Song) {
        viewModelScope.launch {
            try {
                // Toggle liked status
                songRepository.toggleLikeSong(song.id, !song.isLiked)

                // If this is the currently playing song, update its liked status immediately
                _currentPlayingSong.value?.let { currentSong ->
                    if (currentSong.id == song.id) {
                        _currentPlayingSong.value = currentSong.copy(isLiked = !currentSong.isLiked)
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun getRecentlyPlayedSongs(): List<Song> {
        // Get the songs from the current UI state
        val allSongs = when (val state = this.uiState.value) {
            is LibraryUiState.Success -> state.songs
            else -> emptyList()
        }

        // Filter songs that have been played (non-null lastPlayedAt)
        // and sort by lastPlayedAt (most recent first)
        return allSongs
            .filter { it.lastPlayedAt != null }
            .sortedByDescending { it.lastPlayedAt }
    }

    fun playSong(song: Song) {
        viewModelScope.launch {
            if (_currentPlayingSong.value?.id == song.id) {
                // Toggle play/pause if it's the same song
                togglePlayPause()
            } else {
                // Play a new song
                _currentPlayingSong.value = song
                musicService?.playSong(song)

                // Mark song as played
                songRepository.markSongAsPlayed(song.id)
            }
        }
    }

    fun togglePlayPause() {
        musicService?.togglePlayPause()
    }

    fun stopPlayback() {
        musicService?.stopPlayback()
        _currentPlayingSong.value = null
    }

    fun updateProgress(newProgress: Float) {
        val progress = newProgress.coerceIn(0f, 1f)
        musicService?.seekToProgress(progress)
    }

    fun updatePosition(newPosition: Long) {
        musicService?.seekTo(newPosition)
    }

    // Get next/previous song functionality
    fun playNextSong() {
        viewModelScope.launch {
            val currentSongId = _currentPlayingSong.value?.id ?: return@launch
            val currentList = when (_activeTab.value) {
                LibraryTab.ALL -> _allSongs.value
                LibraryTab.LIKED -> _likedSongs.value
            }

            // Find the current song index
            val currentIndex = currentList.indexOfFirst { it.id == currentSongId }
            if (currentIndex == -1) return@launch

            // Get the next song (or wrap around to the first if at the end)
            val nextIndex = (currentIndex + 1) % currentList.size
            val nextSong = currentList[nextIndex]

            // Play the next song
            playSong(nextSong)
        }
    }

    fun playPreviousSong() {
        viewModelScope.launch {
            val currentSongId = _currentPlayingSong.value?.id ?: return@launch
            val currentList = when (_activeTab.value) {
                LibraryTab.ALL -> _allSongs.value
                LibraryTab.LIKED -> _likedSongs.value
            }

            // Find the current song index
            val currentIndex = currentList.indexOfFirst { it.id == currentSongId }
            if (currentIndex == -1) return@launch

            // Get the previous song (or wrap around to the last if at the beginning)
            val previousIndex = if (currentIndex > 0) currentIndex - 1 else currentList.size - 1
            val previousSong = currentList[previousIndex]

            // Play the previous song
            playSong(previousSong)
        }
    }
}


enum class LibraryTab {
    ALL, LIKED
}

sealed class LibraryUiState {
    object Loading : LibraryUiState()
    object Empty : LibraryUiState()
    data class Success(val songs: List<Song>) : LibraryUiState()
    data class Error(val message: String) : LibraryUiState()
}