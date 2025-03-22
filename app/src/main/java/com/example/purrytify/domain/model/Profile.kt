package com.example.purrytify.domain.model

import com.example.purrytify.data.remote.dto.response.ProfileResponse
import com.example.purrytify.util.Constants

data class Profile(
    val id: Int,
    val username: String,
    val email: String,
    val profilePhotoUrl: String,
    val location: String,
    val createdAt: String,
    val updatedAt: String
)

// Extension function to map from DTO to domain model
fun ProfileResponse.toDomain(): Profile {
    return Profile(
        id = id,
        username = username,
        email = email,
        profilePhotoUrl = "${Constants.BASE_URL}/uploads/profile-picture/$profilePhoto",
        location = location,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}