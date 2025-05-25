package com.example.purrytify.data.repository

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for handling navigation events that need to be triggered from background services
 * or other components that don't have direct access to navigation controllers
 */
@Singleton
class NavigationEventRepository @Inject constructor() {
    
    private val TAG = "NavigationEventRepository"
    
    // SharedFlow for navigation events - uses replay 0 to avoid replaying events to new subscribers
    private val _navigationEvents = MutableSharedFlow<NavigationEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val navigationEvents: SharedFlow<NavigationEvent> = _navigationEvents.asSharedFlow()
    
    /**
     * Emit a logout event that requires navigation to login screen
     * This should be called when user needs to be logged out due to authentication failures
     */
    fun emitLogoutEvent(reason: LogoutReason) {
        Log.d(TAG, "Emitting logout event with reason: $reason")
        val success = _navigationEvents.tryEmit(NavigationEvent.Logout(reason))
        if (!success) {
            Log.w(TAG, "Failed to emit logout event - buffer might be full")
        } else {
            Log.d(TAG, "Successfully emitted logout event")
        }
    }
    
    /**
     * Emit a navigation event to a specific route
     */
    fun emitNavigationEvent(route: String) {
        Log.d(TAG, "Emitting navigation event to route: $route")
        val success = _navigationEvents.tryEmit(NavigationEvent.NavigateToRoute(route))
        if (!success) {
            Log.w(TAG, "Failed to emit navigation event - buffer might be full")
        } else {
            Log.d(TAG, "Successfully emitted navigation event to: $route")
        }
    }
}

/**
 * Sealed class representing different navigation events
 */
sealed class NavigationEvent {
    /**
     * Logout event that requires navigation to login screen
     */
    data class Logout(val reason: LogoutReason) : NavigationEvent()
    
    /**
     * Navigate to a specific route
     */
    data class NavigateToRoute(val route: String) : NavigationEvent()
}

/**
 * Enum representing different reasons for logout
 */
enum class LogoutReason {
    TOKEN_REFRESH_FAILED,
    USER_INITIATED,
    TOKEN_EXPIRED,
    INVALID_TOKEN,
    NETWORK_ERROR
}