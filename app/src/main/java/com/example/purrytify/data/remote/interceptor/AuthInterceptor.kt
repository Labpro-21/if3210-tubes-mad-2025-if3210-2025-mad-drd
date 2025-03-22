package com.example.purrytify.data.remote.interceptor

import com.example.purrytify.data.local.datastore.UserPreferences
import com.example.purrytify.util.Constants
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val userPreferences: UserPreferences
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Skip adding auth header for login and refresh token endpoints
        if (originalRequest.url.encodedPath.contains(Constants.API_LOGIN) ||
            originalRequest.url.encodedPath.contains(Constants.API_REFRESH_TOKEN)) {
            return chain.proceed(originalRequest)
        }
        
        // Get token from DataStore
        val accessToken = runBlocking {
            userPreferences.getAccessToken().first()
        }
        
        // Add auth header if token exists
        return if (accessToken.isNotEmpty()) {
            val newRequest = originalRequest.newBuilder()
                .header(Constants.AUTH_HEADER, "${Constants.BEARER_PREFIX}$accessToken")
                .build()
            chain.proceed(newRequest)
        } else {
            chain.proceed(originalRequest)
        }
    }
}