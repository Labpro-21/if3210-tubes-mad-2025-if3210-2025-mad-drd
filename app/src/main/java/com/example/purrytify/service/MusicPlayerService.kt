package com.example.purrytify.service

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.example.purrytify.domain.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException

class MusicPlayerService : Service() {
    // Media Player instance
    private var mediaPlayer: MediaPlayer? = null
    
    // Binder for client communication
    private val binder = MusicPlayerBinder()
    
    // Playback state flows
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress
    
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong
    
    // Add callback for song completion
    var onSongCompletion: (() -> Unit)? = null
    
    // Coroutine for position updates
    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var positionUpdateJob: Job? = null
    
    inner class MusicPlayerBinder : Binder() {
        fun getService(): MusicPlayerService = this@MusicPlayerService
    }
    
    override fun onBind(intent: Intent): IBinder {
        return binder
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "MusicPlayerService created")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        releaseMediaPlayer()
        positionUpdateJob?.cancel()
        Log.d(TAG, "MusicPlayerService destroyed")
    }
    
    fun playSong(song: Song) {
        // If a song is already playing, stop it
        if (mediaPlayer != null) {
            stopPlayback()
        }
        
        // Create a new MediaPlayer
        mediaPlayer = MediaPlayer().apply {
            try {
                // Set the data source
                setDataSource(applicationContext, song.fileUri)
                
                // Prepare the MediaPlayer
                prepare()
                
                // Start playback
                start()
                
                // Update state
                _isPlaying.value = true
                _currentSong.value = song
                
                // Start position updates
                startPositionUpdates()
                
                // Set completion listener
                setOnCompletionListener {
                    _isPlaying.value = false
                    _progress.value = 1f  // Set to end
                    onSongCompletion?.invoke()  // Trigger the completion callback
                    Log.d(TAG, "Song completed, calling onSongCompletion")
                }
                
                Log.d(TAG, "Started playing: ${song.title}")
            } catch (e: IOException) {
                Log.e(TAG, "Error setting data source: ${e.message}")
                releaseMediaPlayer()
            } catch (e: IllegalStateException) {
                Log.e(TAG, "MediaPlayer in illegal state: ${e.message}")
                releaseMediaPlayer()
            }
        }
    }
    
    fun togglePlayPause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
                Log.d(TAG, "Paused playback")
            } else {
                it.start()
                _isPlaying.value = true
                Log.d(TAG, "Resumed playback")
            }
        }
    }
    
    fun stopPlayback() {
        mediaPlayer?.stop()
        releaseMediaPlayer()
        _isPlaying.value = false
        _progress.value = 0f
        _currentPosition.value = 0L
        positionUpdateJob?.cancel()
        Log.d(TAG, "Stopped playback")
    }
    
    fun seekTo(position: Long) {
        mediaPlayer?.seekTo(position.toInt())
        _currentPosition.value = position
        updateProgress()
        Log.d(TAG, "Seeked to position: $position")
    }
    
    fun seekToProgress(newProgress: Float) {
        mediaPlayer?.let {
            val duration = it.duration.toLong()
            val newPosition = (duration * newProgress).toLong()
            seekTo(newPosition)
        }
    }
    
    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = serviceScope.launch {
            while (isActive) {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        _currentPosition.value = it.currentPosition.toLong()
                        updateProgress()
                    }
                }
                delay(100) // Update every 100ms
            }
        }
    }
    
    private fun updateProgress() {
        mediaPlayer?.let {
            val duration = it.duration.toLong()
            val position = it.currentPosition.toLong()
            _progress.value = if (duration > 0) position.toFloat() / duration.toFloat() else 0f
        }
    }
    
    private fun releaseMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
    
    companion object {
        private const val TAG = "MusicPlayerService"
    }
}