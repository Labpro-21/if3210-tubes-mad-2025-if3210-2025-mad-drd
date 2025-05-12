package com.example.purrytify.util

import android.content.Context
import android.widget.Toast
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager class to track network connectivity and notify UI
 */
@Singleton
class NetworkManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val externalScope: CoroutineScope
) {
    private val _isNetworkAvailable = MutableStateFlow(false)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable
    
    private var networkObservingJob: Job? = null
    private var lastNetworkState: Boolean? = null
    
    /**
     * Start observing network connectivity changes
     */
    fun startObservingNetwork() {
        // Cancel any existing job
        networkObservingJob?.cancel()
        
        // Start a new observation
        networkObservingJob = NetworkUtil.observeNetworkConnectivity(context)
            .onEach { isAvailable ->
                _isNetworkAvailable.value = isAvailable
                
                // Show toast only when network state changes, not on initial setup
                if (lastNetworkState != null && lastNetworkState != isAvailable) {
                    val message = if (isAvailable) {
                        "Internet connection restored"
                    } else {
                        "No internet connection"
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
                
                lastNetworkState = isAvailable
            }
            .launchIn(externalScope)
    }
    
    /**
     * Stop observing network connectivity changes
     */
    fun stopObservingNetwork() {
        networkObservingJob?.cancel()
        networkObservingJob = null
    }
}