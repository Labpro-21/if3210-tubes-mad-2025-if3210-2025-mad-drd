package com.example.purrytify.data.remote.api

import com.example.purrytify.data.remote.dto.response.SongDetailDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Retrofit interface for fetching individual song details
 */
interface SongDetailApi {
    /**
     * Get song details by ID
     * Used for deep link handling when sharing songs
     */
    @GET("api/songs/{songId}")
    suspend fun getSongById(@Path("songId") songId: String): Response<SongDetailDto>
}