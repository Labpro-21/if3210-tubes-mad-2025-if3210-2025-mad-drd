package com.example.purrytify.data.remote.interceptor

import com.example.purrytify.data.local.datastore.TokenDataStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp interceptor that adds the JWT token to all requests
 * (except login and refresh token endpoints)
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenDataStore: TokenDataStore
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestUrl = originalRequest.url.toString()
        
        // Skip auth for login and refresh token endpoints
        if (requestUrl.contains("/api/login") || requestUrl.contains("/api/refresh-token")) {
            return chain.proceed(originalRequest)
        }
        
        // For other endpoints, add the token if available
        return runBlocking {
            val token = tokenDataStore.jwtToken.firstOrNull()
            
            if (token != null) {
                val newRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
                chain.proceed(newRequest)
            } else {
                // No token available, proceed with original request
                chain.proceed(originalRequest)
            }
        }
    }
}