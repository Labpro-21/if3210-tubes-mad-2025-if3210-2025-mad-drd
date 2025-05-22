package com.example.purrytify.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.purrytify.domain.model.PlaylistItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for handling actual media playback using ExoPlayer
 */
@Singleton
class PlayerRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val externalScope: CoroutineScope
) {
    private val TAG = "PlayerRepository"
    
    private var exoPlayer: ExoPlayer? = null
    private var positionTrackingJob: Job? = null
    
    // Player state flows
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()
    
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition = _currentPosition.asStateFlow()
    
    private val _duration = MutableStateFlow(0L)
    val duration = _duration.asStateFlow()
    
    private val _isBuffering = MutableStateFlow(false)
    val isBuffering = _isBuffering.asStateFlow()
    
    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError = _playbackError.asStateFlow()
    
    // Current playing item
    private var _currentPlayingItem: PlaylistItem? = null
    
    // Player listener
    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            _isBuffering.value = playbackState == Player.STATE_BUFFERING
            
            when (playbackState) {
                Player.STATE_ENDED -> {
                    Log.d(TAG, "Playback ended")
                    _isPlaying.value = false
                    stopPositionTracking()
                    onPlaybackEnded?.invoke()
                }
                Player.STATE_READY -> {
                    _duration.value = exoPlayer?.duration ?: 0L
                    Log.d(TAG, "Playback ready, duration: ${_duration.value}ms")
                }
                Player.STATE_IDLE -> {
                    _isPlaying.value = false
                    stopPositionTracking()
                }
            }
        }
        
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            Log.d(TAG, "Is playing changed: $isPlaying")
            _isPlaying.value = isPlaying
            if (isPlaying) {
                startPositionTracking()
            } else {
                stopPositionTracking()
            }
        }
        
        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "Player error: ${error.message}", error)
            _playbackError.value = error.message ?: "Playback error occurred"
            _isPlaying.value = false
            stopPositionTracking()
            onPlaybackError?.invoke(error.message ?: "Unknown error")
        }
    }
    
    // Callbacks
    var onPlaybackEnded: (() -> Unit)? = null
    var onPlaybackError: ((String) -> Unit)? = null
    var onPositionChanged: ((Long) -> Unit)? = null
    
    init {
        initializePlayer()
    }
    
    private fun initializePlayer() {
        try {
            exoPlayer = ExoPlayer.Builder(context).build().apply {
                addListener(playerListener)
            }
            Log.d(TAG, "ExoPlayer initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing player: ${e.message}", e)
        }
    }
    
    private fun startPositionTracking() {
        stopPositionTracking() // Stop any existing tracking
        
        positionTrackingJob = externalScope.launch(Dispatchers.Main) {
            while (exoPlayer?.isPlaying == true) {
                try {
                    val position = getCurrentPosition()
                    _currentPosition.value = position
                    onPositionChanged?.invoke(position)
                    delay(100) // Update every 100ms
                } catch (e: Exception) {
                    Log.e(TAG, "Error in position tracking: ${e.message}")
                    break
                }
            }
        }
    }
    
    private fun stopPositionTracking() {
        positionTrackingJob?.cancel()
        positionTrackingJob = null
    }
    
    /**
     * Prepare and play a playlist item
     */
    fun playItem(item: PlaylistItem) {
        // Ensure ExoPlayer operations happen on main thread
        externalScope.launch(Dispatchers.Main) {
            try {
                Log.d(TAG, "Playing item: ${item.title} by ${item.artist}")
                
                val mediaItem = when (item) {
                    is PlaylistItem.LocalSong -> {
                        MediaItem.fromUri(Uri.parse("file://${item.filePath}"))
                    }
                    is PlaylistItem.OnlineSong -> {
                        MediaItem.fromUri(Uri.parse(item.songUrl))
                    }
                }
                
                _currentPlayingItem = item
                
                exoPlayer?.apply {
                    setMediaItem(mediaItem)
                    prepare()
                    play()
                }
                
                _playbackError.value = null
                Log.d(TAG, "Started playing: ${item.title}")
            } catch (e: Exception) {
                Log.e(TAG, "Error playing item: ${e.message}", e)
                _playbackError.value = e.message ?: "Failed to play song"
            }
        }
    }
    
    /**
     * Play or resume playbook
     */
    fun play() {
        externalScope.launch(Dispatchers.Main) {
            Log.d(TAG, "Play requested")
            exoPlayer?.play()
        }
    }
    
    /**
     * Pause playback
     */
    fun pause() {
        externalScope.launch(Dispatchers.Main) {
            Log.d(TAG, "Pause requested")
            exoPlayer?.pause()
        }
    }
    
    /**
     * Toggle play/pause
     */
    fun togglePlayPause() {
        externalScope.launch(Dispatchers.Main) {
            exoPlayer?.let { player ->
                if (player.isPlaying) {
                    Log.d(TAG, "Toggling to pause")
                    player.pause()
                } else {
                    Log.d(TAG, "Toggling to play")
                    player.play()
                }
            }
        }
    }
    
    /**
     * Seek to position
     */
    fun seekTo(positionMs: Long) {
        externalScope.launch(Dispatchers.Main) {
            Log.d(TAG, "Seeking to position: ${positionMs}ms")
            exoPlayer?.seekTo(positionMs)
        }
    }
    
    /**
     * Get current position
     */
    fun getCurrentPosition(): Long {
        return try {
            exoPlayer?.currentPosition ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current position: ${e.message}")
            0L
        }
    }
    
    /**
     * Get duration
     */
    fun getDuration(): Long {
        return try {
            exoPlayer?.duration ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "Error getting duration: ${e.message}")
            0L
        }
    }
    
    /**
     * Stop playback and release resources
     */
    fun stop() {
        externalScope.launch(Dispatchers.Main) {
            Log.d(TAG, "Stop requested")
            stopPositionTracking()
            exoPlayer?.stop()
            _currentPlayingItem = null
            _isPlaying.value = false
            _currentPosition.value = 0L
            _duration.value = 0L
        }
    }
    
    /**
     * Check if currently playing a specific item
     */
    fun isPlayingItem(itemId: String): Boolean {
        return try {
            _currentPlayingItem?.id == itemId && _isPlaying.value
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if item is playing: ${e.message}")
            false
        }
    }
    
    /**
     * Get currently playing item
     */
    fun getCurrentPlayingItem(): PlaylistItem? {
        return _currentPlayingItem
    }
    
    /**
     * Release the player
     */
    fun release() {
        externalScope.launch(Dispatchers.Main) {
            Log.d(TAG, "Releasing player")
            stopPositionTracking()
            exoPlayer?.removeListener(playerListener)
            exoPlayer?.release()
            exoPlayer = null
            _currentPlayingItem = null
        }
    }
}