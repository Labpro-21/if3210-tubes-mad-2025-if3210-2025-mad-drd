package com.example.purrytify.di

import android.content.Context
import com.example.purrytify.data.local.dao.PlaybackEventDao
import com.example.purrytify.data.repository.AnalyticsRepository
import com.example.purrytify.util.AnalyticsDebugHelper
import com.example.purrytify.util.ExportManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

/**
 * Module for utility classes
 */
@Module
@InstallIn(SingletonComponent::class)
object UtilModule {
    
    @Provides
    @Singleton
    fun provideExportManager(
        @ApplicationContext context: Context,
        analyticsRepository: AnalyticsRepository
    ): ExportManager {
        return ExportManager(context, analyticsRepository)
    }
    
    @Provides
    @Singleton
    fun provideAnalyticsDebugHelper(
        playbackEventDao: PlaybackEventDao,
        analyticsRepository: AnalyticsRepository,
        externalScope: CoroutineScope
    ): AnalyticsDebugHelper {
        return AnalyticsDebugHelper(playbackEventDao, analyticsRepository, externalScope)
    }
}