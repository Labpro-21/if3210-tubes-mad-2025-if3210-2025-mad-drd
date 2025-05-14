package com.example.purrytify.domain.model

/**
 * Domain model for user profile, used throughout the app
 */
data class Profile(
    val id: Int,
    val username: String,
    val email: String,
    val profilePhoto: String,
    val profilePhotoUrl: String, // Full URL to the profile photo
    val location: String // Country code
)