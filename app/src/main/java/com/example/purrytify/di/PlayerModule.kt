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
    fun providePlayerBridge(playerRepository: PlayerRepository): PlayerBridge {
        return PlayerBridge(playerRepository)
    }
    
    /**
     * Provide the AudioOutputManager as a singleton
     */
    @Provides
    @Singleton
    fun provideAudioOutputManager(
        @ApplicationContext context: Context,
        externalScope: CoroutineScope
    ): AudioOutputManager {
        return AudioOutputManager(context, externalScope)
    }
}