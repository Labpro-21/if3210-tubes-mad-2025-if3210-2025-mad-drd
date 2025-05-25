package com.example.purrytify.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.purrytify.data.repository.AuthRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker to periodically refresh JWT token
 */
@HiltWorker
class TokenRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val authRepository: AuthRepository
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Check if user is logged in
            if (!authRepository.isLoggedIn()) {
                Log.d(TAG, "User not logged in, skipping token refresh")
                return@withContext Result.success()
            }
            
            // Attempt to refresh the token without checking if it's valid first
            Log.d(TAG, "Automatically refreshing token")
            
            val result = authRepository.refreshToken()
            
            return@withContext when (result) {
                is com.example.purrytify.domain.util.Result.Success -> {
                    Log.d(TAG, "Token refresh successful")
                    Result.success()
                }
                is com.example.purrytify.domain.util.Result.Error -> {
                    Log.e(TAG, "Token refresh failed: ${result.message}")
                    // Token refresh failed, user needs to login again
                    Result.failure()
                }
                is com.example.purrytify.domain.util.Result.Loading -> {
                    // Shouldn't happen, but retry if it does
                    Log.w(TAG, "Token refresh returned Loading state, retrying")
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing token: ${e.message}", e)
            Result.retry()
        }
    }
    
    companion object {
        const val WORK_NAME = "token_refresh_worker"
        private const val TAG = "TokenRefreshWorker"
    }
}