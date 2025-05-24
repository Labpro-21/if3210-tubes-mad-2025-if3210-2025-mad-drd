package com.example.purrytify.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.purrytify.data.local.entity.PlaybackEventEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * Data Access Object for PlaybackEvent entity
 * Updated with analytics queries for Sound Capsule feature
 */
@Dao
interface PlaybackEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaybackEvent(event: PlaybackEventEntity): Long

    @Query("SELECT * FROM playback_event WHERE userId = :userId ORDER BY startTime DESC LIMIT :limit")
    fun getRecentPlaybackEvents(userId: Int, limit: Int = 50): Flow<List<PlaybackEventEntity>>

    /**
     * Get all months/years that have analytics data for a user
     */
    @Query("""
        SELECT 
            CAST(strftime('%Y', startTime) AS INTEGER) as year,
            CAST(strftime('%m', startTime) AS INTEGER) as month,
            COUNT(*) as totalEvents
        FROM playback_event 
        WHERE userId = :userId 
        GROUP BY year, month
        HAVING totalEvents > 0
        ORDER BY year DESC, month DESC
    """)
    suspend fun getAllMonthsWithData(userId: Int): List<MonthYearData>

    // Analytics queries for monthly data
    
    /**
     * Get total listening time for a specific month
     */
    @Query("""
        SELECT SUM(listeningDuration) FROM playback_event 
        WHERE userId = :userId 
        AND startTime >= :startDate 
        AND startTime < :endDate
    """)
    suspend fun getTotalListeningTimeInMonth(userId: Int, startDate: LocalDateTime, endDate: LocalDateTime): Long?

    /**
     * Get top artist for a specific month based on total listening duration
     */
    @Query("""
        SELECT artistName, SUM(listeningDuration) as totalDuration, COUNT(*) as playCount
        FROM playback_event 
        WHERE userId = :userId 
        AND startTime >= :startDate 
        AND startTime < :endDate
        GROUP BY artistName 
        ORDER BY totalDuration DESC 
        LIMIT 1
    """)
    suspend fun getTopArtistInMonth(userId: Int, startDate: LocalDateTime, endDate: LocalDateTime): TopArtistData?

    /**
     * Get top song for a specific month based on total listening duration
     */
    @Query("""
        SELECT songTitle, artistName, SUM(listeningDuration) as totalDuration, COUNT(*) as playCount
        FROM playback_event 
        WHERE userId = :userId 
        AND startTime >= :startDate 
        AND startTime < :endDate
        GROUP BY songTitle, artistName 
        ORDER BY totalDuration DESC 
        LIMIT 1
    """)
    suspend fun getTopSongInMonth(userId: Int, startDate: LocalDateTime, endDate: LocalDateTime): TopSongData?

    /**
     * Get all artists for a specific month with their listening data
     */
    @Query("""
        SELECT artistName, SUM(listeningDuration) as totalDuration, COUNT(*) as playCount
        FROM playback_event 
        WHERE userId = :userId 
        AND startTime >= :startDate 
        AND startTime < :endDate
        GROUP BY artistName 
        ORDER BY totalDuration DESC
    """)
    suspend fun getAllArtistsInMonth(userId: Int, startDate: LocalDateTime, endDate: LocalDateTime): List<TopArtistData>

    /**
     * Get all songs for a specific month with their listening data
     */
    @Query("""
        SELECT songTitle, artistName, SUM(listeningDuration) as totalDuration, COUNT(*) as playCount
        FROM playback_event 
        WHERE userId = :userId 
        AND startTime >= :startDate 
        AND startTime < :endDate
        GROUP BY songTitle, artistName 
        ORDER BY totalDuration DESC
    """)
    suspend fun getAllSongsInMonth(userId: Int, startDate: LocalDateTime, endDate: LocalDateTime): List<TopSongData>

    /**
     * Calculate day streak - songs played on consecutive days (2+ days)
     * Simplified approach that counts unique days a song was played
     */
    @Query("""
        SELECT songTitle, artistName, COUNT(DISTINCT DATE(startTime)) as uniqueDays
        FROM playback_event 
        WHERE userId = :userId 
        AND startTime >= :startDate 
        AND startTime < :endDate
        GROUP BY songTitle, artistName 
        HAVING uniqueDays >= 2
        ORDER BY uniqueDays DESC 
        LIMIT 1
    """)
    suspend fun getLongestDayStreakInMonth(userId: Int, startDate: LocalDateTime, endDate: LocalDateTime): DayStreakData?

    /**
     * Get daily listening data for a specific month (for charts)
     */
    @Query("""
        SELECT DATE(startTime) as date, SUM(listeningDuration) as totalDuration
        FROM playback_event 
        WHERE userId = :userId 
        AND startTime >= :startDate 
        AND startTime < :endDate
        GROUP BY DATE(startTime)
        ORDER BY date ASC
    """)
    suspend fun getDailyListeningDataInMonth(userId: Int, startDate: LocalDateTime, endDate: LocalDateTime): List<DailyListeningData>

    /**
     * Check if user has any playback data for a specific month
     */
    @Query("""
        SELECT COUNT(*) > 0 FROM playback_event 
        WHERE userId = :userId 
        AND startTime >= :startDate 
        AND startTime < :endDate
    """)
    suspend fun hasDataInMonth(userId: Int, startDate: LocalDateTime, endDate: LocalDateTime): Boolean

    // Data classes for query results
    data class MonthYearData(
        val year: Int,
        val month: Int,
        val totalEvents: Int
    )
    
    data class TopArtistData(
        val artistName: String,
        val totalDuration: Long,
        val playCount: Int
    )
    
    data class TopSongData(
        val songTitle: String,
        val artistName: String,
        val totalDuration: Long,
        val playCount: Int
    )
    
    data class DayStreakData(
        val songTitle: String,
        val artistName: String,
        val uniqueDays: Int
    )

    data class DailyListeningData(
        val date: String,
        val totalDuration: Long
    )
}