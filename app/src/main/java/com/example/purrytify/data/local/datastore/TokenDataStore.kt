package com.example.purrytify.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.purrytify.di.TokenDataStoreQualifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages secure storage of authentication tokens
 * In a production app, should consider using EncryptedSharedPreferences or
 * a more secure storage mechanism for tokens
 */
@Singleton
class TokenDataStore @Inject constructor(
    @TokenDataStoreQualifier private val dataStore: DataStore<Preferences>
) {
    // Keys for the datastore
    private object PreferencesKeys {
        val JWT_TOKEN = stringPreferencesKey("jwt_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    }

    /**
     * Get the JWT access token
     */
    val jwtToken: Flow<String?> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.JWT_TOKEN]
    }

    /**
     * Get the refresh token
     */
    val refreshToken: Flow<String?> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.REFRESH_TOKEN]
    }

    /**
     * Save both JWT and refresh tokens
     */
    suspend fun saveTokens(jwt: String, refreshToken: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.JWT_TOKEN] = jwt
            preferences[PreferencesKeys.REFRESH_TOKEN] = refreshToken
        }
    }

    /**
     * Update just the JWT token (after refresh)
     */
    suspend fun updateJwtToken(jwt: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.JWT_TOKEN] = jwt
        }
    }

    /**
     * Clear all tokens on logout
     */
    suspend fun clearTokens() {
        dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.JWT_TOKEN)
            preferences.remove(PreferencesKeys.REFRESH_TOKEN)
        }
    }
    
    /**
     * Create the auth header string with the JWT token
     */
    suspend fun createAuthHeader(): String? {
        val token = dataStore.data.map { preferences ->
            preferences[PreferencesKeys.JWT_TOKEN]
        }.map { token ->
            token?.let { "Bearer $it" }
        }
        
        return token.first()
    }
}