package com.example.purrytify

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import androidx.navigation.compose.rememberNavController
import com.example.purrytify.service.MusicServiceConnection
import com.example.purrytify.ui.navigation.PurrytifyAppScaffold
import com.example.purrytify.ui.navigation.Routes
import com.example.purrytify.ui.screens.player.PlayerViewModel
import com.example.purrytify.ui.theme.PurrytifyBlack
import com.example.purrytify.ui.theme.PurrytifyTheme
import com.example.purrytify.util.NetworkManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var networkManager: NetworkManager
    
    @Inject
    lateinit var musicServiceConnection: MusicServiceConnection

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
        
        // Handle deep link if activity was started from notification
        handleIntent(intent)

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
                        
                        // Handle deep link from notification if present
                        if (intent?.extras?.getBoolean("OPEN_PLAYER", false) == true) {
                            val songId = intent.getStringExtra("SONG_ID") ?: ""
                            if (songId.isNotEmpty()) {
                                navController.navigate(Routes.PLAYER.replace("{songId}", songId))
                                // Clear the intent to prevent duplicate navigation
                                intent.removeExtra("OPEN_PLAYER")
                            }
                        }
                    }
                }
            }
        }
    }
    
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: android.content.Intent?) {
        // Process intent from notification or deep link
        if (intent?.extras?.getBoolean("OPEN_PLAYER", false) == true) {
            val songId = intent.getStringExtra("SONG_ID") ?: ""
            if (songId.isNotEmpty()) {
                // We'll navigate in setContent once navController is available
            }
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