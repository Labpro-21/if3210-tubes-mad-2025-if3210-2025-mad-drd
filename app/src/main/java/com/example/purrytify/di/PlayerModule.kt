package com.example.purrytify.di

import android.content.Context
import com.example.purrytify.data.repository.PlayerRepository
import com.example.purrytify.domain.player.PlayerBridge
import com.example.purrytify.util.AudioOutputManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton
import com.example.purrytify.domain.analytics.ListeningSessionTracker

/**
 * Module for providing player-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {
    /**
     * Provide the PlayerBridge as a singleton with PlayerRepository dependency
     */
    @Provides
    @Singleton
    fun providePlayerBridge(
        playerRepository: PlayerRepository,
        listeningSessionTracker: ListeningSessionTracker,
        @ApplicationContext context: Context
    ): PlayerBridge {
        return PlayerBridge(playerRepository, listeningSessionTracker, context)
    }
    
    /**
     * Provide the ListeningSessionTracker as a singleton
     */
    @Provides
    @Singleton
    fun provideListeningSessionTracker(
        analyticsRepository: com.example.purrytify.data.repository.AnalyticsRepository,
        externalScope: CoroutineScope
    ): ListeningSessionTracker {
        return ListeningSessionTracker(analyticsRepository, externalScope)
    }
    
    /**
     * Provide the AudioOutputManager as a singleton with PlayerRepository dependency
     */
    @Provides
    @Singleton
    fun provideAudioOutputManager(
        @ApplicationContext context: Context,
        externalScope: CoroutineScope,
        playerRepository: PlayerRepository
    ): AudioOutputManager {
        return AudioOutputManager(context, externalScope, playerRepository)
    }
}