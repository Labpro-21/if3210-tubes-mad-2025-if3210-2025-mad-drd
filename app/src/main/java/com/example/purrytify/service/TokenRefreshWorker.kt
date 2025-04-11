package com.example.purrytify.service

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.purrytify.data.repository.AuthRepository
import com.example.purrytify.domain.auth.AuthStateManager
import com.example.purrytify.domain.util.Resource
import com.example.purrytify.util.Constants.TOKEN_REFRESH_INTERVAL_MINUTES
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlin.coroutines.cancellation.CancellationException

@HiltWorker
class TokenRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val authRepository: AuthRepository
) : CoroutineWorker(context, params) {

    init {
        Log.d("TokenRefreshWorker", "Worker started")
    }

    override suspend fun doWork(): Result {
        try {
            delay((TOKEN_REFRESH_INTERVAL_MINUTES) * 60 * 1000L) // 4 minutes
            Log.d("TokenRefreshWorker", "Woke up from delay")

            val isTokenValid = authRepository.verifyToken().first()
            Log.d("TokenRefreshWorker", "Token valid: $isTokenValid")

            if (!isTokenValid) {
                val result = authRepository.refreshToken()
                    .dropWhile { it is Resource.Loading }
                    .first()

                when (result) {
                    is Resource.Success -> {
                        Log.d("TokenRefreshWorker", "Token Refreshed")
                    }
                    is Resource.Error -> {
                        if (result.code == 401 || result.code == 403) {
                            Log.d("TokenRefreshWorker", "Token Refresh Failed")
                            authRepository.clearTokens()
                            AuthStateManager.triggerLogout()
                            return Result.failure()
                        }
                        Log.d("TokenRefreshWorker", "Token Refresh Failed - Retrying")
                        return Result.retry()
                    }
                    else -> {
                        Log.d("TokenRefreshWorker", "Unexpected state - Retrying")
                        return Result.retry()
                    }
                }
            }

            TokenRefreshScheduler.schedule(applicationContext)
            return Result.success()
        } catch (e: Exception) {

            if (e is CancellationException) throw e

            Log.e("TokenRefreshWorker", "Error in doWork()", e)
            TokenRefreshScheduler.schedule(applicationContext)
            return Result.retry()
        }
    }

}
