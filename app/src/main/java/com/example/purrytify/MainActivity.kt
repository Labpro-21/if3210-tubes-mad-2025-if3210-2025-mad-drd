package com.example.purrytify

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.purrytify.data.repository.LogoutReason
import com.example.purrytify.data.repository.NavigationEvent
import com.example.purrytify.data.repository.NavigationEventRepository
import com.example.purrytify.service.MusicServiceConnection
import com.example.purrytify.ui.navigation.PurrytifyAppScaffold
import com.example.purrytify.ui.navigation.Routes
import com.example.purrytify.ui.screens.player.PlayerViewModel
import com.example.purrytify.ui.theme.PurrytifyBlack
import com.example.purrytify.ui.theme.PurrytifyTheme
import com.example.purrytify.util.DeepLinkHandler
import com.example.purrytify.util.NetworkManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var networkManager: NetworkManager
    
    @Inject
    lateinit var musicServiceConnection: MusicServiceConnection
    
    @Inject
    lateinit var deepLinkHandler: DeepLinkHandler
    
    @Inject
    lateinit var navigationEventRepository: NavigationEventRepository

    private val viewModel: MainViewModel by viewModels()
    private val playerViewModel: PlayerViewModel by viewModels()

    // Permission request launcher for notifications
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Notification permission granted
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Keep splash screen until we have determined the start destination
        val splashScreen = installSplashScreen()
        
        // This condition ensures splash screen is shown until we have both:
        // 1. Completed loading
        // 2. Determined the start destination
        splashScreen.setKeepOnScreenCondition {
            viewModel.isLoading.value || viewModel.startDestination.value == null
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Observe navigation events for logout handling
        observeNavigationEvents()

        setContent {
            PurrytifyTheme {
                // Observe network availability
                val isNetworkAvailable by networkManager.isNetworkAvailable.collectAsState()
                val startDestination by viewModel.startDestination.collectAsState()
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = PurrytifyBlack
                ) {
                    // Only show the app scaffold once we have a start destination
                    startDestination?.let { destination ->
                        val navController = rememberNavController()
                        
                        PurrytifyAppScaffold(
                            navController = navController,
                            startDestination = destination,
                            isNetworkAvailable = isNetworkAvailable,
                            playerViewModel = playerViewModel
                        )
                    }
                }
            }
        }
        
        // Handle deep link from intent after UI is set up
        handleDeepLinkFromIntent(intent)
    }
    
    /**
     * Observe navigation events to handle logout and other navigation requirements
     */
    private fun observeNavigationEvents() {
        lifecycleScope.launch {
            navigationEventRepository.navigationEvents.collect { event ->
                when (event) {
                    is NavigationEvent.Logout -> {
                        handleLogoutEvent(event.reason)
                    }
                    is NavigationEvent.NavigateToRoute -> {
                        // Handle other navigation events if needed in the future
                        android.util.Log.d("MainActivity", "Navigation event to route: ${event.route}")
                    }
                }
            }
        }
    }
    
    /**
     * Handle logout event by showing appropriate message and restarting the activity
     * to reset the navigation state and go to login screen
     */
    private fun handleLogoutEvent(reason: LogoutReason) {
        android.util.Log.d("MainActivity", "Handling logout event with reason: $reason")
        
        // Show appropriate message to user based on logout reason
        val message = when (reason) {
            LogoutReason.TOKEN_REFRESH_FAILED -> "Session expired. Please log in again."
            LogoutReason.TOKEN_EXPIRED -> "Your session has expired. Please log in again."
            LogoutReason.INVALID_TOKEN -> "Authentication error. Please log in again."
            LogoutReason.NETWORK_ERROR -> "Network error. Please check your connection and log in again."
            LogoutReason.USER_INITIATED -> "Logged out successfully."
        }
        
        // Show toast message
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        
        // Stop any ongoing music playback
        playerViewModel.stopPlayback()
        
        // Restart activity to reset navigation state and trigger auth check
        // This will cause MainViewModel to re-check auth status and navigate to login
        lifecycleScope.launch {
            // Small delay to ensure the toast is shown
            kotlinx.coroutines.delay(500)
            
            // Restart the activity to reset navigation state
            val intent = Intent(this@MainActivity, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLinkFromIntent(intent)
    }
    
    /**
     * Handle deep link from intent
     */
    private fun handleDeepLinkFromIntent(intent: Intent?) {
        intent?.let { 
            if (deepLinkHandler.hasDeepLink(it)) {
                // Wait for the app to be fully initialized before handling deep link
                // This prevents issues with handling deep links before the nav controller is ready
                post {
                    handleDeepLink(it)
                }
            }
        }
    }
    
    /**
     * Handle deep link navigation
     */
    private fun handleDeepLink(intent: Intent) {
        deepLinkHandler.handleDeepLink(
            intent = intent,
            onNavigateToPlayer = { songId ->
                // Navigation to player will be handled by the deep link handler
                // The player screen will be opened automatically when a song starts playing
            },
            onShowError = { errorMessage ->
                showError(errorMessage)
            }
        )
    }
    
    /**
     * Show error message to user
     */
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    /**
     * Post a runnable to be executed after a short delay
     * This ensures the UI is fully initialized before handling deep links
     */
    private fun post(runnable: () -> Unit) {
        window.decorView.post {
            runnable()
        }
    }
    
    override fun onStart() {
        super.onStart()
        // Connect to music service
        musicServiceConnection.connect()
    }
    
    override fun onResume() {
        super.onResume()
        
        // Refresh network state when activity resumes
        networkManager.refreshNetworkState()
    }
    
    override fun onStop() {
        super.onStop()
        // Don't disconnect from service to allow background playback
        // musicServiceConnection.disconnect()
    }
}