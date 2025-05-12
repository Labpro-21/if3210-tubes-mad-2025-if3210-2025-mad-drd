package com.example.purrytify.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the splash screen
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _navigationState = MutableStateFlow<SplashNavigationState>(SplashNavigationState.Loading)
    val navigationState: StateFlow<SplashNavigationState> = _navigationState
    
    init {
        checkAuthStatus()
    }
    
    /**
     * Check if user is logged in and decide where to navigate
     */
    private fun checkAuthStatus() {
        viewModelScope.launch {
            // Add delay for the splash animation
            delay(1500)
            
            val isLoggedIn = authRepository.isLoggedIn()
            _navigationState.value = if (isLoggedIn) {
                SplashNavigationState.NavigateToHome
            } else {
                SplashNavigationState.NavigateToLogin
            }
        }
    }
}

/**
 * Possible navigation states after splash screen
 */
sealed class SplashNavigationState {
    data object Loading : SplashNavigationState()
    data object NavigateToLogin : SplashNavigationState()
    data object NavigateToHome : SplashNavigationState()
}