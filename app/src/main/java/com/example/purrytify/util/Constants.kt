package com.example.purrytify.util

object Constants {
    // API
    const val BASE_URL = "http://34.101.226.132:3000"
    const val API_LOGIN = "/api/login"
    const val API_REFRESH_TOKEN = "/api/refresh-token"
    const val API_VERIFY_TOKEN = "/api/verify-token"
    const val API_PROFILE = "/api/profile"
    
    // Auth
    const val AUTH_HEADER = "Authorization"
    const val BEARER_PREFIX = "Bearer "
    
    // Preferences
    const val USER_PREFERENCES = "user_preferences"
    const val KEY_ACCESS_TOKEN = "access_token"
    const val KEY_REFRESH_TOKEN = "refresh_token"
    
    // Workers
    const val TOKEN_REFRESH_WORKER = "token_refresh_worker"
    const val TOKEN_REFRESH_INTERVAL_MINUTES = 4L // Before the 5-min expiration
    
    // Database
    const val DATABASE_NAME = "purrytify_database"
    const val SONGS_TABLE = "songs"
    const val LIKED_SONGS_TABLE = "liked_songs"
    
    // Media
    const val MEDIA_ROOT_ID = "root_id"
    const val NOTIFICATION_CHANNEL_ID = "music_player_channel"
    const val NOTIFICATION_ID = 1
    
    // Intent Actions
    const val ACTION_PLAY = "com.example.purrytify.PLAY"
    const val ACTION_PAUSE = "com.example.purrytify.PAUSE"
    const val ACTION_PREVIOUS = "com.example.purrytify.PREVIOUS"
    const val ACTION_NEXT = "com.example.purrytify.NEXT"
    
    // Broadcast Actions
    const val ACTION_CONNECTIVITY_CHANGE = "com.example.purrytify.CONNECTIVITY_CHANGE"
    const val EXTRA_IS_CONNECTED = "is_connected"
}