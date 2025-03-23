package com.example.purrytify.data.remote.api

import com.example.purrytify.data.remote.dto.request.LoginRequest
import com.example.purrytify.data.remote.dto.request.RefreshTokenRequest
import com.example.purrytify.data.remote.dto.request.UpdateProfileRequest
import com.example.purrytify.data.remote.dto.response.LoginResponse
import com.example.purrytify.data.remote.dto.response.MessageResponse
import com.example.purrytify.data.remote.dto.response.ProfileResponse
import com.example.purrytify.data.remote.dto.response.TokenResponse
import com.example.purrytify.util.Constants
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part

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
    
    @PUT(Constants.API_PROFILE)
    suspend fun updateProfile(
        @Body updateProfileRequest: UpdateProfileRequest
    ): Response<MessageResponse>
    
    @Multipart
    @PUT(Constants.API_PROFILE)
    suspend fun updateProfilePicture(
        @Part profilePhoto: MultipartBody.Part
    ): Response<MessageResponse>
}