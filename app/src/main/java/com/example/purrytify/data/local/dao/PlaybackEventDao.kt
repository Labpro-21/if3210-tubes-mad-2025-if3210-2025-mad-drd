package com.example.purrytify.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.purrytify.data.local.entity.PlaybackEventEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import java.time.LocalDate

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
        SELECT COALESCE(SUM(listeningDuration), 0) FROM playback_event 
        WHERE userId = :userId 
        AND startTime >= :startDate 
        AND startTime < :endDate
    """)
    suspend fun getTotalListeningTimeInMonth(userId: Int, startDate: LocalDateTime, endDate: LocalDateTime): Long

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
     * Ordered by total listening duration (not play count)
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
     * Ordered by total listening duration (not play count)
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
     * Get song play dates for streak calculation - FIXED VERSION
     * Simple query that returns song-date combinations
     */
    @Query("""
        SELECT 
            songTitle,
            artistName,
            DATE(startTime) as playDate
        FROM playback_event 
        WHERE userId = :userId 
        AND startTime >= :startDate 
        AND startTime < :endDate
        GROUP BY songTitle, artistName, DATE(startTime)
        ORDER BY songTitle, artistName, DATE(startTime)
    """)
    suspend fun getSongPlayDatesInMonth(userId: Int, startDate: LocalDateTime, endDate: LocalDateTime): List<SongPlayDateData>

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

    /**
    * Extension function to calculate longest day streak
    * This replaces the complex SQL query with Kotlin logic
    */
    suspend fun getLongestDayStreakInMonth(
        userId: Int, 
        startDate: LocalDateTime, 
        endDate: LocalDateTime
    ): DayStreakData? {
        val songPlayDates = getSongPlayDatesInMonth(userId, startDate, endDate)
        
        if (songPlayDates.isEmpty()) return null
        
        // Group by song
        val songGroups = songPlayDates.groupBy { "${it.songTitle}|${it.artistName}" }
        
        var longestStreak = 0
        var longestStreakSong: Pair<String, String>? = null
        
        songGroups.forEach { (songKey, playDates) ->
            val (songTitle, artistName) = songKey.split("|", limit = 2)
            
            // Convert dates to LocalDate and sort
            val dates = playDates.map { LocalDate.parse(it.playDate) }.sorted()
            
            // Calculate longest consecutive streak
            if (dates.isNotEmpty()) {
                var currentStreak = 1
                var maxStreak = 1
                
                for (i in 1 until dates.size) {
                    val currentDate = dates[i]
                    val previousDate = dates[i - 1]
                    
                    if (currentDate == previousDate.plusDays(1)) {
                        // Consecutive day
                        currentStreak++
                        maxStreak = maxOf(maxStreak, currentStreak)
                    } else {
                        // Non-consecutive, reset streak
                        currentStreak = 1
                    }
                }
                
                // Only consider streaks of 2+ days
                if (maxStreak >= 2 && maxStreak > longestStreak) {
                    longestStreak = maxStreak
                    longestStreakSong = songTitle to artistName
                }
            }
        }
        
        return longestStreakSong?.let { (title, artist) ->
            PlaybackEventDao.DayStreakData(
                songTitle = title,
                artistName = artist,
                uniqueDays = longestStreak
            )
        }
    }

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

    data class SongPlayDateData(
        val songTitle: String,
        val artistName: String,
        val playDate: String
    )
}