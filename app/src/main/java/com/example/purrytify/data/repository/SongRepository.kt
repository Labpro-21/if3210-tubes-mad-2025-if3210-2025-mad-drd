package com.example.purrytify.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.purrytify.data.local.dao.SongDao
import com.example.purrytify.data.local.entity.SongEntity
import com.example.purrytify.domain.model.Song
import com.example.purrytify.domain.util.Result
import com.example.purrytify.util.AudioUtils
import com.example.purrytify.util.FileUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for song operations
 */
@Singleton
class SongRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songDao: SongDao
) {
    
    private val TAG = "SongRepository"
    
    fun getAllSongs(userId: Int): Flow<List<Song>> {
        return songDao.getAllSongs(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getLikedSongs(userId: Int): Flow<List<Song>> {
        return songDao.getLikedSongs(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getDownloadedSongs(userId: Int): Flow<List<Song>> {
        return songDao.getDownloadedSongs(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun addSong(
        userId: Int,
        title: String,
        artist: String,
        audioUri: Uri,
        artworkUri: Uri?
    ): Result<Song> = withContext(Dispatchers.IO) {
        try {
            // Generate a unique ID for the song
            val songId = UUID.randomUUID().toString()
            val now = LocalDateTime.now()
            
            // Save the audio file to the app's files directory
            val audioFileName = "${songId}_audio.mp3"
            val audioFile = FileUtils.saveUriToFile(context, audioUri, audioFileName)
                ?: return@withContext Result.Error(Exception("Failed to save audio file"))
            
            // Save the artwork if provided
            val artworkPath = if (artworkUri != null) {
                val artworkFileName = "${songId}_artwork.jpg"
                val artworkFile = FileUtils.saveUriToFile(context, artworkUri, artworkFileName)
                artworkFile?.absolutePath ?: ""
            } else {
                "" // No artwork
            }
            
            // Get the duration of the audio file
            val duration = AudioUtils.getAudioDuration(context, audioUri)
            
            // Create a song entity
            val songEntity = SongEntity(
                id = songId,
                title = title,
                artist = artist,
                artworkPath = artworkPath,
                filePath = audioFile.absolutePath,
                duration = duration,
                userId = userId,
                createdAt = now,
                updatedAt = now
            )
            
            // Insert the song into the database
            songDao.insertSong(songEntity)
            
            // Return the domain model
            Result.Success(songEntity.toDomain())
        } catch (e: Exception) {
            Log.e(TAG, "Error adding song: ${e.message}", e)
            Result.Error(e, "Failed to add song: ${e.localizedMessage}")
        }
    }

    suspend fun updateSong(
        songId: String,
        title: String,
        artist: String,
        artworkUri: Uri?
    ): Result<Song> = withContext(Dispatchers.IO) {
        try {
            // Get the existing song
            val existingSong = songDao.getSongById(songId)
                ?: return@withContext Result.Error(Exception("Song not found"))
            
            // Update the artwork if provided
            val artworkPath = if (artworkUri != null) {
                val artworkFileName = "${songId}_artwork.jpg"
                val artworkFile = FileUtils.saveUriToFile(context, artworkUri, artworkFileName)
                artworkFile?.absolutePath ?: existingSong.artworkPath
            } else {
                existingSong.artworkPath
            }
            
            // Create updated entity
            val updatedSong = existingSong.copy(
                title = title,
                artist = artist,
                artworkPath = artworkPath,
                updatedAt = LocalDateTime.now()
            )
            
            // Update in database
            songDao.updateSong(updatedSong)
            
            // Return the domain model
            Result.Success(updatedSong.toDomain())
        } catch (e: Exception) {
            Log.e(TAG, "Error updating song: ${e.message}", e)
            Result.Error(e, "Failed to update song: ${e.localizedMessage}")
        }
    }

    suspend fun deleteSong(songId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Get the existing song
            val existingSong = songDao.getSongById(songId)
                ?: return@withContext Result.Error(Exception("Song not found"))
            
            // Delete the song from the database
            songDao.deleteSong(existingSong)
            
            // Delete the audio file
            FileUtils.deleteFile(existingSong.filePath)
            
            // Delete the artwork if it exists
            if (existingSong.artworkPath.isNotEmpty()) {
                FileUtils.deleteFile(existingSong.artworkPath)
            }
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting song: ${e.message}", e)
            Result.Error(e, "Failed to delete song: ${e.localizedMessage}")
        }
    }

    suspend fun toggleLike(songId: String, userId: Int, isLiked: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val now = LocalDateTime.now()
            val likedAt = if (isLiked) now else null
            
            songDao.updateLikeStatus(songId, userId, likedAt, now)
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling like: ${e.message}", e)
            Result.Error(e, "Failed to update like status: ${e.localizedMessage}")
        }
    }

    suspend fun updateLastPlayed(songId: String, userId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val now = LocalDateTime.now()
            
            songDao.updateLastPlayed(songId, userId, now, now)
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating last played: ${e.message}", e)
            Result.Error(e, "Failed to update last played: ${e.localizedMessage}")
        }
    }

    suspend fun getSongById(songId: String): Result<Song> = withContext(Dispatchers.IO) {
        try {
            val songEntity = songDao.getSongById(songId)
                ?: return@withContext Result.Error(Exception("Song not found"))
                
            Result.Success(songEntity.toDomain())
        } catch (e: Exception) {
            Log.e(TAG, "Error getting song: ${e.message}", e)
            Result.Error(e, "Failed to get song: ${e.localizedMessage}")
        }
    }
    
    /**
     * Extension function to convert SongEntity to Song domain model
     */
    private fun SongEntity.toDomain(): Song {
        return Song(
            id = id,
            title = title,
            artist = artist,
            artworkPath = artworkPath,
            filePath = filePath,
            duration = duration,
            userId = userId,
            isLiked = likedAt != null,
            likedAt = likedAt,
            lastPlayedAt = lastPlayedAt,
            downloadedAt = downloadedAt,
            originalId = originalId,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}