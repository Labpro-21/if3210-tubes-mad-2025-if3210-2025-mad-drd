package com.example.purrytify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.purrytify.ui.components.BottomNavigation
import com.example.purrytify.ui.navigation.NavGraph
import com.example.purrytify.ui.theme.PurrytifyTheme
import dagger.hilt.android.AndroidEntryPoint
import android.os.Handler
import android.os.Looper

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Keep splash screen shown for 2 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            splashScreen.setKeepOnScreenCondition { false }
        }, 2000)

        setContent {
            PurrytifyTheme {
                PurrytifyApp()
            }
        }
    }
}
@Composable
fun PurrytifyApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Main content area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (shouldShowBottomNav(currentRoute)) 80.dp else 0.dp)
        ) {
            NavGraph(
                navController = navController,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Bottom navigation
        if (shouldShowBottomNav(currentRoute)) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                BottomNavigation(navController = navController)
            }
        }

        // if (isSongPlaying) {
        //    MiniPlayer(modifier = Modifier.align(Alignment.BottomCenter))
        // }
    }
}

// Helper function to determine if bottom nav should be shown
private fun shouldShowBottomNav(currentRoute: String?): Boolean {
    return currentRoute in listOf("home", "library", "profile")
}