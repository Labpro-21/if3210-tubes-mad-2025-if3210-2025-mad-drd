package com.example.purrytify.util

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.purrytify.data.repository.AnalyticsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for handling analytics export and sharing
 */
@Singleton
class ExportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analyticsRepository: AnalyticsRepository
) {
    private val TAG = "ExportManager"
    
    /**
     * Export analytics for a specific month as CSV and share it
     */
    suspend fun exportAndShareAnalytics(userId: Int, year: Int, month: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting export for user $userId, $year-$month")
                
                // Generate CSV content
                val csvContent = analyticsRepository.exportAnalyticsAsCSV(userId, year, month)
                
                if (csvContent.isEmpty() || csvContent.startsWith("Error")) {
                    Log.e(TAG, "Failed to generate CSV content: $csvContent")
                    showToast("Failed to generate analytics report")
                    return@withContext false
                }
                
                // Create analytics directory
                val analyticsDir = File(context.cacheDir, "analytics")
                if (!analyticsDir.exists()) {
                    val created = analyticsDir.mkdirs()
                    Log.d(TAG, "Analytics directory created: $created")
                }
                
                // Create file
                val fileName = "purrytify_analytics_${year}_${String.format("%02d", month)}.csv"
                val file = File(analyticsDir, fileName)
                
                // Write content to file
                file.writeText(csvContent, Charsets.UTF_8)
                
                Log.d(TAG, "CSV file created: ${file.absolutePath}, size: ${file.length()} bytes")
                
                // Share the file
                val shared = shareFile(file)
                
                if (shared) {
                    showToast("Analytics exported successfully!")
                } else {
                    showToast("Failed to share analytics file")
                }
                
                shared
            } catch (e: Exception) {
                Log.e(TAG, "Error exporting analytics: ${e.message}", e)
                showToast("Failed to export analytics: ${e.localizedMessage}")
                false
            }
        }
    }
    
    /**
     * Share a file using Android share intent
     */
    private fun shareFile(file: File): Boolean {
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Purrytify Sound Capsule Analytics")
                putExtra(Intent.EXTRA_TEXT, "Here are my Purrytify listening analytics!")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            val chooser = Intent.createChooser(shareIntent, "Share Analytics")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            context.startActivity(chooser)
            
            Log.d(TAG, "Successfully started share activity")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing file: ${e.message}", e)
            false
        }
    }
    
    /**
     * Show toast message on main thread
     */
    private fun showToast(message: String) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            try {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "Error showing toast: ${e.message}", e)
            }
        }
    }
}