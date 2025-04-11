package com.example.purrytify.domain.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object AuthStateManager {
    private val _shouldLogout = MutableStateFlow(false)
    val shouldLogout = _shouldLogout.asStateFlow()

    fun triggerLogout() {
        _shouldLogout.value = true
    }

    fun clearLogout() {
        _shouldLogout.value = false
    }
}
