package com.example.purrytify.domain.model

import java.time.LocalDateTime

/**
 * Domain model for User, used throughout the app
 */
data class User(
    val id: Int,
    val username: String,
    val email: String,
    val profilePhoto: String = "dummy.png",
    val location: String = "ID", // Default to Indonesia
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)