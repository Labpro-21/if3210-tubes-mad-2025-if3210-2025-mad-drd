package com.example.purrytify.data.repository

import com.example.purrytify.data.local.datastore.TokenDataStore
import com.example.purrytify.data.local.datastore.UserPreferences
import com.example.purrytify.data.remote.api.ProfileApi
import com.example.purrytify.domain.model.User
import com.example.purrytify.domain.util.Result
import kotlinx.coroutines.flow.firstOrNull
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for user profile operations
 */
@Singleton
class ProfileRepository @Inject constructor(
    private val profileApi: ProfileApi,
    private val tokenDataStore: TokenDataStore,
    private val userPreferences: UserPreferences
) {
    /**
     * Get the current user's profile from the API
     */
    suspend fun getProfile(): Result<User> {
        return try {
            val authHeader = tokenDataStore.createAuthHeader()
                ?: return Result.Error(Exception("No JWT token found"))
                
            val response = profileApi.getProfile(authHeader)
            
            if (response.isSuccessful) {
                val user = response.body()?.toDomain()
                    ?: return Result.Error(IOException("Profile response body is null"))
                    
                // Cache user info
                userPreferences.saveUserInfo(user)
                
                Result.Success(user)
            } else {
                Result.Error(IOException("Profile fetch failed with code: ${response.code()}"))
            }
        } catch (e: HttpException) {
            Result.Error(e, "Network error: ${e.message}")
        } catch (e: IOException) {
            Result.Error(e, "IO error: ${e.message}")
        } catch (e: Exception) {
            Result.Error(e, "Unknown error: ${e.message}")
        }
    }
    
    /**
     * Get the cached user ID, if available
     */
    suspend fun getUserId(): Int? {
        return userPreferences.userId.firstOrNull()
    }
    
    /**
     * Get the cached user location, if available
     */
    suspend fun getUserLocation(): String? {
        return userPreferences.userLocation.firstOrNull()
    }
}