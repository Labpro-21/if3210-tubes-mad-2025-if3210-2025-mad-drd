package com.example.purrytify

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.example.purrytify.service.TokenService
import com.example.purrytify.util.NetworkManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

@HiltAndroidApp
class PurrytifyApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var tokenService: TokenService

    @Inject
    lateinit var networkManager: NetworkManager

    @Inject
    lateinit var externalScope: CoroutineScope

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    // We need to avoid accessing workerFactory before it's initialized
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Ensure WorkManager is initialized after Hilt injections
        // This delays the creation of WorkManager until after workerFactory is initialized
        WorkManager.initialize(this, workManagerConfiguration)

        // Start network monitoring using NetworkCallback API
        networkManager.startObservingNetwork()

        // Start token check service - now WorkManager is properly initialized
        tokenService.startTokenCheck()

        Log.d("PurrytifyApp", "Purrytify application started")
    }

    override fun onTerminate() {
        super.onTerminate()

        // Stop network monitoring
        networkManager.stopObservingNetwork()

        // Stop token check service
        tokenService.stopTokenCheck()
    }
}