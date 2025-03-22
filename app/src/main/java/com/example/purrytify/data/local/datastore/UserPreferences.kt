package com.example.purrytify.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.purrytify.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = Constants.USER_PREFERENCES
)

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.userPreferencesDataStore
    
    companion object {
        private val ACCESS_TOKEN = stringPreferencesKey(Constants.KEY_ACCESS_TOKEN)
        private val REFRESH_TOKEN = stringPreferencesKey(Constants.KEY_REFRESH_TOKEN)
    }
    
    // Access Token
    suspend fun saveAccessToken(token: String) {
        dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN] = token
        }
    }
    
    fun getAccessToken(): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[ACCESS_TOKEN] ?: ""
        }
    }
    
    // Refresh Token
    suspend fun saveRefreshToken(token: String) {
        dataStore.edit { preferences ->
            preferences[REFRESH_TOKEN] = token
        }
    }
    
    fun getRefreshToken(): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[REFRESH_TOKEN] ?: ""
        }
    }
    
    // Clear tokens on logout
    suspend fun clearTokens() {
        dataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN)
            preferences.remove(REFRESH_TOKEN)
        }
    }
}