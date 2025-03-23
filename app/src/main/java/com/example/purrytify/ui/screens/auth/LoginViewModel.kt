package com.example.purrytify.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.repository.AuthRepository
import com.example.purrytify.domain.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _loginUiState = MutableStateFlow<LoginUiState>(LoginUiState.Initial)
    val loginUiState: StateFlow<LoginUiState> = _loginUiState.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _emailError = MutableStateFlow<String?>(null)
    val emailError: StateFlow<String?> = _emailError.asStateFlow()

    private val _passwordError = MutableStateFlow<String?>(null)
    val passwordError: StateFlow<String?> = _passwordError.asStateFlow()

    fun updateEmail(newEmail: String) {
        _email.value = newEmail
        validateEmail(newEmail)
    }

    fun updatePassword(newPassword: String) {
        _password.value = newPassword
        validatePassword(newPassword)
    }

    private fun validateEmail(email: String): Boolean {
        return if (email.isEmpty()) {
            _emailError.value = "Email cannot be empty"
            false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailError.value = "Invalid email format"
            false
        } else {
            _emailError.value = null
            true
        }
    }

    private fun validatePassword(password: String): Boolean {
        return if (password.isEmpty()) {
            _passwordError.value = "Password cannot be empty"
            false
        } else {
            _passwordError.value = null
            true
        }
    }

    fun login() {
        val emailValid = validateEmail(_email.value)
        val passwordValid = validatePassword(_password.value)

        if (!emailValid || !passwordValid) {
            return
        }

        _loginUiState.value = LoginUiState.Loading

        viewModelScope.launch {
            val result = authRepository.login(_email.value, _password.value)
            result.collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        _loginUiState.value = LoginUiState.Success
                    }
                    is Resource.Error -> {
                        _loginUiState.value = LoginUiState.Error(resource.message)
                    }
                    is Resource.Loading -> {
                        _loginUiState.value = LoginUiState.Loading
                    }
                }
            }
        }
    }

    fun resetState() {
        _loginUiState.value = LoginUiState.Initial
    }
}

sealed class LoginUiState {
    object Initial : LoginUiState()
    object Loading : LoginUiState()
    object Success : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}
