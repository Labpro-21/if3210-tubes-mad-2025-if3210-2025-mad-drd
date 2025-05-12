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
        
        if (intent.action == ConnectivityManager.CONNECTIVITY_ACTION) {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkCapabilities = connectivityManager.activeNetwork?.let {
                connectivityManager.getNetworkCapabilities(it)
            }
            
            val isConnected = networkCapabilities?.let {
                it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                (it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                 it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                 it.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
            } ?: false
            
            // Let NetworkManager handle the state updates
            networkManager.startObservingNetwork()
        }
    }
}