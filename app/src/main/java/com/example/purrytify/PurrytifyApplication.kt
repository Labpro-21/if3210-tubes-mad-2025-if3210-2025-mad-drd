package com.example.purrytify

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.purrytify.service.TokenRefreshWorker
import com.example.purrytify.util.Constants
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class PurrytifyApplication : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    @Inject
    lateinit var workManager: WorkManager
    
    override fun onCreate() {
        super.onCreate()
        
        // Schedule the token refresh worker
        scheduleTokenRefreshWorker()
    }
    
    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }
    
    private fun scheduleTokenRefreshWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
            
        val tokenRefreshRequest = PeriodicWorkRequestBuilder<TokenRefreshWorker>(
            Constants.TOKEN_REFRESH_INTERVAL_MINUTES, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()
            
        workManager.enqueueUniquePeriodicWork(
            Constants.TOKEN_REFRESH_WORKER,
            ExistingPeriodicWorkPolicy.UPDATE,
            tokenRefreshRequest
        )
    }
}