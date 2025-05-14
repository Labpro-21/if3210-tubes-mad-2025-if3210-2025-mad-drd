package com.example.purrytify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.example.purrytify.ui.navigation.PurrytifyAppScaffold
import com.example.purrytify.ui.theme.PurrytifyBlack
import com.example.purrytify.ui.theme.PurrytifyTheme
import com.example.purrytify.util.NetworkManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var networkManager: NetworkManager

    private val viewModel: MainViewModel by viewModels()

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
                            isNetworkAvailable = isNetworkAvailable
                        )
                    }
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        // Refresh network state when activity resumes
        networkManager.refreshNetworkState()
    }
}