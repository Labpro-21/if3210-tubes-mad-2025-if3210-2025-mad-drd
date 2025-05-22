package com.example.purrytify.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.purrytify.di.UserPreferencesDataStoreQualifier
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
    @UserPreferencesDataStoreQualifier private val dataStore: DataStore<Preferences>
) {
    // Keys for the datastore
    private object PreferencesKeys {
        val USER_ID = intPreferencesKey("user_id")
        val USERNAME = stringPreferencesKey("username")
        val EMAIL = stringPreferencesKey("email")
        val LOCATION = stringPreferencesKey("location")
        val DAILY_PLAYLIST_DATE = stringPreferencesKey("daily_playlist_date")
        val DAILY_PLAYLIST_JSON = stringPreferencesKey("daily_playlist_json")
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
     * Get the last daily playlist generation date
     */
    val dailyPlaylistDate: Flow<String?> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DAILY_PLAYLIST_DATE]
    }

    /**
     * Get the cached daily playlist as JSON string
     */
    val dailyPlaylistJson: Flow<String?> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DAILY_PLAYLIST_JSON]
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
     * Save daily playlist generation info
     */
    suspend fun saveDailyPlaylistInfo(date: String, playlistJson: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DAILY_PLAYLIST_DATE] = date
            preferences[PreferencesKeys.DAILY_PLAYLIST_JSON] = playlistJson
        }
    }

    /**
     * Clear daily playlist cache (force regeneration)
     */
    suspend fun clearDailyPlaylistCache() {
        dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.DAILY_PLAYLIST_DATE)
            preferences.remove(PreferencesKeys.DAILY_PLAYLIST_JSON)
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
            preferences.remove(PreferencesKeys.DAILY_PLAYLIST_DATE)
            preferences.remove(PreferencesKeys.DAILY_PLAYLIST_JSON)
        }
    }
}