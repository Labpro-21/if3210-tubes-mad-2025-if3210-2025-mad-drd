package com.example.purrytify.ui.screens.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.repository.AuthRepository
import com.example.purrytify.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the login screen
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _email = MutableStateFlow("")
    val email = _email.asStateFlow()
    
    private val _password = MutableStateFlow("")
    val password = _password.asStateFlow()
    
    private val _emailError = MutableStateFlow<String?>(null)
    val emailError = _emailError.asStateFlow()
    
    private val _passwordError = MutableStateFlow<String?>(null)
    val passwordError = _passwordError.asStateFlow()
    
    private val _loginUiState = MutableStateFlow<LoginUiState>(LoginUiState.Initial)
    val loginUiState: StateFlow<LoginUiState> = _loginUiState
    
    /**
     * Update email and validate
     */
    fun updateEmail(value: String) {
        _email.value = value
        validateEmail()
    }
    
    /**
     * Update password and validate
     */
    fun updatePassword(value: String) {
        _password.value = value
        validatePassword()
    }
    
    /**
     * Validate email format
     */
    private fun validateEmail() {
        val emailValue = _email.value.trim()
        
        _emailError.value = when {
            emailValue.isEmpty() -> "Email is required"
            // Basic email validation
            !emailValue.contains('@') || !emailValue.contains('.') -> "Please enter a valid email"
            else -> null
        }
    }
    
    /**
     * Validate password
     */
    private fun validatePassword() {
        val passwordValue = _password.value
        
        _passwordError.value = when {
            passwordValue.isEmpty() -> "Password is required"
            else -> null
        }
    }
    
    /**
     * Attempt to login with current credentials
     */
    fun login() {
        // Validate fields first
        validateEmail()
        validatePassword()
        
        // Check if there are validation errors
        if (_emailError.value != null || _passwordError.value != null) {
            return
        }
        
        // Set loading state
        _loginUiState.value = LoginUiState.Loading
        
        viewModelScope.launch {
            try {
                val result = authRepository.login(_email.value.trim(), _password.value)
                
            when (result) {
                is Result.Success -> {
                    Log.d("LoginViewModel", "Login successful")
                    _loginUiState.value = LoginUiState.Success
                }
                is Result.Error -> {
                    Log.e("LoginViewModel", "Login failed: ${result.exception.message}")
                    
                    // For credential errors, ONLY set field errors, NOT the general UI state error
                    if (result.message.contains("Invalid username or password", ignoreCase = true)) {
                        _emailError.value = "Invalid username or password"
                        _passwordError.value = "Invalid username or password"
                        _loginUiState.value = LoginUiState.Initial // Reset UI state
                    } else {
                        // For other errors, use the general UI state error
                        _loginUiState.value = LoginUiState.Error(result.message)
                    }
                }
                is Result.Loading -> {
                    _loginUiState.value = LoginUiState.Loading
                }
            }
            
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Unexpected error during login: ${e.message}")
                _loginUiState.value = LoginUiState.Error("An unexpected error occurred")
            }
        }
    }
}

/**
 * UI state for the login screen
 */
sealed class LoginUiState {
    data object Initial : LoginUiState()
    data object Loading : LoginUiState()
    data object Success : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}