package com.example.purrytify.di

import android.content.Context
import androidx.room.Room
import com.example.purrytify.data.local.dao.SongDao
import com.example.purrytify.data.local.database.PurrytifyDatabase
import com.example.purrytify.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): PurrytifyDatabase {
        return Room.databaseBuilder(
            context,
            PurrytifyDatabase::class.java,
            Constants.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideSongDao(database: PurrytifyDatabase): SongDao {
        return database.songDao()
    }
}