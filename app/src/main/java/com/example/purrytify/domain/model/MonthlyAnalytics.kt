package com.example.purrytify.domain.model

/**
 * Domain models for Sound Capsule analytics
 */

/**
 * Represents a month/year with data
 */
data class MonthYearData(
    val year: Int,
    val month: Int,
    val totalEvents: Int
) {
    val monthName: String
        get() = java.time.Month.of(month).name.lowercase()
            .replaceFirstChar { it.uppercase() }
            
    val displayName: String
        get() = "$monthName $year"
}

/**
 * Monthly analytics data for a user
 */
data class MonthlyAnalytics(
    val year: Int,
    val month: Int,
    val totalListeningTimeMs: Long,
    val topArtist: TopArtist?,
    val topSong: TopSong?,
    val dayStreak: DayStreak?,
    val dailyData: List<DailyListeningData> = emptyList()
) {
    val hasData: Boolean
        get() = totalListeningTimeMs > 0 || topArtist != null || topSong != null || dayStreak != null
        
    val formattedListeningTime: String
        get() {
            val totalMinutes = totalListeningTimeMs / (1000 * 60)
            return "$totalMinutes minutes"
        }

    val monthName: String
        get() = java.time.Month.of(month).name.lowercase()
            .replaceFirstChar { it.uppercase() }
            
    val displayName: String
        get() = "$monthName $year"
}

/**
 * Top artist data for analytics
 */
data class TopArtist(
    val name: String,
    val totalDurationMs: Long,
    val playCount: Int
) {
    val formattedDuration: String
        get() {
            val totalMinutes = totalDurationMs / (1000 * 60)
            return "$totalMinutes min"
        }
}

/**
 * Top song data for analytics
 */
data class TopSong(
    val title: String,
    val artist: String,
    val totalDurationMs: Long,
    val playCount: Int
) {
    val formattedDuration: String
        get() {
            val totalMinutes = totalDurationMs / (1000 * 60)
            return "$totalMinutes min"
        }
}

/**
 * Day streak data for analytics
 */
data class DayStreak(
    val songTitle: String,
    val artist: String,
    val consecutiveDays: Int
)

/**
 * Daily listening data for charts
 */
data class DailyListeningData(
    val date: String,
    val totalDurationMs: Long
) {
    val formattedDuration: String
        get() {
            val totalMinutes = totalDurationMs / (1000 * 60)
            return "$totalMinutes min"
        }
        
    val dayOfMonth: Int
        get() = try {
            date.split("-").last().toInt()
        } catch (e: Exception) {
            1
        }
}

/**
 * Detailed artist analytics
 */
data class ArtistAnalytics(
    val artists: List<TopArtist>,
    val year: Int,
    val month: Int
) {
    val monthName: String
        get() = java.time.Month.of(month).name.lowercase()
            .replaceFirstChar { it.uppercase() }
            
    val displayName: String
        get() = "$monthName $year"
            
    val hasData: Boolean
        get() = artists.isNotEmpty()
        
    val totalArtists: Int
        get() = artists.size
}

/**
 * Detailed song analytics
 */
data class SongAnalytics(
    val songs: List<TopSong>,
    val year: Int,
    val month: Int
) {
    val monthName: String
        get() = java.time.Month.of(month).name.lowercase()
            .replaceFirstChar { it.uppercase() }
            
    val displayName: String
        get() = "$monthName $year"
            
    val hasData: Boolean
        get() = songs.isNotEmpty()
        
    val totalSongs: Int
        get() = songs.size
}