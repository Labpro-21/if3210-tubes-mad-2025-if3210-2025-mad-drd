package com.example.purrytify.data.remote.dto.request

data class UpdateProfileRequest(
    val username: String,
    val email: String,
    val location: String
)