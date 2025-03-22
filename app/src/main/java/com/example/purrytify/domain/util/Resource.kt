package com.example.purrytify.domain.util

sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String, val code: Int? = null) : Resource<Nothing>()
    object Loading : Resource<Nothing>()
    
    val isLoading get() = this is Loading
    val isSuccess get() = this is Success
    val isError get() = this is Error
    
    fun handle(
        onSuccess: (T) -> Unit = {},
        onError: (String) -> Unit = {},
        onLoading: () -> Unit = {}
    ) {
        when (this) {
            is Success -> onSuccess(data)
            is Error -> onError(message)
            is Loading -> onLoading()
        }
    }
}
