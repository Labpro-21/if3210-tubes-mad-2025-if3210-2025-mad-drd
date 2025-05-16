package com.example.purrytify.di

import com.example.purrytify.domain.player.PlayerBridge
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Module for providing player-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {
    
    /**
     * Provide the PlayerBridge as a singleton
     */
    @Provides
    @Singleton
    fun providePlayerBridge(): PlayerBridge {
        return PlayerBridge()
    }
}