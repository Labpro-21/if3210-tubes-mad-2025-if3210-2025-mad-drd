package com.example.purrytify.broadcast

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.purrytify.util.NetworkManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * BroadcastReceiver to handle network connectivity changes
 */
@AndroidEntryPoint
class NetworkReceiver : HiltBroadcastReceiver() {
    
    @Inject
    lateinit var networkManager: NetworkManager
    
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        networkManager.startObservingNetwork()
    }
}