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
        // Try to verify token first
        try {
            val isTokenValid = authRepository.verifyToken().first()
            
            // If token is not valid or verification fails, refresh it
            if (!isTokenValid) {
                val result = authRepository.refreshToken().first()
                
                return when (result) {
                    is Resource.Success -> {
                        // Token refreshed successfully
                        Result.success()
                    }
                    is Resource.Error -> {
                        // Only clear tokens if token refresh explicitly fails 
                        if (result.code == 401 || result.code == 403) {
                            authRepository.clearTokens()
                            Result.failure()
                        } else {
                            // For network errors, retry
                            Result.retry()
                        }
                    }
                    is Resource.Loading -> Result.retry()
                }
            }
            
            return Result.success()
        } catch (e: Exception) {
            // If any error occurs (like network error), retry
            return Result.retry()
        }
    }
}