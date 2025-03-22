package com.example.purrytify.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.purrytify.data.repository.AuthRepository
import com.example.purrytify.domain.util.Resource
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class TokenRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val authRepository: AuthRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Check if token is valid
        val isTokenValid = authRepository.verifyToken().first()
        
        // If token is not valid, try to refresh it
        if (!isTokenValid) {
            val result = authRepository.refreshToken().first()
            
            return when (result) {
                is Resource.Success -> Result.success()
                is Resource.Error -> {
                    // If token refresh fails, user needs to log in again
                    authRepository.clearTokens()
                    Result.failure()
                }
                is Resource.Loading -> Result.retry()
            }
        }
        
        return Result.success()
    }
}