package com.example.purrytify.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.repository.AuthRepository
import com.example.purrytify.domain.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkAuthStatus()
    }

    private fun checkAuthStatus() {
        viewModelScope.launch {
            // First check if we have an access token
            val accessToken = authRepository.getAccessToken().first()
            
            if (accessToken.isEmpty()) {
                _authState.value = AuthState.Unauthenticated
                return@launch
            }

            // Verify if token is valid
            try {
                val isTokenValid = authRepository.verifyToken().first()
                
                if (isTokenValid) {
                    _authState.value = AuthState.Authenticated
                    return@launch
                }

                // Token is invalid, try to refresh
                val refreshResult = authRepository.refreshToken().first()
                
                when (refreshResult) {
                    is Resource.Success -> {
                        _authState.value = AuthState.Authenticated
                    }
                    else -> {
                        // Failed to refresh token, clear tokens
                        authRepository.clearTokens()
                        _authState.value = AuthState.Unauthenticated
                    }
                }
            } catch (e: Exception) {
                // Handle any unexpected errors
                authRepository.clearTokens()
                _authState.value = AuthState.Unauthenticated
            }
        }
    }
}

sealed class AuthState {
    object Loading : AuthState()
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
}