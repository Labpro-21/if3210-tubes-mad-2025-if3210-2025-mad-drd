package com.example.purrytify.data.repository

import com.example.purrytify.data.remote.api.PurrytifyApi
import com.example.purrytify.data.remote.dto.request.UpdateProfileRequest
import com.example.purrytify.domain.model.Profile
import com.example.purrytify.domain.model.toDomain
import com.example.purrytify.domain.util.Resource
import com.example.purrytify.util.NetworkUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val api: PurrytifyApi,
    private val networkUtils: NetworkUtils
) {
    // Get user profile
    fun getProfile(): Flow<Resource<Profile>> = flow {
        emit(Resource.Loading)

        // Check network availability
        val isNetworkAvailable = networkUtils.isNetworkAvailable.first()
        if (!isNetworkAvailable) {
            emit(Resource.Error("No internet connection"))
            return@flow
        }

        val response = networkUtils.safeApiCall {
            api.getProfile()
        }

        when (response) {
            is Resource.Success -> {
                emit(Resource.Success(response.data.toDomain()))
            }
            is Resource.Error -> {
                emit(Resource.Error(response.message))
            }
            is Resource.Loading -> {
                // Already emitted loading state
            }
        }
    }
    
    // Update user profile
    fun updateProfile(username: String, email: String, location: String): Flow<Resource<String>> = flow {
        emit(Resource.Loading)
        
        // Check network availability
        val isNetworkAvailable = networkUtils.isNetworkAvailable.first()
        if (!isNetworkAvailable) {
            emit(Resource.Error("No internet connection"))
            return@flow
        }
        
        val updateRequest = UpdateProfileRequest(username, email, location)
        val response = networkUtils.safeApiCall {
            api.updateProfile(updateRequest)
        }
        
        when (response) {
            is Resource.Success -> {
                emit(Resource.Success(response.data.message))
            }
            is Resource.Error -> {
                emit(Resource.Error(response.message))
            }
            is Resource.Loading -> {
                // Already emitted loading state
            }
        }
    }
    
    // Update profile picture
    fun updateProfilePicture(file: File): Flow<Resource<String>> = flow {
        emit(Resource.Loading)
        
        // Check network availability
        val isNetworkAvailable = networkUtils.isNetworkAvailable.first()
        if (!isNetworkAvailable) {
            emit(Resource.Error("No internet connection"))
            return@flow
        }
        
        // Create multipart request
        val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("profilePhoto", file.name, requestBody)
        
        val response = networkUtils.safeApiCall {
            api.updateProfilePicture(part)
        }
        
        when (response) {
            is Resource.Success -> {
                emit(Resource.Success(response.data.message))
            }
            is Resource.Error -> {
                emit(Resource.Error(response.message))
            }
            is Resource.Loading -> {
                // Already emitted loading state
            }
        }
    }
}