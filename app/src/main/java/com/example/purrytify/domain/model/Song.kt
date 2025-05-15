package com.example.purrytify.domain.model

import android.net.Uri
import com.example.purrytify.R
import java.time.LocalDateTime

/**
 * Domain model for Song, used throughout the app UI
 */
data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val artworkPath: String, // Local path for artwork
    val filePath: String, // Local path for audio file
    val duration: Long, // in milliseconds
    val userId: Int, // The user who added/owns the song
    val isLiked: Boolean = false, // Whether the user has liked this song
    val likedAt: LocalDateTime? = null, // When the song was liked (null if not liked)
    val lastPlayedAt: LocalDateTime? = null, // When the song was last played (null if never played)
    val downloadedAt: LocalDateTime? = null, // When the song was downloaded (null if local)
    val originalId: String? = null, // Original online ID if downloaded, null for user-created
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    /**
     * Get the artwork URI for display in UI
     */
    val artworkUri: Any
        get() = if (artworkPath.isNotEmpty()) {
            Uri.parse("file://$artworkPath")
        } else {
            // Default artwork resource ID
            R.drawable.ic_launcher_foreground
        }
}