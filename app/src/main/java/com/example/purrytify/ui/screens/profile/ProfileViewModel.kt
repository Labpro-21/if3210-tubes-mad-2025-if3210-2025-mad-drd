package com.example.purrytify.ui.screens.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.local.datastore.UserPreferences
import com.example.purrytify.data.repository.AnalyticsRepository
import com.example.purrytify.data.repository.AuthRepository
import com.example.purrytify.data.repository.ProfileRepository
import com.example.purrytify.domain.model.MonthlyAnalytics
import com.example.purrytify.domain.player.PlayerBridge
import com.example.purrytify.util.ExportManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for the profile screen
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
    private val analyticsRepository: AnalyticsRepository,
    private val userPreferences: UserPreferences,
    private val playerBridge: PlayerBridge,
    private val exportManager: ExportManager
) : ViewModel() {
    
    private val TAG = "ProfileViewModel"
    
    // UI state for the profile screen
    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState = _uiState.asStateFlow()
    
    // Dialog visibility states
    private val _showLogoutDialog = MutableStateFlow(false)
    val showLogoutDialog = _showLogoutDialog.asStateFlow()
    
    private val _showEditDialog = MutableStateFlow(false)
    val showEditDialog = _showEditDialog.asStateFlow()
    
    // Update loading state and error
    private val _isUpdateLoading = MutableStateFlow(false)
    val isUpdateLoading = _isUpdateLoading.asStateFlow()
    
    private val _updateError = MutableStateFlow<String?>(null)
    val updateError = _updateError.asStateFlow()
    
    // Song statistics
    private val _songsCount = MutableStateFlow(0)
    val songsCount: StateFlow<Int> = _songsCount
    
    private val _likedSongsCount = MutableStateFlow(0)
    val likedSongsCount: StateFlow<Int> = _likedSongsCount
    
    private val _listenedSongsCount = MutableStateFlow(0)
    val listenedSongsCount: StateFlow<Int> = _listenedSongsCount

    // All monthly analytics (for Sound Capsule display)
    private val _allMonthlyAnalytics = MutableStateFlow<List<MonthlyAnalytics>>(emptyList())
    val allMonthlyAnalytics: StateFlow<List<MonthlyAnalytics>> = _allMonthlyAnalytics.asStateFlow()

    // Loading state for analytics
    private val _analyticsLoading = MutableStateFlow(false)
    val analyticsLoading: StateFlow<Boolean> = _analyticsLoading.asStateFlow()

    // User ID
    private val _userId = MutableStateFlow<Int?>(null)
    val userId: StateFlow<Int?> = _userId.asStateFlow()

    // Export loading state
    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()
    
    init {
        // Load user ID and analytics
        viewModelScope.launch {
            userPreferences.userId.collect { userId ->
                _userId.value = userId
                userId?.let {
                    // Set user ID for player bridge analytics
                    playerBridge.setCurrentUserId(it)
                    // Load all analytics data
                    loadAllMonthlyAnalytics()
                    // Start periodic refresh for real-time updates
                    startPeriodicAnalyticsRefresh()
                }
            }
        }
        
        loadProfile()
    }
    
    /**
     * Start periodic refresh of analytics for real-time updates
     * Refreshes every 30 seconds while user is on profile page
     */
    private fun startPeriodicAnalyticsRefresh() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(30_000) // 30 seconds
                
                // Only refresh if we have a user ID
                if (_userId.value != null) {
                    loadAllMonthlyAnalytics()
                }
            }
        }
    }
    
    /**
     * Load the user's profile and song statistics
     */
    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            
            try {
                // Get user ID
                val userId = userPreferences.userId.firstOrNull()
                
                if (userId != null) {
                    // Load song statistics
                    loadSongStats(userId)
                    
                    // Load profile
                    when (val result = profileRepository.getProfile()) {
                        is com.example.purrytify.domain.util.Result.Success -> {
                            _uiState.value = ProfileUiState.Success(result.data)
                        }
                        is com.example.purrytify.domain.util.Result.Error -> {
                            _uiState.value = ProfileUiState.Error(result.message)
                        }
                        is com.example.purrytify.domain.util.Result.Loading -> {
                            _uiState.value = ProfileUiState.Loading
                        }
                    }
                } else {
                    _uiState.value = ProfileUiState.Error("User ID not found")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading profile: ${e.message}", e)
                _uiState.value = ProfileUiState.Error("Failed to load profile: ${e.message}")
            }
        }
    }
    
    /**
     * Load song statistics
     */
    private suspend fun loadSongStats(userId: Int) {
        try {
            val stats = profileRepository.getSongStats(userId)
            _songsCount.value = stats.first
            _likedSongsCount.value = stats.second
            _listenedSongsCount.value = stats.third
        } catch (e: Exception) {
            Log.e(TAG, "Error loading song stats: ${e.message}", e)
        }
    }
    
    /**
     * Update the user's profile picture
     */
    fun updateProfilePicture(photoFile: File) {
        viewModelScope.launch {
            _isUpdateLoading.value = true
            _updateError.value = null
            
            try {
                val result = profileRepository.updateProfile(null, photoFile)
                
                when (result) {
                    is com.example.purrytify.domain.util.Result.Success -> {
                        // Explicitly reload profile after update
                        loadProfile()
                    }
                    is com.example.purrytify.domain.util.Result.Error -> {
                        _updateError.value = result.message
                    }
                    is com.example.purrytify.domain.util.Result.Loading -> {
                        // No-op
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating profile picture: ${e.message}", e)
                _updateError.value = "Failed to update profile picture: ${e.message}"
            } finally {
                _isUpdateLoading.value = false
            }
        }
    }
    
    /**
     * Update the profile location
     */
    fun updateLocation(location: String) {
        viewModelScope.launch {
            _isUpdateLoading.value = true
            _updateError.value = null
            
            try {
                val result = profileRepository.updateProfile(location, null)
                
                when (result) {
                    is com.example.purrytify.domain.util.Result.Success -> {
                        // Explicitly reload profile after update
                        loadProfile()
                        _showEditDialog.value = false
                    }
                    is com.example.purrytify.domain.util.Result.Error -> {
                        _updateError.value = result.message
                    }
                    is com.example.purrytify.domain.util.Result.Loading -> {
                        // No-op
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating location: ${e.message}", e)
                _updateError.value = "Failed to update location: ${e.message}"
            } finally {
                _isUpdateLoading.value = false
            }
        }
    }
    
    /**
     * Logout the user and stop any playing music
     */
    fun logout() {
        viewModelScope.launch {
            try {
                // Stop any currently playing music
                playerBridge.stop()
                Log.d(TAG, "Stopped music playback on logout")
                
                // Perform logout
                authRepository.logout()
                Log.d(TAG, "User logged out successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error during logout: ${e.message}", e)
            }
        }
    }
    
    /**
     * Show/hide the logout dialog
     */
    fun onLogoutClick() {
        _showLogoutDialog.value = true
    }
    
    fun dismissLogoutDialog() {
        _showLogoutDialog.value = false
    }
    
    /**
     * Load all monthly analytics for Sound Capsule display
     */
    private fun loadAllMonthlyAnalytics() {
        val userId = _userId.value ?: return
        
        viewModelScope.launch {
            try {
                _analyticsLoading.value = true
                
                val allAnalytics = analyticsRepository.getAllMonthlyAnalytics(userId)
                _allMonthlyAnalytics.value = allAnalytics
                
                Log.d(TAG, "Loaded analytics for ${allAnalytics.size} months")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading all monthly analytics: ${e.message}", e)
            } finally {
                _analyticsLoading.value = false
            }
        }
    }
    
    /**
     * Refresh analytics data (called periodically or on user action)
     */
    fun refreshAnalytics() {
        loadAllMonthlyAnalytics()
    }
    
    /**
     * Export analytics for a specific month to local Downloads folder
     */
    fun exportAnalytics(year: Int, month: Int) {
        val userId = _userId.value ?: return
        
        viewModelScope.launch {
            try {
                _isExporting.value = true
                
                Log.d(TAG, "Starting local analytics export for $year-$month")
                
                val success = exportManager.exportAndSaveAnalytics(userId, year, month)
                
                if (success) {
                    Log.d(TAG, "Analytics export to Downloads completed successfully")
                } else {
                    Log.e(TAG, "Analytics export to Downloads failed")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error exporting analytics to Downloads: ${e.message}", e)
            } finally {
                _isExporting.value = false
            }
        }
    }
    
    /**
     * Share analytics for a specific month via system share sheet
     */
    fun shareAnalytics(year: Int, month: Int) {
        val userId = _userId.value ?: return
        
        viewModelScope.launch {
            try {
                _isExporting.value = true
                
                Log.d(TAG, "Starting analytics share for $year-$month")
                
                val success = exportManager.exportAndShareAnalytics(userId, year, month)
                
                if (success) {
                    Log.d(TAG, "Analytics share completed successfully")
                } else {
                    Log.e(TAG, "Analytics share failed")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error sharing analytics: ${e.message}", e)
            } finally {
                _isExporting.value = false
            }
        }
    }
    
    /**
     * Show/hide the edit profile dialog
     */
    fun onEditProfileClick() {
        _showEditDialog.value = true
    }
    
    fun dismissEditDialog() {
        _showEditDialog.value = false
        _updateError.value = null
    }
}