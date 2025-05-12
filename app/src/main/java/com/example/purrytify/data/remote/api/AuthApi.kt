package com.example.purrytify.data.remote.api

import com.example.purrytify.data.remote.dto.request.LoginRequest
import com.example.purrytify.data.remote.dto.request.RefreshTokenRequest
import com.example.purrytify.data.remote.dto.response.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit interface for authentication-related API calls
 */
interface AuthApi {
    /**
     * Log in a user with email and password
     */
    @POST("api/login")
    suspend fun login(
        @Body loginRequest: LoginRequest
    ): Response<LoginResponse>
    
    /**
     * Refresh the JWT token using a refresh token
     */
    @POST("api/refresh-token")
    suspend fun refreshToken(
        @Body refreshTokenRequest: RefreshTokenRequest
    ): Response<LoginResponse>
    
    /**
     * Verify if the current JWT token is valid
     * Returns 200 OK if valid, 403 Forbidden if expired
     */
    @GET("api/verify-token")
    suspend fun verifyToken(
        @Header("Authorization") authHeader: String
    ): Response<Unit>
}