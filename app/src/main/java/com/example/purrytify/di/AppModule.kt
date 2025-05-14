package com.example.purrytify.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.example.purrytify.data.local.dao.PlaybackEventDao
import com.example.purrytify.data.local.dao.SongDao
import com.example.purrytify.data.local.database.PurritifyDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

// Extension properties for DataStore instances
private val Context.userPrefsDataStore by preferencesDataStore(name = "user_preferences")
private val Context.tokenDataStore by preferencesDataStore(name = "token_store")

/**
 * Main application dependency injection module
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    @UserPreferencesDataStoreQualifier
    fun provideUserPreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.userPrefsDataStore
    }
    
    @Provides
    @Singleton
    @TokenDataStoreQualifier
    fun provideTokenDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.tokenDataStore
    }
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PurritifyDatabase {
        
        // context.deleteDatabase("purrytify_database")

        return Room.databaseBuilder(
            context,
            PurritifyDatabase::class.java,
            "purrytify_database"
        ).build()
    }
    
    @Provides
    @Singleton
    fun provideSongDao(database: PurritifyDatabase): SongDao {
        return database.songDao()
    }
    
    @Provides
    @Singleton
    fun providePlaybackEventDao(database: PurritifyDatabase): PlaybackEventDao {
        return database.playbackEventDao()
    }
    
    @Provides
    @Singleton
    fun provideApplicationScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}