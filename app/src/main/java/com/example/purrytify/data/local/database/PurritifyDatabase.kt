package com.example.purrytify.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.purrytify.data.local.dao.PlaybackEventDao
import com.example.purrytify.data.local.dao.SongDao
import com.example.purrytify.data.local.entity.PlaybackEventEntity
import com.example.purrytify.data.local.entity.SongEntity

/**
 * Main Room database for the application
 */
@Database(
    entities = [SongEntity::class, PlaybackEventEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PurritifyDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playbackEventDao(): PlaybackEventDao
}