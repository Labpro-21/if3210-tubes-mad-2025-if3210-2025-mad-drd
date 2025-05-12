package com.example.purrytify

import android.app.Application
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.util.Log
import com.example.purrytify.broadcast.NetworkReceiver
import com.example.purrytify.service.TokenService
import com.example.purrytify.util.NetworkManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

@HiltAndroidApp
class PurrytifyApplication : Application() {
    
    @Inject
    lateinit var tokenService: TokenService
    
    @Inject
    lateinit var networkManager: NetworkManager
    
    @Inject
    lateinit var externalScope: CoroutineScope
    
    private val networkReceiver = NetworkReceiver()
    
    override fun onCreate() {
        super.onCreate()
        
        // Start token check service
        tokenService.startTokenCheck()
        
        // Start network monitoring
        networkManager.startObservingNetwork()
        
        // Register network broadcast receiver
        val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(networkReceiver, intentFilter)
        
        Log.d("PurrytifyApp", "Purrytify application started")
    }
    
    override fun onTerminate() {
        super.onTerminate()
        
        // Unregister network broadcast receiver
        unregisterReceiver(networkReceiver)
        
        // Stop network monitoring
        networkManager.stopObservingNetwork()
        
        // Stop token check service
        tokenService.stopTokenCheck()
    }
}