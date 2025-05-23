package com.example.purrytify.data.remote.dto.response

import com.google.gson.annotations.SerializedName

/**
 * Data class representing a single song detail from the API
 * Used for deep link handling when sharing songs
 */
data class SongDetailDto(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String,
    @SerializedName("artist") val artist: String,
    @SerializedName("artwork") val artwork: String,
    @SerializedName("url") val url: String,
    @SerializedName("duration") val duration: String, // Format: "mm:ss"
    @SerializedName("country") val country: String,
    @SerializedName("rank") val rank: Int,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String
) {
    /**
     * Convert to OnlineSong domain model
     */
    fun toDomain(): com.example.purrytify.domain.model.OnlineSong {
        return com.example.purrytify.domain.model.OnlineSong(
            id = id,
            title = title,
            artist = artist,
            artworkUrl = artwork,
            songUrl = url,
            duration = duration,
            country = country,
            rank = rank,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}