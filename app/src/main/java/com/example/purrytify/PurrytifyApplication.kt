package com.example.purrytify

import android.app.Activity
import android.app.Application
import android.content.ComponentCallbacks2
import android.os.Bundle
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.example.purrytify.domain.player.PlayerBridge
import com.example.purrytify.service.MusicServiceConnection
import com.example.purrytify.service.TokenService
import com.example.purrytify.util.NetworkManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class PurrytifyApplication : Application(), Configuration.Provider, Application.ActivityLifecycleCallbacks {

    @Inject
    lateinit var tokenService: TokenService

    @Inject
    lateinit var networkManager: NetworkManager
    
    @Inject
    lateinit var musicServiceConnection: MusicServiceConnection

    @Inject
    lateinit var playerBridge: PlayerBridge

    @Inject
    lateinit var externalScope: CoroutineScope

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    // Track active activities
    private var activityCount = 0
    private var isAppInForeground = false

    // We need to avoid accessing workerFactory before it's initialized
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Register activity lifecycle callbacks to track app state
        registerActivityLifecycleCallbacks(this)

        // Ensure WorkManager is initialized after Hilt injections
        // This delays the creation of WorkManager until after workerFactory is initialized
        WorkManager.initialize(this, workManagerConfiguration)

        // Start network monitoring using NetworkCallback API
        networkManager.startObservingNetwork()

        // Start token check service - now WorkManager is properly initialized
        tokenService.startTokenCheck()
        
        // Connect to music service
        musicServiceConnection.connect()

        Log.d("PurrytifyApp", "Purrytify application started")
    }

    override fun onTerminate() {
        super.onTerminate()

        // Unregister activity lifecycle callbacks
        unregisterActivityLifecycleCallbacks(this)

        // Stop network monitoring
        networkManager.stopObservingNetwork()

        // Stop token check service
        tokenService.stopTokenCheck()
        
        // Disconnect from music service
        musicServiceConnection.disconnect()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        
        Log.d("PurrytifyApp", "onTrimMemory called with level: $level, isAppInForeground: $isAppInForeground")
        
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                // App is no longer visible to the user
                Log.d("PurrytifyApp", "App UI is hidden")
                handleAppMovedToBackground()
            }
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                // System is running very low on memory and the process is being killed
                Log.d("PurrytifyApp", "App is being killed due to memory pressure")
                stopMusicAndCleanup()
            }
            ComponentCallbacks2.TRIM_MEMORY_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                // System is running low on memory, app might be killed soon
                if (!isAppInForeground) {
                    Log.d("PurrytifyApp", "App in background and system low on memory, stopping music")
                    stopMusicAndCleanup()
                }
            }
        }
    }

    private fun handleAppMovedToBackground() {
        // Give some time for user to return to app (e.g., switching between apps)
        // If user doesn't return within a reasonable time and system needs memory, stop music
        externalScope.launch {
            kotlinx.coroutines.delay(5000) // Wait 5 seconds
            if (!isAppInForeground) {
                Log.d("PurrytifyApp", "App stayed in background for 5 seconds, preparing to stop music on memory pressure")
                // Music will be stopped if onTrimMemory is called with higher levels
            }
        }
    }

    private fun stopMusicAndCleanup() {
        Log.d("PurrytifyApp", "Stopping music and cleaning up due to app closure")
        
        try {
            // Stop music playback
            playerBridge.stopForAppClosure()
            
            // Stop and disconnect from music service
            musicServiceConnection.stopAndDisconnect()
            
            Log.d("PurrytifyApp", "Music stopped and resources cleaned up")
        } catch (e: Exception) {
            Log.e("PurrytifyApp", "Error stopping music and cleaning up: ${e.message}", e)
        }
    }

    // Activity Lifecycle Callbacks

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        Log.d("PurrytifyApp", "Activity created: ${activity.javaClass.simpleName}")
    }

    override fun onActivityStarted(activity: Activity) {
        activityCount++
        isAppInForeground = true
        Log.d("PurrytifyApp", "Activity started: ${activity.javaClass.simpleName}, active count: $activityCount")
    }

    override fun onActivityResumed(activity: Activity) {
        Log.d("PurrytifyApp", "Activity resumed: ${activity.javaClass.simpleName}")
    }

    override fun onActivityPaused(activity: Activity) {
        Log.d("PurrytifyApp", "Activity paused: ${activity.javaClass.simpleName}")
    }

    override fun onActivityStopped(activity: Activity) {
        activityCount--
        Log.d("PurrytifyApp", "Activity stopped: ${activity.javaClass.simpleName}, active count: $activityCount")
        
        if (activityCount <= 0) {
            isAppInForeground = false
            Log.d("PurrytifyApp", "All activities stopped, app moved to background")
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        Log.d("PurrytifyApp", "Activity save instance state: ${activity.javaClass.simpleName}")
    }

    override fun onActivityDestroyed(activity: Activity) {
        Log.d("PurrytifyApp", "Activity destroyed: ${activity.javaClass.simpleName}")
    }
}