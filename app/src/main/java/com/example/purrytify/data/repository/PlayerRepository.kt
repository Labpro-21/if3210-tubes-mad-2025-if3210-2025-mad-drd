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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for handling actual media playback using ExoPlayer
 */
@Singleton
class PlayerRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "PlayerRepository"
    
    private var exoPlayer: ExoPlayer? = null
    
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
    
    // Player listener
    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            _isBuffering.value = playbackState == Player.STATE_BUFFERING
            
            when (playbackState) {
                Player.STATE_ENDED -> {
                    onPlaybackEnded?.invoke()
                }
                Player.STATE_READY -> {
                    _duration.value = exoPlayer?.duration ?: 0L
                }
            }
        }
        
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }
        
        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "Player error: ${error.message}", error)
            _playbackError.value = error.message ?: "Playback error occurred"
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
            
            // Start position tracking
            startPositionTracking()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing player: ${e.message}", e)
        }
    }
    
    private fun startPositionTracking() {
        // This would typically be done with a coroutine or handler
        // For now, we'll rely on the UI to update positions
    }
    
    /**
     * Prepare and play a playlist item
     */
    fun playItem(item: PlaylistItem) {
        try {
            val mediaItem = when (item) {
                is PlaylistItem.LocalSong -> {
                    MediaItem.fromUri(Uri.parse("file://${item.filePath}"))
                }
                is PlaylistItem.OnlineSong -> {
                    MediaItem.fromUri(Uri.parse(item.songUrl))
                }
            }
            
            exoPlayer?.apply {
                setMediaItem(mediaItem)
                prepare()
                play()
            }
            
            _playbackError.value = null
            Log.d(TAG, "Playing: ${item.title} by ${item.artist}")
        } catch (e: Exception) {
            Log.e(TAG, "Error playing item: ${e.message}", e)
            _playbackError.value = e.message ?: "Failed to play song"
        }
    }
    
    /**
     * Play or resume playback
     */
    fun play() {
        exoPlayer?.play()
    }
    
    /**
     * Pause playback
     */
    fun pause() {
        exoPlayer?.pause()
    }
    
    /**
     * Toggle play/pause
     */
    fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                pause()
            } else {
                play()
            }
        }
    }
    
    /**
     * Seek to position
     */
    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }
    
    /**
     * Get current position
     */
    fun getCurrentPosition(): Long {
        return exoPlayer?.currentPosition ?: 0L
    }
    
    /**
     * Get duration
     */
    fun getDuration(): Long {
        return exoPlayer?.duration ?: 0L
    }
    
    /**
     * Stop playback and release resources
     */
    fun stop() {
        exoPlayer?.stop()
    }
    
    /**
     * Release the player
     */
    fun release() {
        exoPlayer?.removeListener(playerListener)
        exoPlayer?.release()
        exoPlayer = null
    }
    
    /**
     * Update current position (called by UI)
     */
    fun updateCurrentPosition() {
        val position = getCurrentPosition()
        _currentPosition.value = position
        onPositionChanged?.invoke(position)
    }
}