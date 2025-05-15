package com.example.purrytify.data.remote.api

import com.example.purrytify.data.remote.dto.response.TopSongDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Retrofit interface for top songs related API calls
 */
interface TopSongsApi {
    /**
     * Get global top 50 songs
     */
    @GET("api/top-songs/global")
    suspend fun getGlobalTopSongs(): Response<List<TopSongDto>>
    
    /**
     * Get top 10 songs by country code
     * Note: Only available for ID, MY, US, GB, CH, DE, and BR
     */
    @GET("api/top-songs/{countryCode}")
    suspend fun getCountryTopSongs(@Path("countryCode") countryCode: String): Response<List<TopSongDto>>
}