package com.example.purrytify.data.remote.api

import com.example.purrytify.data.remote.dto.request.LoginRequest
import com.example.purrytify.data.remote.dto.request.RefreshTokenRequest
import com.example.purrytify.data.remote.dto.response.LoginResponse
import com.example.purrytify.data.remote.dto.response.ProfileResponse
import com.example.purrytify.data.remote.dto.response.TokenResponse
import com.example.purrytify.util.Constants
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface PurrytifyApi {
    
    @POST(Constants.API_LOGIN)
    suspend fun login(
        @Body loginRequest: LoginRequest
    ): Response<LoginResponse>
    
    @POST(Constants.API_REFRESH_TOKEN)
    suspend fun refreshToken(
        @Body refreshTokenRequest: RefreshTokenRequest
    ): Response<TokenResponse>
    
    @GET(Constants.API_VERIFY_TOKEN)
    suspend fun verifyToken(): Response<Unit>
    
    @GET(Constants.API_PROFILE)
    suspend fun getProfile(): Response<ProfileResponse>
}