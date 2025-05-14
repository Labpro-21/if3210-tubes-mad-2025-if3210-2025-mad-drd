package com.example.purrytify.data.remote.api

import com.example.purrytify.data.remote.dto.response.ProfileResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.Part

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
    
    /**
     * Update the user's profile information
     * According to Milestone 2 spec:
     * PATCH {{baseUrl}}/api/profile with header "Authorization: Bearer {token}"
     * with Content-Type: multipart/form-data
     */
    @Multipart
    @PATCH("api/profile")
    suspend fun updateProfile(
        @Header("Authorization") authHeader: String,
        @Part location: MultipartBody.Part?,
        @Part profilePhoto: MultipartBody.Part?
    ): Response<ProfileResponse>
}