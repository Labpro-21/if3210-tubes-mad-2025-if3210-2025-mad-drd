package com.example.purrytify.ui.screens.library

import com.example.purrytify.domain.model.Song

/**
 * Sealed class representing the different states of the library screen UI
 */
sealed class LibraryUiState {
    /**
     * Loading state while songs are being fetched
     */
    data object Loading : LibraryUiState()

    /**
     * Empty state when no songs are available or no songs match the search query
     */
    data object Empty : LibraryUiState()

    /**
     * Success state with a list of songs
     */
    data class Success(val songs: List<Song>) : LibraryUiState()

    /**
     * Error state with an error message
     */
    data class Error(val message: String) : LibraryUiState()
}