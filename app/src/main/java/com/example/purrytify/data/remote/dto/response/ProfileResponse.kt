package com.example.purrytify.data.remote.dto.response

import com.example.purrytify.domain.model.User
import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Data class representing the profile response from the API
 */
data class ProfileResponse(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("username")
    val username: String,
    
    @SerializedName("email")
    val email: String,
    
    @SerializedName("profilePhoto")
    val profilePhoto: String,
    
    @SerializedName("location")
    val location: String,
    
    @SerializedName("createdAt")
    val createdAt: String,
    
    @SerializedName("updatedAt")
    val updatedAt: String
) {
    /**
     * Maps this response to a domain model
     */
    fun toDomain(): User {
        val formatter = DateTimeFormatter.ISO_DATE_TIME
        return User(
            id = id,
            username = username,
            email = email,
            profilePhoto = profilePhoto,
            location = location,
            createdAt = LocalDateTime.parse(createdAt, formatter),
            updatedAt = LocalDateTime.parse(updatedAt, formatter)
        )
    }
}