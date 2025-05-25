package com.example.purrytify.data.repository

import android.util.Log
import com.example.purrytify.data.local.dao.PlaybackEventDao
import com.example.purrytify.data.local.entity.PlaybackEventEntity
import com.example.purrytify.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for handling analytics and Sound Capsule data
 */
@Singleton
class AnalyticsRepository @Inject constructor(
    private val playbackEventDao: PlaybackEventDao
) {
    private val TAG = "AnalyticsRepository"
    
    // Mutex to prevent race conditions when recording sessions
    private val recordingMutex = Mutex()

    /**
     * Get all months with analytics data for a user
     */
    suspend fun getAllMonthsWithData(userId: Int): List<MonthYearData> = withContext(Dispatchers.IO) {
        try {
            val monthsData = playbackEventDao.getAllMonthsWithData(userId)
            monthsData.map { data ->
                MonthYearData(
                    year = data.year,
                    month = data.month,
                    totalEvents = data.totalEvents
                )
            }.sortedWith(compareByDescending<MonthYearData> { it.year }.thenByDescending { it.month })
        } catch (e: Exception) {
            Log.e(TAG, "Error getting months with data: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Get monthly analytics for a specific user, year, and month
     */
    suspend fun getMonthlyAnalytics(userId: Int, year: Int, month: Int): MonthlyAnalytics = withContext(Dispatchers.IO) {
        try {
            val startDate = LocalDateTime.of(year, month, 1, 0, 0)
            val endDate = startDate.plusMonths(1)
            
            Log.d(TAG, "Getting analytics for user $userId, $year-$month")
            
            val totalListeningTime = playbackEventDao.getTotalListeningTimeInMonth(userId, startDate, endDate)
            val topArtistData = playbackEventDao.getTopArtistInMonth(userId, startDate, endDate)
            val topSongData = playbackEventDao.getTopSongInMonth(userId, startDate, endDate)
            val dayStreakData = playbackEventDao.getLongestDayStreakInMonth(userId, startDate, endDate)
            val dailyData = playbackEventDao.getDailyListeningDataInMonth(userId, startDate, endDate)
            
            Log.d(TAG, "Analytics results - Time: ${totalListeningTime}ms (${totalListeningTime/60000.0} minutes), TopArtist: ${topArtistData?.artistName}, TopSong: ${topSongData?.songTitle}")
            
            MonthlyAnalytics(
                year = year,
                month = month,
                totalListeningTimeMs = totalListeningTime,
                topArtist = topArtistData?.let { 
                    TopArtist(it.artistName, it.totalDuration, it.playCount) 
                },
                topSong = topSongData?.let { 
                    TopSong(it.songTitle, it.artistName, it.totalDuration, it.playCount) 
                },
                dayStreak = dayStreakData?.let { 
                    DayStreak(it.songTitle, it.artistName, it.uniqueDays) 
                },
                dailyData = dailyData.map { 
                    DailyListeningData(it.date, it.totalDuration) 
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting monthly analytics: ${e.message}", e)
            MonthlyAnalytics(year, month, 0L, null, null, null)
        }
    }

    /**
     * Get analytics for all months with data
     */
    suspend fun getAllMonthlyAnalytics(userId: Int): List<MonthlyAnalytics> = withContext(Dispatchers.IO) {
        try {
            val monthsWithData = getAllMonthsWithData(userId)
            val analytics = mutableListOf<MonthlyAnalytics>()
            
            for (monthData in monthsWithData) {
                val monthlyAnalytics = getMonthlyAnalytics(userId, monthData.year, monthData.month)
                if (monthlyAnalytics.hasData) {
                    analytics.add(monthlyAnalytics)
                }
            }
            
            Log.d(TAG, "Loaded analytics for ${analytics.size} months")
            analytics
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all monthly analytics: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Get detailed artist analytics for a specific month
     */
    suspend fun getArtistAnalytics(userId: Int, year: Int, month: Int): ArtistAnalytics = withContext(Dispatchers.IO) {
        try {
            val startDate = LocalDateTime.of(year, month, 1, 0, 0)
            val endDate = startDate.plusMonths(1)
            
            val artistsData = playbackEventDao.getAllArtistsInMonth(userId, startDate, endDate)
            val artists = artistsData.take(10).map { 
                TopArtist(it.artistName, it.totalDuration, it.playCount) 
            }
            
            Log.d(TAG, "Artist analytics for $year-$month: ${artists.size} artists")
            ArtistAnalytics(artists, year, month)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting artist analytics: ${e.message}", e)
            ArtistAnalytics(emptyList(), year, month)
        }
    }

    /**
     * Get detailed song analytics for a specific month
     */
    suspend fun getSongAnalytics(userId: Int, year: Int, month: Int): SongAnalytics = withContext(Dispatchers.IO) {
        try {
            val startDate = LocalDateTime.of(year, month, 1, 0, 0)
            val endDate = startDate.plusMonths(1)
            
            val songsData = playbackEventDao.getAllSongsInMonth(userId, startDate, endDate)
            val songs = songsData.take(10).map { 
                TopSong(it.songTitle, it.artistName, it.totalDuration, it.playCount) 
            }
            
            Log.d(TAG, "Song analytics for $year-$month: ${songs.size} songs")
            SongAnalytics(songs, year, month)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting song analytics: ${e.message}", e)
            SongAnalytics(emptyList(), year, month)
        }
    }

    /**
     * Record a listening session for analytics
     * This should be called when a user finishes listening to a song or skips
     */
    suspend fun recordListeningSession(
        userId: Int,
        songId: String,
        songTitle: String,
        artistName: String,
        listeningDurationMs: Long
    ) = withContext(Dispatchers.IO) {
        // Use mutex to prevent race conditions when recording sessions
        recordingMutex.withLock {
            try {
                // Only record if there was actual listening time
                if (listeningDurationMs > 0) {
                    val event = PlaybackEventEntity(
                        userId = userId,
                        songId = songId,
                        songTitle = songTitle,
                        artistName = artistName,
                        startTime = LocalDateTime.now(),
                        listeningDuration = listeningDurationMs
                    )
                    
                    val eventId = playbackEventDao.insertPlaybackEvent(event)
                    Log.d(TAG, "Recorded listening session: $songTitle by $artistName, duration: ${listeningDurationMs}ms (${listeningDurationMs/1000.0}s), eventId: $eventId")
                } else {
                    Log.d(TAG, "Skipping session recording: duration is ${listeningDurationMs}ms")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error recording listening session: ${e.message}", e)
                throw e // Re-throw to let caller handle
            }
        }
    }

    /**
     * Check if user has any data for a specific month
     */
    suspend fun hasDataForMonth(userId: Int, year: Int, month: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val startDate = LocalDateTime.of(year, month, 1, 0, 0)
            val endDate = startDate.plusMonths(1)
            
            playbackEventDao.hasDataInMonth(userId, startDate, endDate)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking data for month: ${e.message}", e)
            false
        }
    }

    /**
     * Get current month analytics (for real-time updates in profile)
     */
    suspend fun getCurrentMonthAnalytics(userId: Int): MonthlyAnalytics {
        val now = LocalDateTime.now()
        return getMonthlyAnalytics(userId, now.year, now.monthValue)
    }

    /**
     * Export analytics data as CSV string for a specific month
     */
    suspend fun exportAnalyticsAsCSV(userId: Int, year: Int, month: Int): String = withContext(Dispatchers.IO) {
        try {
            val analytics = getMonthlyAnalytics(userId, year, month)
            val artistAnalytics = getArtistAnalytics(userId, year, month)
            val songAnalytics = getSongAnalytics(userId, year, month)
            
            val csv = StringBuilder()
            csv.appendLine("Purrytify Sound Capsule - ${analytics.monthName} $year")
            csv.appendLine()
            
            // Summary
            csv.appendLine("SUMMARY")
            csv.appendLine("Total Listening Time,${analytics.formattedListeningTime}")
            csv.appendLine("Top Artist,${analytics.topArtist?.name ?: "N/A"}")
            csv.appendLine("Top Song,${analytics.topSong?.title ?: "N/A"} by ${analytics.topSong?.artist ?: "N/A"}")
            csv.appendLine("Day Streak,${analytics.dayStreak?.songTitle ?: "N/A"} (${analytics.dayStreak?.consecutiveDays ?: 0} days)")
            csv.appendLine()
            
            // Top Artists
            csv.appendLine("TOP ARTISTS")
            csv.appendLine("Rank,Artist,Total Time,Play Count")
            artistAnalytics.artists.forEachIndexed { index, artist ->
                csv.appendLine("${index + 1},\"${artist.name}\",${artist.formattedDuration},${artist.playCount}")
            }
            csv.appendLine()
            
            // Top Songs
            csv.appendLine("TOP SONGS")
            csv.appendLine("Rank,Song,Artist,Total Time,Play Count")
            songAnalytics.songs.forEachIndexed { index, song ->
                csv.appendLine("${index + 1},\"${song.title}\",\"${song.artist}\",${song.formattedDuration},${song.playCount}")
            }
            
            Log.d(TAG, "Generated CSV export with ${csv.length} characters")
            csv.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting analytics as CSV: ${e.message}", e)
            "Error generating analytics report: ${e.message}"
        }
    }
}