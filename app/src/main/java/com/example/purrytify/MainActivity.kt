package com.example.purrytify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.purrytify.ui.components.BottomNavigation
import com.example.purrytify.ui.components.MiniPlayerBar
import com.example.purrytify.ui.components.NetworkConnectivityDialog
import com.example.purrytify.ui.navigation.NavGraph
import com.example.purrytify.ui.navigation.Screen
import com.example.purrytify.ui.screens.library.LibraryViewModel
import com.example.purrytify.ui.theme.PurrytifyTheme
import com.example.purrytify.util.NetworkUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var networkUtils: NetworkUtils
    
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        var isAppReady = false

        // Keep splash screen until content is ready
        splashScreen.setKeepOnScreenCondition { !isAppReady }

        super.onCreate(savedInstanceState)

        setContent {
            PurrytifyTheme {
                isAppReady = true
                PurrytifyApp(networkUtils)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurrytifyApp(networkUtils: NetworkUtils) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    val viewModel: LibraryViewModel = hiltViewModel()
    val currentPlayingSong by viewModel.currentPlayingSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progress by viewModel.progress.collectAsState()
    
    // Network connectivity state
    val isNetworkAvailable by networkUtils.isNetworkAvailable.collectAsState(initial = true)
    var showNetworkDialog by remember { mutableStateOf(false) }
    
    // If we need to show the bottom nav
    val showBottomNav = shouldShowBottomNav(currentRoute)
    
    // If on the player screen
    val isPlayerScreen = currentRoute?.startsWith(Screen.Player.route) == true
    
    // Show mini player only if a song is playing and we're not on the full player screen
    val showMiniPlayer = currentPlayingSong != null && !isPlayerScreen
    
    // Monitor network state changes
    LaunchedEffect(isNetworkAvailable) {
        if (!isNetworkAvailable) {
            showNetworkDialog = true
        } else {
            showNetworkDialog = false
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    bottom = when {
                        showBottomNav && showMiniPlayer -> 136.dp // Both nav and mini player
                        showBottomNav -> 76.dp // Only nav
                        showMiniPlayer -> 60.dp // Only mini player
                        else -> 0.dp // Neither
                    }
                )
        ) {
            NavGraph(
                navController = navController,
                modifier = Modifier.fillMaxSize(),
                viewModel = viewModel,
                isNetworkAvailable = isNetworkAvailable
            )
        }

        // Mini player - above content but below nav bar
        if (showMiniPlayer) {
            Box(
                modifier = Modifier
                    .align(if (showBottomNav) Alignment.BottomCenter else Alignment.BottomCenter)
                    .fillMaxWidth()
                    .zIndex(1f)
                    .padding(bottom = if (showBottomNav) 76.dp else 0.dp)
            ) {
                currentPlayingSong?.let { song ->
                    MiniPlayerBar(
                        song = song,
                        isPlaying = isPlaying,
                        progress = progress,
                        onPlayPauseClick = { viewModel.togglePlayPause() },
                        onLikeClick = { viewModel.toggleLikeSong(song) },
                        onBarClick = {
                            navController.navigate("${Screen.Player.route}/${song.id}")
                        }
                    )
                }
            }
        }

        // Bottom navigation - with highest zIndex so it appears above everything
        if (showBottomNav) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .zIndex(2f)
            ) {
                BottomNavigation(navController = navController)
            }
        }
        
        // Network connectivity dialog
        NetworkConnectivityDialog(
            isVisible = showNetworkDialog,
            onDismiss = { showNetworkDialog = false }
        )
    }
}

private fun shouldShowBottomNav(currentRoute: String?): Boolean {
    return currentRoute in listOf("home", "library", "profile")
}