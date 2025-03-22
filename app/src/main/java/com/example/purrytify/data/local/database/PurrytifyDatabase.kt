package com.example.purrytify.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.purrytify.data.local.dao.SongDao
import com.example.purrytify.data.local.entity.LikedSongEntity
import com.example.purrytify.data.local.entity.SongEntity

@Database(
    entities = [
        SongEntity::class,
        LikedSongEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PurrytifyDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
}