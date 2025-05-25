package com.example.purrytify.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.purrytify.data.repository.AnalyticsRepository
import com.example.purrytify.domain.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for exporting analytics data to CSV files
 */
@Singleton
class ExportManager @Inject constructor(
    private val context: Context,
    private val analyticsRepository: AnalyticsRepository
) {
    private val TAG = "ExportManager"
    
    /**
     * Export analytics to CSV and save locally to Downloads folder
     */
    suspend fun exportAndSaveAnalytics(userId: Int, year: Int, month: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting local export for user $userId, $year-$month")
            
            // Get analytics data
            val analytics = analyticsRepository.getMonthlyAnalytics(userId, year, month)
            
            if (!analytics.hasData) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "No data available for ${analytics.displayName}", Toast.LENGTH_SHORT).show()
                }
                return@withContext false
            }
            
            // Create CSV content
            val csvContent = createCsvContent(analytics, userId)
            
            // Save to Downloads folder
            val success = saveToDownloads(csvContent, "${analytics.displayName}_Analytics.csv")
            
            if (success) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context, 
                        "Analytics exported to Downloads folder", 
                        Toast.LENGTH_LONG
                    ).show()
                }
                Log.d(TAG, "Successfully exported analytics to Downloads")
                return@withContext true
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to export analytics", Toast.LENGTH_SHORT).show()
                }
                return@withContext false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting analytics: ${e.message}", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error exporting analytics: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            return@withContext false
        }
    }
    
    /**
     * Export analytics to CSV and share via system share sheet
     */
    suspend fun exportAndShareAnalytics(userId: Int, year: Int, month: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting share export for user $userId, $year-$month")
            
            // Get analytics data
            val analytics = analyticsRepository.getMonthlyAnalytics(userId, year, month)
            
            if (!analytics.hasData) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "No data available for ${analytics.displayName}", Toast.LENGTH_SHORT).show()
                }
                return@withContext false
            }
            
            // Create CSV content
            val csvContent = createCsvContent(analytics, userId)
            
            // Save to cache directory for sharing
            val success = saveToCache(csvContent, "${analytics.displayName}_Analytics.csv")
            
            if (success) {
                Log.d(TAG, "Successfully exported analytics for sharing")
                return@withContext true
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to export analytics", Toast.LENGTH_SHORT).show()
                }
                return@withContext false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting analytics: ${e.message}", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error exporting analytics: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            return@withContext false
        }
    }
    
    /**
     * Create CSV content from analytics data
     */
    private suspend fun createCsvContent(analytics: com.example.purrytify.domain.model.MonthlyAnalytics, userId: Int): String {
        val csv = StringBuilder()
        
        // Header
        csv.appendLine("Purrytify Sound Capsule - ${analytics.displayName}")
        csv.appendLine("")
        
        // Summary section
        csv.appendLine("SUMMARY")
        csv.appendLine("Total Listening Time,${analytics.formattedListeningTime}")
        csv.appendLine("Top Artist,${analytics.topArtist?.name ?: "N/A"}")
        csv.appendLine("Top Song,${analytics.topSong?.title ?: "N/A"} - ${analytics.topSong?.artist ?: "N/A"}")
        
        // Day streak section
        analytics.dayStreak?.let { streak ->
            csv.appendLine("Day Streak,${streak.consecutiveDays} days - ${streak.songTitle} by ${streak.artist}")
        } ?: csv.appendLine("Day Streak,None")
        
        csv.appendLine("")
        
        // Get detailed data - need to get userId from the analytics method parameters
        // Since we don't have access to userId in this method, we'll get it from the outer scope
        // This is a limitation of the current design, but we'll work with it
        
        // Top Artists section
        csv.appendLine("TOP ARTISTS")
        csv.appendLine("Rank,Artist,Total Time,Play Count")
        
        val artistAnalytics = analyticsRepository.getArtistAnalytics(
            userId = userId,
            year = analytics.year,
            month = analytics.month
        )
        
        artistAnalytics.artists.forEachIndexed { index, artist ->
            csv.appendLine("${index + 1},\"${artist.name}\",${artist.formattedDuration},${artist.playCount}")
        }
        
        csv.appendLine("")
        
        // Top Songs section
        csv.appendLine("TOP SONGS")
        csv.appendLine("Rank,Song,Artist,Total Time,Play Count")
        
        val songAnalytics = analyticsRepository.getSongAnalytics(
            userId = userId,
            year = analytics.year,
            month = analytics.month
        )
        
        songAnalytics.songs.forEachIndexed { index, song ->
            csv.appendLine("${index + 1},\"${song.title}\",\"${song.artist}\",${song.formattedDuration},${song.playCount}")
        }
        
        return csv.toString()
    }
    
    /**
     * Save CSV content to Downloads folder
     */
    private fun saveToDownloads(csvContent: String, fileName: String): Boolean {
        return try {
            val downloadsDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10+ (API 29+), use the public Downloads directory
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            } else {
                // For older versions, use app-specific external files directory
                File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Purrytify")
            }
            
            // Create directory if it doesn't exist
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            
            val file = File(downloadsDir, fileName)
            
            FileWriter(file).use { writer ->
                writer.write(csvContent)
            }
            
            Log.d(TAG, "File saved to: ${file.absolutePath}")
            true
        } catch (e: IOException) {
            Log.e(TAG, "Error saving to Downloads: ${e.message}", e)
            false
        }
    }
    
    /**
     * Save CSV content to cache directory for sharing
     */
    private fun saveToCache(csvContent: String, fileName: String): Boolean {
        return try {
            val cacheDir = File(context.cacheDir, "analytics_exports")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            
            val file = File(cacheDir, fileName)
            
            FileWriter(file).use { writer ->
                writer.write(csvContent)
            }
            
            // Share the file
            shareFile(file)
            
            true
        } catch (e: IOException) {
            Log.e(TAG, "Error saving to cache: ${e.message}", e)
            false
        }
    }
    
    /**
     * Share a file using system share sheet
     */
    private fun shareFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Purrytify Analytics Export")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            val chooser = Intent.createChooser(shareIntent, "Share Analytics")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing file: ${e.message}", e)
            Toast.makeText(context, "Error sharing file", Toast.LENGTH_SHORT).show()
        }
    }
}