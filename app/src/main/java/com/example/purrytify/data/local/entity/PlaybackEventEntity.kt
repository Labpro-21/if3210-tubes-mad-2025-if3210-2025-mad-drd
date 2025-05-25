package com.example.purrytify.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.purrytify.data.local.database.Converters
import java.time.LocalDateTime

/**
 * Entity for storing playback events for analytics
 */
@Entity(
    tableName = "playback_event",
    foreignKeys = [
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId", "songId"]), // For song frequency analytics
        Index(value = ["userId", "startTime"]), // For date-based analytics
        Index(value = ["songId"]),
        Index(value = ["userId", "artistName"]), // For artist analytics
        Index(value = ["userId", "songTitle", "artistName"]) // For song analytics
    ]
)
data class PlaybackEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Int,
    val songId: String,
    val songTitle: String, // Store song title for analytics (in case title is edited later)
    val artistName: String, // Store artist name for analytics (in case artist is edited later)
    @TypeConverters(Converters::class)
    val startTime: LocalDateTime,
    val listeningDuration: Long // Actual listening duration in milliseconds
)