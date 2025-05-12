package com.example.purrytify.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.purrytify.data.repository.AuthRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker to periodically check JWT token validity
 */
@HiltWorker
class TokenCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val authRepository: AuthRepository
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Check if user is logged in
            if (!authRepository.isLoggedIn()) {
                return@withContext Result.success()
            }
            
            // Verify token and attempt refresh if expired
            val result = authRepository.verifyToken()
            
            return@withContext when (result) {
                is com.example.purrytify.domain.util.Result.Success -> {
                    // Token is valid or was successfully refreshed
                    Result.success()
                }
                is com.example.purrytify.domain.util.Result.Error -> {
                    // Token refresh failed, user needs to login again
                    // We'll handle this through our TokenService, which will observe this worker
                    Result.failure()
                }
                is com.example.purrytify.domain.util.Result.Loading -> {
                    // Shouldn't happen, but retry if it does
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
    
    companion object {
        const val WORK_NAME = "token_check_worker"
    }
}