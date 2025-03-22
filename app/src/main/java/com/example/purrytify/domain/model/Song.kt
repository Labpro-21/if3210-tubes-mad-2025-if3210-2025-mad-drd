package com.example.purrytify.domain.model

import android.net.Uri
import com.example.purrytify.data.local.entity.SongEntity
import com.example.purrytify.data.local.entity.SongWithLikeStatus

data class Song(
    val id: Long = 0,
    val title: String,
    val artist: String,
    val filePath: String,
    val artworkPath: String?,
    val duration: Long,  // in milliseconds
    val addedAt: Long = System.currentTimeMillis(),
    val lastPlayedAt: Long? = null,
    val isLiked: Boolean = false
) {
    val fileUri: Uri
        get() = Uri.parse(filePath)
        
    val artworkUri: Uri?
        get() = artworkPath?.let { Uri.parse(it) }
        
    // Format duration to mm:ss
    val formattedDuration: String
        get() {
            val minutes = duration / 1000 / 60
            val seconds = (duration / 1000) % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
}

// Extension functions for mapping between domain and data layer
fun SongEntity.toDomain(isLiked: Boolean = false): Song {
    return Song(
        id = id,
        title = title,
        artist = artist,
        filePath = filePath,
        artworkPath = artworkPath,
        duration = duration,
        addedAt = addedAt,
        lastPlayedAt = lastPlayedAt,
        isLiked = isLiked
    )
}

fun SongWithLikeStatus.toDomain(): Song {
    return song.toDomain(isLiked)
}

fun Song.toEntity(): SongEntity {
    return SongEntity(
        id = id,
        title = title,
        artist = artist,
        filePath = filePath,
        artworkPath = artworkPath,
        duration = duration,
        addedAt = addedAt,
        lastPlayedAt = lastPlayedAt
    )
}