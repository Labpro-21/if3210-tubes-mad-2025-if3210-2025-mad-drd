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
    const val PLAYER = "player"
    
    // Other routes
    const val ADD_SONG = "add_song"
    const val EDIT_SONG = "edit_song/{songId}"
    const val SETTINGS = "settings"
}