package com.example.purrytify.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.purrytify.data.local.database.Converters
import java.time.LocalDateTime

/**
 * Entity for storing locally created songs or downloaded songs
 */
@Entity(
    tableName = "song",
    indices = [
        Index(value = ["userId", "createdAt"]), // For sorting songs by creation date
        Index(value = ["userId", "likedAt"]), // For user's liked songs
        Index(value = ["userId", "lastPlayedAt"]), // For user's recently played
        Index(value = ["userId", "downloadedAt"]) // For user's downloaded songs
    ]
)
data class SongEntity(
    @PrimaryKey
    val id: String, // UUID for local songs
    val title: String,
    val artist: String,
    val artworkPath: String, // Local path for artwork
    val filePath: String, // Local path for audio file
    val duration: Long, // in milliseconds
    val userId: Int, // The user who added/owns the song
    @TypeConverters(Converters::class)
    val likedAt: LocalDateTime? = null, // Null if not liked
    @TypeConverters(Converters::class)
    val lastPlayedAt: LocalDateTime? = null, // For recently played tracking
    // For downloaded songs
    @TypeConverters(Converters::class)
    val downloadedAt: LocalDateTime? = null, // Null if user-created, non-null if downloaded from server
    val originalId: String? = null, // Original online ID if downloaded, null for user-created
    @TypeConverters(Converters::class)
    val createdAt: LocalDateTime,
    @TypeConverters(Converters::class)
    val updatedAt: LocalDateTime
)