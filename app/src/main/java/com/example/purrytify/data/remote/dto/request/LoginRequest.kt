package com.example.purrytify.data.remote.dto.request

/**
 * Data class representing the login request payload
 */
data class LoginRequest(
    val email: String,
    val password: String
)