package com.example.purrytify.util

import android.util.Log
import com.example.purrytify.data.local.dao.PlaybackEventDao
import com.example.purrytify.data.repository.AnalyticsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debug helper for analytics functionality
 * Can be used to verify analytics are working correctly
 */
@Singleton
class AnalyticsDebugHelper @Inject constructor(
    private val playbackEventDao: PlaybackEventDao,
    private val analyticsRepository: AnalyticsRepository,
    private val externalScope: CoroutineScope
) {
    private val TAG = "AnalyticsDebugHelper"
    
    /**
     * Log all playback events for a user (for debugging)
     */
    fun logAllPlaybackEvents(userId: Int) {
        externalScope.launch(Dispatchers.IO) {
            try {
                // Get all recent events
                val events = playbackEventDao.getRecentPlaybackEvents(userId, 100)
                
                events.collect { eventList ->
                    Log.d(TAG, "=== All Playback Events for User $userId ===")
                    eventList.forEach { event ->
                        Log.d(TAG, "Event: ${event.songTitle} by ${event.artistName}")
                        Log.d(TAG, "  Duration: ${event.listeningDuration}ms (${event.listeningDuration/1000.0}s)")
                        Log.d(TAG, "  Time: ${event.startTime}")
                        Log.d(TAG, "  Song ID: ${event.songId}")
                    }
                    Log.d(TAG, "=== Total Events: ${eventList.size} ===")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error logging playback events: ${e.message}", e)
            }
        }
    }
    
    /**
     * Log current month analytics for debugging
     */
    fun logCurrentMonthAnalytics(userId: Int) {
        externalScope.launch(Dispatchers.IO) {
            try {
                val now = LocalDateTime.now()
                val analytics = analyticsRepository.getMonthlyAnalytics(userId, now.year, now.monthValue)
                
                Log.d(TAG, "=== Current Month Analytics for User $userId ===")
                Log.d(TAG, "Month: ${analytics.displayName}")
                Log.d(TAG, "Total listening time: ${analytics.totalListeningTimeMs}ms (${analytics.formattedListeningTime})")
                Log.d(TAG, "Top artist: ${analytics.topArtist?.name} (${analytics.topArtist?.formattedDuration})")
                Log.d(TAG, "Top song: ${analytics.topSong?.title} by ${analytics.topSong?.artist} (${analytics.topSong?.formattedDuration})")
                Log.d(TAG, "Day streak: ${analytics.dayStreak?.songTitle} (${analytics.dayStreak?.consecutiveDays} days)")
                Log.d(TAG, "Has data: ${analytics.hasData}")
                Log.d(TAG, "Daily data points: ${analytics.dailyData.size}")
            } catch (e: Exception) {
                Log.e(TAG, "Error logging analytics: ${e.message}", e)
            }
        }
    }
    
    /**
     * Manually create a test playback event (for testing)
     */
    fun createTestPlaybackEvent(
        userId: Int,
        songTitle: String = "Test Song",
        artistName: String = "Test Artist",
        durationMs: Long = 30000 // 30 seconds
    ) {
        externalScope.launch(Dispatchers.IO) {
            try {
                analyticsRepository.recordListeningSession(
                    userId = userId,
                    songId = "test_${System.currentTimeMillis()}",
                    songTitle = songTitle,
                    artistName = artistName,
                    listeningDurationMs = durationMs
                )
                
                Log.d(TAG, "Created test playback event: $songTitle by $artistName (${durationMs}ms)")
            } catch (e: Exception) {
                Log.e(TAG, "Error creating test event: ${e.message}", e)
            }
        }
    }
    
    /**
     * Log database statistics
     */
    fun logDatabaseStats(userId: Int) {
        externalScope.launch(Dispatchers.IO) {
            try {
                val now = LocalDateTime.now()
                val startOfMonth = LocalDateTime.of(now.year, now.month, 1, 0, 0)
                val endOfMonth = startOfMonth.plusMonths(1)
                
                val totalTime = playbackEventDao.getTotalListeningTimeInMonth(userId, startOfMonth, endOfMonth)
                val hasData = playbackEventDao.hasDataInMonth(userId, startOfMonth, endOfMonth)
                val monthsWithData = playbackEventDao.getAllMonthsWithData(userId)
                
                Log.d(TAG, "=== Database Stats for User $userId ===")
                Log.d(TAG, "Total time this month: ${totalTime}ms")
                Log.d(TAG, "Has data this month: $hasData")
                Log.d(TAG, "Months with data: ${monthsWithData.size}")
                monthsWithData.forEach { month ->
                    Log.d(TAG, "  ${month.year}-${month.month}: ${month.totalEvents} events")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error logging database stats: ${e.message}", e)
            }
        }
    }
}