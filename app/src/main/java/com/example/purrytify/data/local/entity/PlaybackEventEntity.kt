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
        Index(value = ["songId"])
    ]
)
data class PlaybackEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Int,
    val songId: String,
    @TypeConverters(Converters::class)
    val startTime: LocalDateTime,
    @TypeConverters(Converters::class)
    val endTime: LocalDateTime? = null, // Null if still on current player
    val duration: Long // Duration of playback in milliseconds
)