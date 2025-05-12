package com.example.purrytify.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.purrytify.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * Data Access Object for Song entity
 */
@Dao
interface SongDao {
    // Basic CRUD operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity): Long

    @Update
    suspend fun updateSong(song: SongEntity)

    @Delete
    suspend fun deleteSong(song: SongEntity)

    // Queries
    @Query("SELECT * FROM song WHERE id = :id")
    suspend fun getSongById(id: String): SongEntity?

    @Query("SELECT * FROM song WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAllSongs(userId: Int): Flow<List<SongEntity>>

    // Liked songs
    @Query("SELECT * FROM song WHERE userId = :userId AND likedAt IS NOT NULL ORDER BY likedAt DESC")
    fun getLikedSongs(userId: Int): Flow<List<SongEntity>>

    @Query("UPDATE song SET likedAt = :likedAt, updatedAt = :updatedAt WHERE id = :songId AND userId = :userId")
    suspend fun updateLikeStatus(songId: String, userId: Int, likedAt: LocalDateTime?, updatedAt: LocalDateTime)

    // Recently played
    @Query("SELECT * FROM song WHERE userId = :userId AND lastPlayedAt IS NOT NULL ORDER BY lastPlayedAt DESC LIMIT :limit")
    fun getRecentlyPlayedSongs(userId: Int, limit: Int = 10): Flow<List<SongEntity>>

    @Query("UPDATE song SET lastPlayedAt = :playedAt, updatedAt = :updatedAt WHERE id = :songId AND userId = :userId")
    suspend fun updateLastPlayed(songId: String, userId: Int, playedAt: LocalDateTime, updatedAt: LocalDateTime)

    // Downloaded songs
    @Query("SELECT * FROM song WHERE userId = :userId AND downloadedAt IS NOT NULL ORDER BY downloadedAt DESC")
    fun getDownloadedSongs(userId: Int): Flow<List<SongEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM song WHERE originalId = :originalId AND userId = :userId LIMIT 1)")
    suspend fun isOnlineSongDownloaded(originalId: String, userId: Int): Boolean

    // Analytics
    @Query("SELECT * FROM song WHERE userId = :userId ORDER BY lastPlayedAt DESC LIMIT 1")
    suspend fun getMostRecentlyPlayedSong(userId: Int): SongEntity?

    @Query("SELECT COUNT(*) FROM song WHERE userId = :userId")
    suspend fun getSongCount(userId: Int): Int

    @Query("SELECT COUNT(*) FROM song WHERE userId = :userId AND likedAt IS NOT NULL")
    suspend fun getLikedSongCount(userId: Int): Int
}