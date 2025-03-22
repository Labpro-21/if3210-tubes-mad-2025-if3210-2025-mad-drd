package com.example.purrytify.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.purrytify.data.local.entity.LikedSongEntity
import com.example.purrytify.data.local.entity.SongEntity
import com.example.purrytify.data.local.entity.SongWithLikeStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity): Long

    @Update
    suspend fun updateSong(song: SongEntity)

    @Delete
    suspend fun deleteSong(song: SongEntity)

    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :songId")
    suspend fun getSongById(songId: Long): SongEntity?

    @Query("SELECT * FROM songs WHERE id = :songId")
    fun getSongByIdFlow(songId: Long): Flow<SongEntity?>

    // Recently played songs
    @Query("SELECT * FROM songs ORDER BY lastPlayedAt DESC LIMIT 10")
    fun getRecentlyPlayedSongs(): Flow<List<SongEntity>>

    // Mark song as played
    @Query("UPDATE songs SET lastPlayedAt = :timestamp WHERE id = :songId")
    suspend fun updateLastPlayedTime(songId: Long, timestamp: Long)

    // Liked Songs
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun likeSong(likedSong: LikedSongEntity)

    @Query("DELETE FROM liked_songs WHERE songId = :songId")
    suspend fun unlikeSong(songId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM liked_songs WHERE songId = :songId)")
    fun isSongLiked(songId: Long): Flow<Boolean>

    @Query("SELECT s.* FROM songs s INNER JOIN liked_songs ls ON s.id = ls.songId ORDER BY s.title ASC")
    fun getLikedSongs(): Flow<List<SongEntity>>

    @Transaction
    @Query("SELECT s.*, CASE WHEN ls.songId IS NOT NULL THEN 1 ELSE 0 END as is_liked FROM songs s LEFT JOIN liked_songs ls ON s.id = ls.songId ORDER BY s.title ASC")
    fun getSongsWithLikeStatus(): Flow<List<SongWithLikeStatus>>

    @Transaction
    @Query("SELECT s.*, CASE WHEN ls.songId IS NOT NULL THEN 1 ELSE 0 END as is_liked FROM songs s LEFT JOIN liked_songs ls ON s.id = ls.songId WHERE s.id = :songId")
    fun getSongWithLikeStatus(songId: Long): Flow<SongWithLikeStatus?>
}