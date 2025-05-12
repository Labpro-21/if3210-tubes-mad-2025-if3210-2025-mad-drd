package com.example.purrytify.domain.util

/**
 * Generic wrapper class for API calls and data operations.
 * Provides a consistent way to handle success, error, and loading states.
 */
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable, val message: String = exception.localizedMessage ?: "An error occurred") : Result<Nothing>()
    data object Loading : Result<Nothing>()

    companion object {
        /**
         * Helper function to handle API or database operations and wrap them in Result.
         */
        suspend fun <T> execute(block: suspend () -> T): Result<T> {
            return try {
                Success(block())
            } catch (e: Exception) {
                Error(e)
            }
        }
    }
}