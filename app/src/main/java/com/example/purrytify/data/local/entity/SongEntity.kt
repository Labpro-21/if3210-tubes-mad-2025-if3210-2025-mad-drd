package com.example.purrytify.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.purrytify.util.Constants

@Entity(tableName = Constants.SONGS_TABLE)
data class SongEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val artist: String,
    val filePath: String,
    val artworkPath: String?,
    val duration: Long,  // in milliseconds
    val addedAt: Long = System.currentTimeMillis(),
    val lastPlayedAt: Long? = null
)

@Entity(tableName = Constants.LIKED_SONGS_TABLE)
data class LikedSongEntity(
    @PrimaryKey
    val songId: Long,
    val likedAt: Long = System.currentTimeMillis()
)

// This is a relationship class that Room can understand
data class SongWithLikeStatus(
    @Embedded val song: SongEntity,
    @ColumnInfo(name = "is_liked", defaultValue = "0")
    val isLiked: Boolean = false
)