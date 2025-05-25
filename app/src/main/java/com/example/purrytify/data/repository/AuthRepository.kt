package com.example.purrytify.data.repository

import com.example.purrytify.data.local.datastore.TokenDataStore
import com.example.purrytify.data.local.datastore.UserPreferences
import com.example.purrytify.data.remote.api.AuthApi
import com.example.purrytify.data.remote.api.ProfileApi
import com.example.purrytify.data.remote.dto.request.LoginRequest
import com.example.purrytify.data.remote.dto.request.RefreshTokenRequest
import com.example.purrytify.domain.model.User
import com.example.purrytify.domain.util.Result
import kotlinx.coroutines.flow.firstOrNull
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for authentication-related operations
 */
@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val profileApi: ProfileApi,
    private val tokenDataStore: TokenDataStore,
    private val userPreferences: UserPreferences
) {
    /**
     * Login a user with email and password
     */
    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val loginRequest = LoginRequest(email, password)
            val loginResponse = authApi.login(loginRequest)
            
            if (!loginResponse.isSuccessful) {
                // Login failed with error code
                return when(loginResponse.code()) {
                    401, 400 -> Result.Error(IOException("Invalid username or password"))
                    else -> Result.Error(IOException("Login failed with code: ${loginResponse.code()}"))
                }
            }
            
            // Store tokens
            val tokenData = loginResponse.body()
                ?: return Result.Error(IOException("Login response body is null"))
                
            tokenDataStore.saveTokens(
                jwt = tokenData.token,
                refreshToken = tokenData.refreshToken
            )
            
            // Use the token directly from the response for the profile request
            val authHeader = "Bearer ${tokenData.token}"
            val profileResponse = profileApi.getProfile(authHeader)
            
            if (!profileResponse.isSuccessful) {
                return Result.Error(
                    IOException("Profile fetch failed with code: ${profileResponse.code()}")
                )
            }
            
            val user = profileResponse.body()?.toDomain()
                ?: return Result.Error(IOException("Profile response body is null"))
                
            // Save user info to preferences
            userPreferences.saveUserInfo(user)
            
            Result.Success(user)
        } catch (e: HttpException) {
            Result.Error(e, "Network error: ${e.message}")
        } catch (e: IOException) {
            Result.Error(e, "IO error: ${e.message}")
        } catch (e: Exception) {
            Result.Error(e, "Unknown error: ${e.message}")
        }
    }
    
    /**
     * Check if user is logged in (has valid tokens)
     */
    suspend fun isLoggedIn(): Boolean {
        val token = tokenDataStore.jwtToken.firstOrNull()
        return !token.isNullOrEmpty()
    }
    
    /**
     * Logout the current user
     */
    suspend fun logout() {
        tokenDataStore.clearTokens()
        userPreferences.clearUserData()
    }
    
    /**
     * Verify if the current JWT token is valid
     */
    suspend fun verifyToken(): Result<Boolean> {
        return try {
            val token = tokenDataStore.jwtToken.firstOrNull()
                ?: return Result.Error(Exception("No JWT token found"))
                
            val authHeader = "Bearer $token"
            val response = authApi.verifyToken(authHeader)
            
            if (response.isSuccessful) {
                Result.Success(true)
            } else if (response.code() == 403) {
                // Token expired, try to refresh
                refreshToken()
            } else {
                Result.Error(Exception("Failed to verify token: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.Error(e, "Error verifying token: ${e.message}")
        }
    }
    
    /**
     * Refresh the JWT token using the refresh token
     */
    suspend fun refreshToken(): Result<Boolean> {
        return try {
            val refreshToken = tokenDataStore.refreshToken.firstOrNull()
                ?: return Result.Error(Exception("No refresh token found"))
                
            val request = RefreshTokenRequest(refreshToken)
            val response = authApi.refreshToken(request)
            
            if (response.isSuccessful) {
                val tokenData = response.body()
                    ?: return Result.Error(IOException("Refresh token response body is null"))
                    
                // Update tokens
                tokenDataStore.saveTokens(
                    jwt = tokenData.token,
                    refreshToken = tokenData.refreshToken
                )
                
                Result.Success(true)
            } else {
                // If refresh fails, user needs to login again
                Result.Error(Exception("Failed to refresh token: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.Error(e, "Error refreshing token: ${e.message}")
        }
    }
}