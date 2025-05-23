package com.example.purrytify.ui.navigation

/**
 * Contains all navigation routes for the app
 */
object Routes {
    // Auth routes
    const val LOGIN = "login"
    
    // Main routes
    const val HOME = "home"
    const val LIBRARY = "library"
    const val PROFILE = "profile"
    
    // Audio player routes
    const val PLAYER = "player/{songId}"
    
    // Top Songs routes
    const val TOP_SONGS_GLOBAL = "topsongs/global"
    const val TOP_SONGS_COUNTRY = "topsongs/country"
    
    // Playlist routes
    const val DAILY_PLAYLIST = "playlist/daily"
    
    // QR Scanner route
    const val QR_SCANNER = "qr_scanner"
    
    // Other routes
    const val ADD_SONG = "add_song"
    const val EDIT_SONG = "edit_song/{songId}"
    const val SETTINGS = "settings"
}