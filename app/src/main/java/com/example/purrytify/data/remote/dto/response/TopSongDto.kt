package com.example.purrytify.data.remote.dto.response

import com.google.gson.annotations.SerializedName

/**
 * Data class representing a top song from the API
 */
data class TopSongDto(
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
)