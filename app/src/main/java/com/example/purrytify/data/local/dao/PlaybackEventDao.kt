package com.example.purrytify.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.purrytify.data.local.entity.PlaybackEventEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * Data Access Object for PlaybackEvent entity
 */
@Dao
interface PlaybackEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaybackEvent(event: PlaybackEventEntity): Long

    @Query("UPDATE playback_event SET endTime = :endTime, duration = :duration WHERE id = :id")
    suspend fun updatePlaybackEndTime(id: Long, endTime: LocalDateTime, duration: Long)

    @Query("SELECT * FROM playback_event WHERE userId = :userId ORDER BY startTime DESC LIMIT :limit")
    fun getRecentPlaybackEvents(userId: Int, limit: Int = 50): Flow<List<PlaybackEventEntity>>

    // Get the total playback time for a user in the past month
    @Query("""
        SELECT SUM(duration) FROM playback_event 
        WHERE userId = :userId AND startTime >= :startDate
    """)
    suspend fun getTotalPlaybackTimeInPeriod(userId: Int, startDate: LocalDateTime): Long?

    // Get most played song
    @Query("""
        SELECT songId, COUNT(*) as playCount FROM playback_event 
        WHERE userId = :userId AND startTime >= :startDate
        GROUP BY songId 
        ORDER BY playCount DESC 
        LIMIT 1
    """)
    suspend fun getMostPlayedSongInPeriod(userId: Int, startDate: LocalDateTime): MostPlayedSong?

    // Get consecutive play days
    @Query("""
        SELECT COUNT(DISTINCT date(startTime)) FROM playback_event 
        WHERE userId = :userId AND startTime >= :startDate
    """)
    suspend fun getPlayDaysCountInPeriod(userId: Int, startDate: LocalDateTime): Int
    
    // Data class for the most played song query
    data class MostPlayedSong(
        val songId: String,
        val playCount: Int
    )
    
    // Get most played artist
    @Transaction
    @Query("""
        SELECT s.artist, COUNT(*) as playCount 
        FROM playback_event e 
        JOIN song s ON e.songId = s.id 
        WHERE e.userId = :userId AND e.startTime >= :startDate
        GROUP BY s.artist 
        ORDER BY playCount DESC 
        LIMIT 1
    """)
    suspend fun getMostPlayedArtistInPeriod(userId: Int, startDate: LocalDateTime): MostPlayedArtist?
    
    // Data class for the most played artist query
    data class MostPlayedArtist(
        val artist: String,
        val playCount: Int
    )
}