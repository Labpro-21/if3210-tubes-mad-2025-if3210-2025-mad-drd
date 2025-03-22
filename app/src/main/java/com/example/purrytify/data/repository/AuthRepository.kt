package com.example.purrytify.data.repository

import com.example.purrytify.data.local.datastore.UserPreferences
import com.example.purrytify.data.remote.api.PurrytifyApi
import com.example.purrytify.data.remote.dto.request.LoginRequest
import com.example.purrytify.data.remote.dto.request.RefreshTokenRequest
import com.example.purrytify.domain.util.Resource
import com.example.purrytify.util.NetworkUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: PurrytifyApi,
    private val userPreferences: UserPreferences,
    private val networkUtils: NetworkUtils
) {
    // Login user and save tokens
    fun login(email: String, password: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading)

        val response = networkUtils.safeApiCall {
            api.login(LoginRequest(email, password))
        }

        when (response) {
            is Resource.Success -> {
                userPreferences.saveAccessToken(response.data.accessToken)
                userPreferences.saveRefreshToken(response.data.refreshToken)
                emit(Resource.Success(true))
            }
            is Resource.Error -> {
                emit(Resource.Error(response.message))
            }
            is Resource.Loading -> {
                // Already emitted loading state
            }
        }
    }

    // Verify if token is valid
    fun verifyToken(): Flow<Boolean> = flow {
        val response = networkUtils.safeApiCall {
            api.verifyToken()
        }

        emit(response is Resource.Success)
    }

    // Refresh token
    fun refreshToken(): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading)

        val refreshToken = userPreferences.getRefreshToken().first()

        if (refreshToken.isEmpty()) {
            emit(Resource.Error("No refresh token available"))
            return@flow
        }

        val response = networkUtils.safeApiCall {
            api.refreshToken(RefreshTokenRequest(refreshToken))
        }

        when (response) {
            is Resource.Success -> {
                userPreferences.saveAccessToken(response.data.accessToken)
                userPreferences.saveRefreshToken(response.data.refreshToken)
                emit(Resource.Success(true))
            }
            is Resource.Error -> {
                emit(Resource.Error(response.message))
            }
            is Resource.Loading -> {
                // Already emitted loading state
            }
        }
    }

    // Logout - clear tokens
    suspend fun clearTokens() {
        userPreferences.clearTokens()
    }

    // Get access token
    fun getAccessToken(): Flow<String> {
        return userPreferences.getAccessToken()
    }
}