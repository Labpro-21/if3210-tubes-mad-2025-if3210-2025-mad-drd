package com.example.purrytify.service

import android.content.Context
import androidx.lifecycle.asFlow
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.purrytify.data.repository.AuthRepository
import com.example.purrytify.worker.TokenCheckWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service to manage JWT token validation and refresh
 */
@Singleton
class TokenService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val externalScope: CoroutineScope
) {
    // The JWT token expires after 5 minutes (300 seconds)
    // Schedule the check a bit earlier to have time for refresh
    private val TOKEN_CHECK_INTERVAL_MINUTES = 4L

    // Don't initialize in constructor - this will be null until onCreate of Application is called
    private val workManager by lazy { WorkManager.getInstance(context) }

    /**
     * Start the periodic token check worker
     */
    fun startTokenCheck() {
        // Only start if user is logged in
        externalScope.launch {
            if (!authRepository.isLoggedIn()) {
                return@launch
            }

            val tokenCheckRequest = PeriodicWorkRequestBuilder<TokenCheckWorker>(
                TOKEN_CHECK_INTERVAL_MINUTES, TimeUnit.MINUTES
            ).build()

            workManager.enqueueUniquePeriodicWork(
                TokenCheckWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                tokenCheckRequest
            )

            // Observe the worker to handle token refresh failures
            observeTokenWorker()
        }
    }

    /**
     * Stop the periodic token check worker
     */
    fun stopTokenCheck() {
        workManager.cancelUniqueWork(TokenCheckWorker.WORK_NAME)
    }

    /**
     * Observe the token worker to handle token refresh failures
     */
    private fun observeTokenWorker() {
        val workInfoLiveData = workManager.getWorkInfosForUniqueWorkLiveData(TokenCheckWorker.WORK_NAME)

        externalScope.launch {
            workInfoLiveData.asFlow().collectLatest { workInfoList ->
                workInfoList.forEach { workInfo ->
                    if (workInfo.state == WorkInfo.State.FAILED) {
                        // Token refresh failed, user needs to login again
                        Timber.d("JWT token refresh failed, logging out user")
                        authRepository.logout()

                        // Stop the worker since user is logged out
                        stopTokenCheck()
                    }
                }
            }
        }
    }
}