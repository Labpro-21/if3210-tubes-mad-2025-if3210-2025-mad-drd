package com.example.purrytify.ui.screens.profile

import com.example.purrytify.domain.model.Profile

/**
 * UI state for the profile screen
 */
sealed class ProfileUiState {
    /**
     * Loading state while profile data is being fetched
     */
    data object Loading : ProfileUiState()
    
    /**
     * Success state with profile data
     */
    data class Success(val profile: Profile) : ProfileUiState()
    
    /**
     * Error state with an error message
     */
    data class Error(val message: String) : ProfileUiState()
}