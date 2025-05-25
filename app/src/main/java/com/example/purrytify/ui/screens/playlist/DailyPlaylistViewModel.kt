package com.example.purrytify.ui.screens.playlist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.local.datastore.UserPreferences
import com.example.purrytify.data.repository.SongRepository
import com.example.purrytify.data.repository.TopSongsRepository
import com.example.purrytify.domain.model.PlaylistItem
import com.example.purrytify.domain.player.PlaybackContext
import com.example.purrytify.domain.player.PlayerBridge
import com.example.purrytify.domain.util.Result
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
    private val gson = Gson()
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    // UI state
    private val _uiState = MutableStateFlow<PlaylistUiState>(PlaylistUiState.Loading)
    val uiState = _uiState.asStateFlow()

    // Currently playing item
    private val _currentPlayingItem = MutableStateFlow<PlaylistItem?>(null)
    val currentPlayingItem = _currentPlayingItem.asStateFlow()

    // Whether we have cached content
    private val _hasCache = MutableStateFlow(false)
    val hasCache = _hasCache.asStateFlow()

    // User ID
    private val _userId = MutableStateFlow<Int?>(null)

    init {
        // Check for cached content first
        checkForCache()
        
        // Get user ID
        viewModelScope.launch {
            userPreferences.userId.collect { userId ->
                if (userId != null) {
                    _userId.value = userId
                    // Load playlist only on first initialization
                    loadDailyPlaylistIfNeeded()
                } else {
                    _uiState.value = PlaylistUiState.Error("User not found")
                }
            }
        }

        // Monitor current playing item from player bridge
        viewModelScope.launch {
            playerBridge.currentItem.collect { currentItem ->
                _currentPlayingItem.value = currentItem
            }
        }
    }

    /**
     * Check if we have cached playlist content
     */
    private fun checkForCache() {
        viewModelScope.launch {
            try {
                val cachedJson = userPreferences.dailyPlaylistJson.firstOrNull()
                _hasCache.value = !cachedJson.isNullOrBlank()
                Log.d(TAG, "Has cache: ${_hasCache.value}")
            } catch (e: Exception) {
                Log.e(TAG, "Error checking cache: ${e.message}", e)
                _hasCache.value = false
            }
        }
    }

    /**
     * Load daily playlist only if needed (not generated today)
     */
    private suspend fun loadDailyPlaylistIfNeeded() {
        val today = LocalDate.now().format(dateFormatter)
        val lastGeneratedDate = userPreferences.dailyPlaylistDate.firstOrNull()

        Log.d(TAG, "Today: $today, Last generated: $lastGeneratedDate")

        if (lastGeneratedDate == today) {
            // Playlist was already generated today, load from cache
            loadCachedPlaylist()
        } else {
            // Generate new playlist for today
            generateNewPlaylist()
        }
    }

    /**
     * Load cached playlist from preferences
     */
    private suspend fun loadCachedPlaylist() {
        try {
            val cachedJson = userPreferences.dailyPlaylistJson.firstOrNull()

            if (cachedJson != null) {
                Log.d(TAG, "Loading cached playlist")
                _uiState.value = PlaylistUiState.Loading

                val playlistType = object : TypeToken<List<SerializablePlaylistItem>>() {}.type
                val cachedItems: List<SerializablePlaylistItem> = gson.fromJson(cachedJson, playlistType)

                // Convert back to PlaylistItem and validate
                val validItems = mutableListOf<PlaylistItem>()

                for (item in cachedItems) {
                    try {
                        val playlistItem = item.toPlaylistItem()
                        validItems.add(playlistItem)
                    } catch (e: Exception) {
                        Log.w(TAG, "Skipping invalid cached item: ${e.message}")
                    }
                }

                if (validItems.isNotEmpty()) {
                    _uiState.value = PlaylistUiState.Success(validItems)
                    Log.d(TAG, "Loaded ${validItems.size} items from cache")
                } else {
                    // Cache is empty or invalid, generate new
                    Log.w(TAG, "Cached playlist is empty or invalid, generating new")
                    generateNewPlaylist()
                }
            } else {
                // No cache available, generate new
                Log.d(TAG, "No cached playlist found, generating new")
                generateNewPlaylist()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading cached playlist: ${e.message}", e)
            generateNewPlaylist()
        }
    }

    /**
     * Generate and cache a new daily playlist
     */
    private suspend fun generateNewPlaylist() {
        try {
            Log.d(TAG, "Generating new daily playlist")
            _uiState.value = PlaylistUiState.Loading

            // Get user ID
            val userId = _userId.value ?: run {
                _uiState.value = PlaylistUiState.Error("User not found")
                return
            }

            // Get recommended local songs
            val localSongs = getRecommendedLocalSongs(userId)

            // Get online songs
            val onlineSongs = getRandomOnlineSongs()

            // Mix and shuffle the songs
            val mixedItems = (localSongs + onlineSongs).shuffled()

            if (mixedItems.isEmpty()) {
                _uiState.value = PlaylistUiState.Empty
            } else {
                // Cache the playlist
                cachePlaylist(mixedItems)

                _uiState.value = PlaylistUiState.Success(mixedItems)
                Log.d(TAG, "Generated new playlist with ${mixedItems.size} items")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating daily playlist: ${e.message}", e)
            _uiState.value = PlaylistUiState.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Cache the playlist to preferences
     */
    private suspend fun cachePlaylist(items: List<PlaylistItem>) {
        try {
            val today = LocalDate.now().format(dateFormatter)
            val serializableItems = items.map { SerializablePlaylistItem.fromPlaylistItem(it) }
            val playlistJson = gson.toJson(serializableItems)

            userPreferences.saveDailyPlaylistInfo(today, playlistJson)
            _hasCache.value = true // Update cache status
            Log.d(TAG, "Cached playlist for $today")
        } catch (e: Exception) {
            Log.e(TAG, "Error caching playlist: ${e.message}", e)
        }
    }

    /**
     * Load the daily playlist (force regeneration)
     */
    fun loadDailyPlaylist() {
        viewModelScope.launch {
            generateNewPlaylist()
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
        Log.d(TAG, "Playing item from daily playlist: ${item.title}")

        // Get the current playlist items from UI state for the queue
        val currentItems = when (val state = _uiState.value) {
            is PlaylistUiState.Success -> state.items
            else -> listOf(item) // Fallback to single song if no playlist available
        }

        // Find the index of the clicked item in the current playlist
        val startIndex = currentItems.indexOfFirst { it.id == item.id }

        if (startIndex >= 0) {
            // Set up the queue with the full daily playlist
            playerBridge.playQueue(currentItems, startIndex, PlaybackContext.Recommendation)
            _currentPlayingItem.value = item

            Log.d(TAG, "Set up daily playlist queue with ${currentItems.size} items, starting at index $startIndex")

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
        } else {
            // Fallback: play single item if not found in playlist
            Log.w(TAG, "Item not found in current playlist, playing single item")
            playerBridge.playItem(item)
            _currentPlayingItem.value = item
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
 * Serializable version of PlaylistItem for caching
 */
data class SerializablePlaylistItem(
    val id: String,
    val title: String,
    val artist: String,
    val duration: String,
    val type: String, // "local" or "online"
    // Local song fields
    val artworkPath: String? = null,
    val filePath: String? = null,
    val durationMs: Long? = null,
    val isLiked: Boolean? = null,
    // Online song fields
    val artworkUrl: String? = null,
    val songUrl: String? = null,
    val originalId: String? = null
) {
    fun toPlaylistItem(): PlaylistItem {
        return when (type) {
            "local" -> PlaylistItem.LocalSong(
                id = id,
                title = title,
                artist = artist,
                artworkPath = artworkPath ?: "",
                filePath = filePath ?: "",
                durationMs = durationMs ?: 0L,
                duration = duration,
                isLiked = isLiked ?: false
            )
            "online" -> PlaylistItem.OnlineSong(
                id = id,
                title = title,
                artist = artist,
                artworkUrl = artworkUrl ?: "",
                songUrl = songUrl ?: "",
                duration = duration,
                originalId = originalId ?: ""
            )
            else -> throw IllegalArgumentException("Unknown playlist item type: $type")
        }
    }

    companion object {
        fun fromPlaylistItem(item: PlaylistItem): SerializablePlaylistItem {
            return when (item) {
                is PlaylistItem.LocalSong -> SerializablePlaylistItem(
                    id = item.id,
                    title = item.title,
                    artist = item.artist,
                    duration = item.duration,
                    type = "local",
                    artworkPath = item.artworkPath,
                    filePath = item.filePath,
                    durationMs = item.durationMs,
                    isLiked = item.isLiked
                )
                is PlaylistItem.OnlineSong -> SerializablePlaylistItem(
                    id = item.id,
                    title = item.title,
                    artist = item.artist,
                    duration = item.duration,
                    type = "online",
                    artworkUrl = item.artworkUrl,
                    songUrl = item.songUrl,
                    originalId = item.originalId
                )
            }
        }
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