package com.example.purrytify.data.repository

import android.content.Context
import android.net.Uri
import com.example.purrytify.data.local.dao.SongDao
import com.example.purrytify.data.local.entity.LikedSongEntity
import com.example.purrytify.data.local.entity.SongEntity
import com.example.purrytify.domain.model.Song
import com.example.purrytify.domain.model.toDomain
import com.example.purrytify.domain.model.toEntity
import com.example.purrytify.util.copyToAppStorage
import com.example.purrytify.util.getAudioDuration
import com.example.purrytify.util.getAudioMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SongRepository @Inject constructor(
    private val songDao: SongDao,
    @ApplicationContext private val context: Context
) {
    // Get all songs
    fun getAllSongs(): Flow<List<Song>> {
        return songDao.getSongsWithLikeStatus().map { songs ->
            songs.map { it.toDomain() }
        }
    }
    
    // Get song by ID
    fun getSongById(songId: Long): Flow<Song?> {
        return songDao.getSongWithLikeStatus(songId).map { it?.toDomain() }
    }
    
    // Get recently played songs
    fun getRecentlyPlayedSongs(): Flow<List<Song>> {
        return songDao.getRecentlyPlayedSongs().map { songs ->
            songs.map { it.toDomain() }
        }
    }
    
    // Get liked songs
    fun getLikedSongs(): Flow<List<Song>> {
        return songDao.getLikedSongs().map { songs ->
            songs.map { it.toDomain(true) }
        }
    }
    
    // Add a new song
    suspend fun addSong(
        audioUri: Uri,
        title: String,
        artist: String,
        artworkUri: Uri?
    ): Long? {
        // Copy audio file to app storage
        val filePath = audioUri.copyToAppStorage(context) ?: return null
        
        // Get audio metadata for duration
        val duration = audioUri.getAudioDuration(context)
        
        // Copy artwork to app storage if provided
        val artworkPath = artworkUri?.let {
            val fileName = "artwork_${System.currentTimeMillis()}.jpg"
            context.contentResolver.openInputStream(it)?.use { input ->
                context.openFileOutput(fileName, Context.MODE_PRIVATE).use { output ->
                    input.copyTo(output)
                }
            }
            "${context.filesDir}/$fileName"
        }
        
        // Create song entity
        val songEntity = SongEntity(
            title = title,
            artist = artist,
            filePath = filePath,
            artworkPath = artworkPath,
            duration = duration
        )
        
        // Insert to database
        return songDao.insertSong(songEntity)
    }
    
    // Update song
    suspend fun updateSong(song: Song) {
        songDao.updateSong(song.toEntity())
    }
    
    // Delete song
    suspend fun deleteSong(song: Song) {
        songDao.deleteSong(song.toEntity())
    }
    
    // Mark song as played
    suspend fun markSongAsPlayed(songId: Long) {
        songDao.updateLastPlayedTime(songId, System.currentTimeMillis())
    }
    
    // Like/Unlike song
    suspend fun toggleLikeSong(songId: Long, isLiked: Boolean) {
        if (isLiked) {
            songDao.likeSong(LikedSongEntity(songId))
        } else {
            songDao.unlikeSong(songId)
        }
    }
    
    // Check if song is liked
    fun isSongLiked(songId: Long): Flow<Boolean> {
        return songDao.isSongLiked(songId)
    }
    
    // Extract metadata from audio file
    suspend fun extractMetadata(audioUri: Uri): Map<String, String> {
        return audioUri.getAudioMetadata(context)
    }
}