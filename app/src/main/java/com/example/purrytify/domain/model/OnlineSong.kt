package com.example.purrytify.domain.model

/**
 * Domain model for online song from Top Charts API
 */
data class OnlineSong(
    val id: Long,
    val title: String,
    val artist: String,
    val artworkUrl: String,
    val songUrl: String,
    val duration: String, // Format: "mm:ss"
    val country: String,
    val rank: Int,
    val createdAt: String,
    val updatedAt: String
)