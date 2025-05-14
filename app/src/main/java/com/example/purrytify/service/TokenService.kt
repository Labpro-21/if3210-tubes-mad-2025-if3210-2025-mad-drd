package com.example.purrytify.service

import android.content.Context
import android.util.Log
import androidx.lifecycle.asFlow
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.purrytify.data.repository.AuthRepository
import com.example.purrytify.worker.TokenRefreshWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service to manage JWT token refresh
 */
@Singleton
class TokenService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val externalScope: CoroutineScope
) {
    // The JWT token expires after 5 minutes (300 seconds)
    // Schedule the refresh a bit earlier to ensure token is always valid
    private val TOKEN_REFRESH_INTERVAL_MINUTES = 1L

    // Don't initialize in constructor - this will be null until onCreate of Application is called
    private val workManager by lazy { WorkManager.getInstance(context) }

    /**
     * Start the token refresh worker
     */
    fun startTokenCheck() {
        // Schedule the first token refresh immediately
        scheduleTokenRefresh(0L)

        // Observe the worker to handle token refresh failures and reschedule successful refreshes
        observeTokenWorker()
        
        Log.d(TAG, "Started token refresh service")
    }

    /**
     * Stop the token refresh worker
     */
    fun stopTokenCheck() {
        workManager.cancelUniqueWork(TokenRefreshWorker.WORK_NAME)
        Log.d(TAG, "Stopped token refresh service")
    }

    /**
     * Schedule a one-time token refresh work request
     * @param delayMinutes Delay in minutes before executing the work request
     */
    private fun scheduleTokenRefresh(delayMinutes: Long) {
        Log.d(TAG, "Scheduling token refresh in $delayMinutes minutes")
        
        val tokenRefreshRequest = OneTimeWorkRequestBuilder<TokenRefreshWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniqueWork(
            TokenRefreshWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            tokenRefreshRequest
        )
    }

    /**
     * Observe the token worker to handle token refresh failures and reschedule successful refreshes
     */
    private fun observeTokenWorker() {
        val workInfoLiveData = workManager.getWorkInfosForUniqueWorkLiveData(TokenRefreshWorker.WORK_NAME)

        externalScope.launch {
            workInfoLiveData.asFlow()
                .filter { workInfoList -> workInfoList.isNotEmpty() }
                .collectLatest { workInfoList ->
                    val workInfo = workInfoList.firstOrNull() ?: return@collectLatest
                    
                    when (workInfo.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            Log.d(TAG, "Token refresh succeeded, scheduling next refresh")
                            // Token refresh succeeded, schedule the next refresh
                            scheduleTokenRefresh(TOKEN_REFRESH_INTERVAL_MINUTES)
                        }
                        WorkInfo.State.FAILED -> {
                            // Token refresh failed, user needs to login again
                            Log.d(TAG, "Token refresh failed, logging out user")
                            authRepository.logout()

                            // Stop the worker since user is logged out
                            stopTokenCheck()
                        }
                        else -> {
                            // Other states like ENQUEUED, RUNNING, BLOCKED, CANCELLED
                            // These are intermediate states, no action needed
                        }
                    }
                }
        }
    }

    companion object {
        private const val TAG = "TokenService"
    }
}