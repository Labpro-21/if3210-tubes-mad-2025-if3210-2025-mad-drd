package com.example.purrytify.data.remote.dto.response

import com.google.gson.annotations.SerializedName

/**
 * Data class representing the login response from the API
 */
data class LoginResponse(
    @SerializedName("token")
    val token: String,
    
    @SerializedName("refreshToken")
    val refreshToken: String
)