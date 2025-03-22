package com.example.purrytify.data.remote.dto.response

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String
)