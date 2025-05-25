package com.example.purrytify.util

import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.purrytify.data.repository.SongDetailRepository
import com.example.purrytify.domain.model.PlaylistItem
import com.example.purrytify.domain.player.PlaybackContext
import com.example.purrytify.domain.player.PlayerBridge
import com.example.purrytify.domain.util.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles deep link processing and navigation
 */
@Singleton
class DeepLinkHandler @Inject constructor(
    private val songDetailRepository: SongDetailRepository,
    private val playerBridge: PlayerBridge,
    private val externalScope: CoroutineScope
) {
    private val TAG = "DeepLinkHandler"
    
    /**
     * Handle an incoming deep link intent
     * @param intent The intent containing the deep link
     * @param onNavigateToPlayer Callback to navigate to the player screen
     * @param onShowError Callback to show error messages
     */
    fun handleDeepLink(
        intent: Intent,
        onNavigateToPlayer: (String) -> Unit,
        onShowError: (String) -> Unit
    ) {
        val data = intent.data
        if (data == null) {
            Log.w(TAG, "Deep link intent has no data")
            return
        }
        
        Log.d(TAG, "Handling deep link: $data")
        
        when {
            isSongDeepLink(data) -> {
                handleSongDeepLink(data, onNavigateToPlayer, onShowError)
            }
            else -> {
                Log.w(TAG, "Unknown deep link format: $data")
                onShowError("Invalid link format")
            }
        }
    }
    
    /**
     * Check if the URI is a song deep link
     */
    private fun isSongDeepLink(uri: Uri): Boolean {
        return uri.scheme == "purrytify" && uri.host == "song"
    }
    
    /**
     * Handle song deep link (purrytify://song/<song_id>)
     */
    private fun handleSongDeepLink(
        uri: Uri,
        onNavigateToPlayer: (String) -> Unit,
        onShowError: (String) -> Unit
    ) {
        val songId = extractSongIdFromUri(uri)
        if (songId == null) {
            Log.e(TAG, "Failed to extract song ID from URI: $uri")
            onShowError("Invalid song link")
            return
        }
        
        Log.d(TAG, "Processing song deep link for song ID: $songId")
        
        // Fetch song details and start playing
        externalScope.launch {
            try {
                when (val result = songDetailRepository.getSongById(songId)) {
                    is Result.Success -> {
                        val song = result.data
                        Log.d(TAG, "Successfully fetched song: ${song.title} by ${song.artist}")
                        
                        // Create playlist item and start playing
                        val playlistItem = PlaylistItem.fromOnlineSong(song)
                        playerBridge.playQueue(
                            queue = listOf(playlistItem),
                            startIndex = 0,
                            context = PlaybackContext.TopSongs
                        )
                        
                        // Navigate to player screen
                        onNavigateToPlayer(songId)
                        
                        Log.d(TAG, "Started playing song from deep link")
                    }
                    is Result.Error -> {
                        Log.e(TAG, "Failed to fetch song details: ${result.message}")
                        when {
                            result.message.contains("not found", ignoreCase = true) -> {
                                onShowError("Song not found")
                            }
                            result.message.contains("network", ignoreCase = true) || 
                            result.message.contains("connection", ignoreCase = true) -> {
                                onShowError("No internet connection")
                            }
                            else -> {
                                onShowError("Failed to load song")
                            }
                        }
                    }
                    is Result.Loading -> {
                        // This shouldn't happen in our current implementation
                        Log.d(TAG, "Loading song details...")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception handling song deep link: ${e.message}", e)
                onShowError("Failed to open song")
            }
        }
    }
    
    /**
     * Extract song ID from URI
     */
    private fun extractSongIdFromUri(uri: Uri): String? {
        return try {
            // For purrytify://song/<song_id>, the song ID is the first path segment
            val pathSegments = uri.pathSegments
            if (pathSegments.isNotEmpty()) {
                pathSegments[0]
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting song ID from URI: ${e.message}", e)
            null
        }
    }
    
    /**
     * Check if an intent contains a deep link
     */
    fun hasDeepLink(intent: Intent): Boolean {
        return intent.data != null && 
               intent.action == Intent.ACTION_VIEW &&
               isSongDeepLink(intent.data!!)
    }
    
    /**
     * Validate if a deep link is properly formatted
     */
    fun isValidDeepLink(uriString: String): Boolean {
        return try {
            val uri = Uri.parse(uriString)
            isSongDeepLink(uri) && extractSongIdFromUri(uri) != null
        } catch (e: Exception) {
            false
        }
    }
}