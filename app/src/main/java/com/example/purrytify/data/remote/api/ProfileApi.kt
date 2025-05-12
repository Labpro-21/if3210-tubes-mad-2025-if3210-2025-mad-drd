package com.example.purrytify.data.remote.api

import com.example.purrytify.data.remote.dto.response.ProfileResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

/**
 * Retrofit interface for profile-related API calls
 */
interface ProfileApi {
    /**
     * Get the user's profile information
     */
    @GET("api/profile")
    suspend fun getProfile(
        @Header("Authorization") authHeader: String
    ): Response<ProfileResponse>
}