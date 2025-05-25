package com.example.purrytify.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
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
    private val _isNetworkAvailable = MutableStateFlow(true) // Default to true to prevent initial false negatives
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable

    private var networkObservingJob: Job? = null
    private var lastNetworkState: Boolean? = null

    // Create exception handler to prevent application crashes
    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        Log.e("NetworkManager", "Error in network observation: ${exception.message}", exception)
    }

    // Main thread handler for showing toasts
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        // Initialize network observation
        startObservingNetwork()
    }
    
    /**
     * Start observing network connectivity changes
     */
    fun startObservingNetwork() {
        // Cancel any existing job
        networkObservingJob?.cancel()

        // Start a new observation with error handling
        try {
            // First check the current network state
            val currentState = NetworkUtil.isNetworkAvailable(context)
            _isNetworkAvailable.value = currentState
            lastNetworkState = currentState
            
            // Then start observing for changes
            networkObservingJob = NetworkUtil.observeNetworkConnectivity(context)
                .onEach { isAvailable ->
                    _isNetworkAvailable.value = isAvailable

                    // Show toast only when network state changes, not on initial setup
                    if (lastNetworkState != null && lastNetworkState != isAvailable) {
                        showNetworkStatusToast(isAvailable)
                    }

                    lastNetworkState = isAvailable
                }
                .catch { exception ->
                    Log.e("NetworkManager", "Error in network flow: ${exception.message}", exception)
                    // If we get an error, attempt to recover by checking the network directly
                    _isNetworkAvailable.value = NetworkUtil.isNetworkAvailable(context)
                }
                .launchIn(externalScope)
        } catch (e: Exception) {
            Log.e("NetworkManager", "Failed to start network observation: ${e.message}", e)
            // If we fail to start observation, assume network is available to prevent false negatives
            _isNetworkAvailable.value = true
        }
    }

    /**
     * Force a refresh of the network state
     */
    fun refreshNetworkState() {
        try {
            val isAvailable = NetworkUtil.isNetworkAvailable(context)
            _isNetworkAvailable.value = isAvailable
            Log.d("NetworkManager", "Network state refreshed: $isAvailable")
        } catch (e: Exception) {
            Log.e("NetworkManager", "Error refreshing network state: ${e.message}", e)
        }
    }

    /**
     * Show network status toast on the main thread using Handler
     */
    private fun showNetworkStatusToast(isAvailable: Boolean) {
        // Use main handler to ensure we're on the UI thread
        mainHandler.post {
            try {
                val message = if (isAvailable) {
                    "Internet connection restored"
                } else {
                    "No internet connection"
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("NetworkManager", "Failed to show network toast: ${e.message}", e)
            }
        }
    }

    /**
     * Stop observing network connectivity changes
     */
    fun stopObservingNetwork() {
        networkObservingJob?.cancel()
        networkObservingJob = null
    }
}