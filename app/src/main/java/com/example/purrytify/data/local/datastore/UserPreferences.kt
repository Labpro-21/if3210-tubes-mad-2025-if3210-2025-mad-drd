package com.example.purrytify.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.purrytify.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages non-sensitive user preferences and cached user info
 */
@Singleton
class UserPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    // Keys for the datastore
    private object PreferencesKeys {
        val USER_ID = intPreferencesKey("user_id")
        val USERNAME = stringPreferencesKey("username")
        val EMAIL = stringPreferencesKey("email")
        val LOCATION = stringPreferencesKey("location")
    }

    /**
     * Get the cached user ID
     */
    val userId: Flow<Int?> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.USER_ID]
    }

    /**
     * Get the cached user email
     */
    val userEmail: Flow<String?> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.EMAIL]
    }

    /**
     * Get the cached location (country code)
     */
    val userLocation: Flow<String?> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LOCATION]
    }

    /**
     * Save user info after login/profile update
     */
    suspend fun saveUserInfo(user: User) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_ID] = user.id
            preferences[PreferencesKeys.USERNAME] = user.username
            preferences[PreferencesKeys.EMAIL] = user.email
            preferences[PreferencesKeys.LOCATION] = user.location
        }
    }

    /**
     * Update just the user's location
     */
    suspend fun updateLocation(location: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LOCATION] = location
        }
    }

    /**
     * Clear all user data on logout
     */
    suspend fun clearUserData() {
        dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.USER_ID)
            preferences.remove(PreferencesKeys.USERNAME)
            preferences.remove(PreferencesKeys.EMAIL)
            preferences.remove(PreferencesKeys.LOCATION)
        }
    }
}