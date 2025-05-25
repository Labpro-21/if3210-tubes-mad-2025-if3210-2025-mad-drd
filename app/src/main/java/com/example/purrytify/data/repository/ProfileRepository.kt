package com.example.purrytify.data.repository

import android.util.Log
import com.example.purrytify.data.local.dao.SongDao
import com.example.purrytify.data.local.datastore.TokenDataStore
import com.example.purrytify.data.local.datastore.UserPreferences
import com.example.purrytify.data.remote.api.ProfileApi
import com.example.purrytify.domain.model.Profile
import com.example.purrytify.domain.model.User
import com.example.purrytify.domain.util.Result
import kotlinx.coroutines.flow.firstOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for profile-related operations
 */
@Singleton
class ProfileRepository @Inject constructor(
    private val profileApi: ProfileApi,
    private val tokenDataStore: TokenDataStore,
    private val userPreferences: UserPreferences,
    private val songDao: SongDao
) {
    private val TAG = "ProfileRepository"
    private val BASE_URL = "http://34.101.226.132:3000/"

    /**
     * Get the user's profile
     */
    suspend fun getProfile(): Result<Profile> {
        return try {
            val authHeader = tokenDataStore.createAuthHeader()
                ?: return Result.Error(Exception("No JWT token found"))
                
            val response = profileApi.getProfile(authHeader)
            
            if (response.isSuccessful) {
                val profileResponse = response.body()
                    ?: return Result.Error(IOException("Profile response body is null"))
                
                val profilePhotoUrl = BASE_URL + "uploads/profile-picture/" + profileResponse.profilePhoto
                
                val profile = Profile(
                    id = profileResponse.id,
                    username = profileResponse.username,
                    email = profileResponse.email,
                    profilePhoto = profileResponse.profilePhoto,
                    profilePhotoUrl = profilePhotoUrl,
                    location = profileResponse.location
                )
                
                // Cache user info
                userPreferences.saveUserInfo(profileResponse.toDomain())
                
                Result.Success(profile)
            } else {
                Result.Error(IOException("Profile fetch failed with code: ${response.code()}"))
            }
        } catch (e: HttpException) {
            Log.e(TAG, "Network error: ${e.message}", e)
            Result.Error(e, "Network error: ${e.message}")
        } catch (e: IOException) {
            Log.e(TAG, "IO error: ${e.message}", e)
            Result.Error(e, "IO error: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Unknown error: ${e.message}", e)
            Result.Error(e, "Unknown error: ${e.message}")
        }
    }
    
    /**
     * Update the user's profile
     * Can update location, profile photo, or both
     */
    suspend fun updateProfile(location: String?, profilePhotoFile: File?): Result<Unit> {
        return try {
            val authHeader = tokenDataStore.createAuthHeader()
                ?: return Result.Error(Exception("No JWT token found"))
            
            // Create multipart request parts
            val locationPart = if (location != null) {
                MultipartBody.Part.createFormData("location", location)
            } else null
            
            val profilePhotoPart = if (profilePhotoFile != null) {
                val requestFile = profilePhotoFile.asRequestBody("image/*".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("profilePhoto", profilePhotoFile.name, requestFile)
            } else null
            
            // Make sure at least one part is provided
            if (locationPart == null && profilePhotoPart == null) {
                return Result.Error(Exception("No updates provided"))
            }
            
            val response = profileApi.updateProfile(
                authHeader = authHeader,
                location = locationPart,
                profilePhoto = profilePhotoPart
            )
            
            if (response.isSuccessful) {
                // The server returns a success message rather than the updated profile
                // We'll return success here and let the caller refresh the profile if needed
                Result.Success(Unit)
            } else {
                Result.Error(IOException("Profile update failed with code: ${response.code()}"))
            }
        } catch (e: HttpException) {
            Log.e(TAG, "Network error: ${e.message}", e)
            Result.Error(e, "Network error: ${e.message}")
        } catch (e: IOException) {
            Log.e(TAG, "IO error: ${e.message}", e)
            Result.Error(e, "IO error: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Unknown error: ${e.message}", e)
            Result.Error(e, "Unknown error: ${e.message}")
        }
    }
    
    /**
     * Get song statistics for the user
     * @return Triple of (song count, liked songs count, listened songs count)
     */
    suspend fun getSongStats(userId: Int): Triple<Int, Int, Int> {
        try {
            val totalSongs = songDao.getSongCount(userId)
            val likedSongs = songDao.getLikedSongCount(userId)
            val listenedSongs = songDao.getListenedSongCount(userId)
            
            return Triple(totalSongs, likedSongs, listenedSongs)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting song stats: ${e.message}", e)
            return Triple(0, 0, 0)
        }
    }
}