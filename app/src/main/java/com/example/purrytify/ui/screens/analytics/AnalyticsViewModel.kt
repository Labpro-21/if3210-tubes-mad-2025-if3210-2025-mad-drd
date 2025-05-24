package com.example.purrytify.ui.screens.analytics

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.local.datastore.UserPreferences
import com.example.purrytify.data.repository.AnalyticsRepository
import com.example.purrytify.domain.model.ArtistAnalytics
import com.example.purrytify.domain.model.MonthlyAnalytics
import com.example.purrytify.domain.model.MonthYearData
import com.example.purrytify.domain.model.SongAnalytics
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * ViewModel for analytics screens (Sound Capsule)
 */
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val userPreferences: UserPreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val TAG = "AnalyticsViewModel"
    
    // All months analytics (for profile and all-time analytics)
    private val _allMonthlyAnalytics = MutableStateFlow<List<MonthlyAnalytics>>(emptyList())
    val allMonthlyAnalytics: StateFlow<List<MonthlyAnalytics>> = _allMonthlyAnalytics.asStateFlow()
    
    // Months with data
    private val _monthsWithData = MutableStateFlow<List<MonthYearData>>(emptyList())
    val monthsWithData: StateFlow<List<MonthYearData>> = _monthsWithData.asStateFlow()
    
    // Current month analytics (for real-time updates)
    private val _currentMonthAnalytics = MutableStateFlow<MonthlyAnalytics?>(null)
    val currentMonthAnalytics: StateFlow<MonthlyAnalytics?> = _currentMonthAnalytics.asStateFlow()
    
    // Specific month analytics (for detail screens)
    private val _specificMonthAnalytics = MutableStateFlow<MonthlyAnalytics?>(null)
    val specificMonthAnalytics: StateFlow<MonthlyAnalytics?> = _specificMonthAnalytics.asStateFlow()
    
    // Artist analytics for detail screen
    private val _artistAnalytics = MutableStateFlow<ArtistAnalytics?>(null)
    val artistAnalytics: StateFlow<ArtistAnalytics?> = _artistAnalytics.asStateFlow()
    
    // Song analytics for detail screen
    private val _songAnalytics = MutableStateFlow<SongAnalytics?>(null)
    val songAnalytics: StateFlow<SongAnalytics?> = _songAnalytics.asStateFlow()
    
    // Loading states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()
    
    // Error state
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // Current user ID
    private val _userId = MutableStateFlow<Int?>(null)
    
    init {
        // Load user ID and start monitoring
        viewModelScope.launch {
            userPreferences.userId.collect { userId ->
                _userId.value = userId
                userId?.let {
                    loadAllAnalytics()
                    loadCurrentMonthAnalytics()
                }
            }
        }
    }
    
    /**
     * Load all analytics data (for profile display)
     */
    fun loadAllAnalytics() {
        val userId = _userId.value ?: return
        
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Load months with data
                val monthsData = analyticsRepository.getAllMonthsWithData(userId)
                _monthsWithData.value = monthsData
                
                // Load analytics for all months
                val allAnalytics = analyticsRepository.getAllMonthlyAnalytics(userId)
                _allMonthlyAnalytics.value = allAnalytics
                
                Log.d(TAG, "Loaded analytics for ${allAnalytics.size} months")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading all analytics: ${e.message}", e)
                _errorMessage.value = "Failed to load analytics"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Load current month analytics (for real-time updates)
     */
    fun loadCurrentMonthAnalytics() {
        val userId = _userId.value ?: return
        
        viewModelScope.launch {
            try {
                val analytics = analyticsRepository.getCurrentMonthAnalytics(userId)
                _currentMonthAnalytics.value = analytics
                
                Log.d(TAG, "Loaded current month analytics: ${analytics.formattedListeningTime}")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading current month analytics: ${e.message}", e)
                _errorMessage.value = "Failed to load analytics"
            }
        }
    }
    
    /**
     * Load analytics for a specific month/year (for detail screens)
     */
    fun loadAnalyticsForMonth(year: Int, month: Int) {
        val userId = _userId.value ?: return
        
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                val analytics = analyticsRepository.getMonthlyAnalytics(userId, year, month)
                _specificMonthAnalytics.value = analytics
                
                Log.d(TAG, "Loaded analytics for $year-$month: ${analytics.formattedListeningTime}")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading analytics for $year-$month: ${e.message}", e)
                _errorMessage.value = "Failed to load analytics"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Load detailed artist analytics for specific month/year
     */
    fun loadArtistAnalytics(year: Int, month: Int) {
        val userId = _userId.value ?: return
        
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                val artistAnalytics = analyticsRepository.getArtistAnalytics(userId, year, month)
                _artistAnalytics.value = artistAnalytics
                
                Log.d(TAG, "Loaded artist analytics for $year-$month: ${artistAnalytics.totalArtists} artists")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading artist analytics: ${e.message}", e)
                _errorMessage.value = "Failed to load artist analytics"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Load detailed song analytics for specific month/year
     */
    fun loadSongAnalytics(year: Int, month: Int) {
        val userId = _userId.value ?: return
        
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                val songAnalytics = analyticsRepository.getSongAnalytics(userId, year, month)
                _songAnalytics.value = songAnalytics
                
                Log.d(TAG, "Loaded song analytics for $year-$month: ${songAnalytics.totalSongs} songs")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading song analytics: ${e.message}", e)
                _errorMessage.value = "Failed to load song analytics"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Export analytics for a specific month as CSV
     */
    fun exportAnalytics(year: Int, month: Int) {
        val userId = _userId.value ?: return
        
        viewModelScope.launch {
            try {
                _isExporting.value = true
                
                val csvContent = analyticsRepository.exportAnalyticsAsCSV(userId, year, month)
                
                // Create analytics cache directory if it doesn't exist
                val analyticsDir = File(context.cacheDir, "analytics")
                if (!analyticsDir.exists()) {
                    analyticsDir.mkdirs()
                }
                
                // Save to file and share
                val fileName = "purrytify_analytics_${year}_${month}.csv"
                val file = File(analyticsDir, fileName)
                file.writeText(csvContent)
                
                // Share the file
                shareAnalyticsFile(file)
                
                Log.d(TAG, "Analytics exported to: ${file.absolutePath}")
            } catch (e: Exception) {
                Log.e(TAG, "Error exporting analytics: ${e.message}", e)
                _errorMessage.value = "Failed to export analytics"
            } finally {
                _isExporting.value = false
            }
        }
    }
    
    /**
     * Share analytics file using Android share intent
     */
    private fun shareAnalyticsFile(file: File) {
        try {
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
            
            Log.d(TAG, "Shared analytics file")
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing analytics file: ${e.message}", e)
            _errorMessage.value = "Failed to share analytics"
        }
    }
    
    /**
     * Refresh all analytics (for real-time updates)
     */
    fun refreshAllAnalytics() {
        loadAllAnalytics()
        loadCurrentMonthAnalytics()
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * Get formatted month name for display
     */
    fun getFormattedMonth(year: Int, month: Int): String {
        val monthName = java.time.Month.of(month).name.lowercase()
            .replaceFirstChar { it.uppercase() }
        return "$monthName $year"
    }
    
    /**
     * Check if there's any analytics data
     */
    fun hasAnyData(): Boolean {
        return _allMonthlyAnalytics.value.any { it.hasData }
    }
}