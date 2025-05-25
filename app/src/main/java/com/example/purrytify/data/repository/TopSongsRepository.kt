package com.example.purrytify.data.repository

import android.content.Context
import com.example.purrytify.data.remote.api.TopSongsApi
import com.example.purrytify.data.remote.dto.response.TopSongDto
import com.example.purrytify.domain.model.OnlineSong
import com.example.purrytify.domain.util.Result
import com.example.purrytify.util.FileUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for top songs operations
 */
@Singleton
class TopSongsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val topSongsApi: TopSongsApi,
    private val songRepository: SongRepository
) {
    
    private val TAG = "TopSongsRepository"
    
    /**
     * Get global top 50 songs
     */
    suspend fun getGlobalTopSongs(): Result<List<OnlineSong>> = withContext(Dispatchers.IO) {
        try {
            val response = topSongsApi.getGlobalTopSongs()
            
            if (response.isSuccessful) {
                val songs = response.body() ?: emptyList()
                Result.Success(songs.map { it.toDomain() })
            } else {
                Result.Error(Exception("Failed to fetch global top songs: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.Error(e, "Error fetching global top songs: ${e.message}")
        }
    }
    
    /**
     * Get top 10 songs by country
     */
    suspend fun getCountryTopSongs(countryCode: String): Result<List<OnlineSong>> = withContext(Dispatchers.IO) {
        try {
            val response = topSongsApi.getCountryTopSongs(countryCode)
            
            if (response.isSuccessful) {
                val songs = response.body() ?: emptyList()
                Result.Success(songs.map { it.toDomain() })
            } else {
                Result.Error(Exception("Failed to fetch country top songs: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.Error(e, "Error fetching country top songs: ${e.message}")
        }
    }
    
    /**
     * Download an online song to local storage
     * @param song Online song to download
     * @param userId Current user ID
     */
    suspend fun downloadSong(song: OnlineSong, userId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Check if song is already downloaded
            if (songRepository.isOnlineSongDownloaded(song.id.toString(), userId)) {
                return@withContext Result.Success(Unit) // Already downloaded
            }
            
            // Generate unique filenames
            val songId = song.id.toString()
            val audioFileName = "${songId}_audio.mp3"
            val artworkFileName = "${songId}_artwork.jpg"
            
            // Create destination files
            val audioFile = File(context.filesDir, audioFileName)
            val artworkFile = File(context.filesDir, artworkFileName)
            
            // Download audio file
            val audioUrl = URL(song.songUrl)
            val audioConnection = audioUrl.openConnection()
            audioConnection.connect()
            
            audioConnection.getInputStream().use { input ->
                audioFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            // Download artwork file
            val artworkUrl = URL(song.artworkUrl)
            val artworkConnection = artworkUrl.openConnection()
            artworkConnection.connect()
            
            artworkConnection.getInputStream().use { input ->
                artworkFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            // Parse duration string to milliseconds
            val durationParts = song.duration.split(":")
            val minutes = durationParts[0].toLongOrNull() ?: 0
            val seconds = durationParts[1].toLongOrNull() ?: 0
            val durationMs = (minutes * 60 + seconds) * 1000
            
            // Save to database
            val result = songRepository.addDownloadedSong(
                userId = userId,
                title = song.title,
                artist = song.artist,
                audioFile = audioFile.absolutePath,
                artworkFile = artworkFile.absolutePath,
                duration = durationMs,
                originalId = song.id.toString()
            )
            
            when (result) {
                is Result.Success -> Result.Success(Unit)
                is Result.Error -> {
                    // Clean up files if database insert failed
                    FileUtils.deleteFile(audioFile.absolutePath)
                    FileUtils.deleteFile(artworkFile.absolutePath)
                    result
                }
                is Result.Loading -> Result.Loading
            }
        } catch (e: IOException) {
            // Network or file I/O error
            Result.Error(e, "Network error downloading song: ${e.message}")
        } catch (e: Exception) {
            // Other errors
            Result.Error(e, "Error downloading song: ${e.message}")
        }
    }
    
    /**
     * Download all songs from a list
     * @param songs List of online songs to download
     * @param userId Current user ID
     * @param progressCallback Callback for download progress (current, total)
     */
    suspend fun downloadAllSongs(
        songs: List<OnlineSong>, 
        userId: Int,
        progressCallback: (Int, Int) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val total = songs.size
            
            songs.forEachIndexed { index, song ->
                progressCallback(index + 1, total)
                
                // Download the song
                when (val result = downloadSong(song, userId)) {
                    is Result.Error -> {
                        // If there's an error, report it but continue downloading others
                        android.util.Log.e(TAG, "Error downloading song ${song.title}: ${result.message}")
                    }
                    else -> { /* Continue downloading */ }
                }
            }
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Error downloading songs: ${e.message}")
        }
    }
    
    /**
     * Convert DTO to domain model
     */
    private fun TopSongDto.toDomain(): OnlineSong {
        return OnlineSong(
            id = id,
            title = title,
            artist = artist,
            artworkUrl = artwork,
            songUrl = url,
            duration = duration,
            country = country,
            rank = rank,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}