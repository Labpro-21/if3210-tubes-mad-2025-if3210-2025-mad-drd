package com.example.purrytify.data.repository

import android.util.Log
import com.example.purrytify.data.remote.api.SongDetailApi
import com.example.purrytify.domain.model.OnlineSong
import com.example.purrytify.domain.util.Result
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for fetching individual song details from the API
 * Used primarily for deep link handling
 */
@Singleton
class SongDetailRepository @Inject constructor(
    private val songDetailApi: SongDetailApi
) {
    private val TAG = "SongDetailRepository"
    
    /**
     * Fetch song details by ID
     * @param songId The ID of the song to fetch
     * @return Result containing the OnlineSong or error
     */
    suspend fun getSongById(songId: String): Result<OnlineSong> {
        return try {
            Log.d(TAG, "Fetching song details for ID: $songId")
            
            val response = songDetailApi.getSongById(songId)
            
            if (response.isSuccessful) {
                val songDto = response.body()
                if (songDto != null) {
                    val song = songDto.toDomain()
                    Log.d(TAG, "Successfully fetched song: ${song.title} by ${song.artist}")
                    Result.Success(song)
                } else {
                    Log.e(TAG, "Response body is null for song ID: $songId")
                    Result.Error(IOException("Song data is empty"))
                }
            } else {
                Log.e(TAG, "Failed to fetch song. Response code: ${response.code()}")
                when (response.code()) {
                    404 -> Result.Error(IOException("Song not found"))
                    403 -> Result.Error(IOException("Access denied"))
                    500 -> Result.Error(IOException("Server error"))
                    else -> Result.Error(IOException("Failed to fetch song: ${response.code()}"))
                }
            }
        } catch (e: HttpException) {
            Log.e(TAG, "Network error fetching song: ${e.message}", e)
            Result.Error(e, "Network error: ${e.message}")
        } catch (e: IOException) {
            Log.e(TAG, "IO error fetching song: ${e.message}", e)
            Result.Error(e, "Connection error: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error fetching song: ${e.message}", e)
            Result.Error(e, "Unexpected error: ${e.message}")
        }
    }
    
    /**
     * Check if a song exists by ID (lightweight check)
     * @param songId The ID of the song to check
     * @return Result containing true if song exists, false otherwise
     */
    suspend fun songExists(songId: String): Result<Boolean> {
        return when (val result = getSongById(songId)) {
            is Result.Success -> Result.Success(true)
            is Result.Error -> {
                // If it's a 404, the song doesn't exist
                if (result.message.contains("not found", ignoreCase = true)) {
                    Result.Success(false)
                } else {
                    // Other errors should be propagated
                    Result.Error(result.exception, result.message)
                }
            }
            is Result.Loading -> Result.Loading
        }
    }
}