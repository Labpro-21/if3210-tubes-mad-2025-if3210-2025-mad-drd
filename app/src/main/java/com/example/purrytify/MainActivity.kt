package com.example.purrytify

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.purrytify.ui.components.PurrytifyBottomNavigation
import com.example.purrytify.ui.navigation.PurrytifyNavHost
import com.example.purrytify.ui.navigation.Routes
import com.example.purrytify.ui.theme.PurrytifyTheme
import com.example.purrytify.util.NetworkManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var networkManager: NetworkManager

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        // Keep splash screen showing until viewModel.isLoading is false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val splashScreen = installSplashScreen()
            splashScreen.setKeepOnScreenCondition { viewModel.isLoading.value }
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PurrytifyTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // Observe network availability
                val isNetworkAvailable by networkManager.isNetworkAvailable.collectAsState()

                // Observe auth state
                val isLoggedIn by viewModel.isLoggedIn.collectAsState()

                // Determine start destination based on login state
                val startDestination = if (isLoggedIn) Routes.HOME else Routes.LOGIN

                // Determine if we should show the bottom navigation
                val shouldShowBottomNav = remember(currentRoute) {
                    when (currentRoute) {
                        Routes.LOGIN, Routes.PLAYER -> false
                        else -> true
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (shouldShowBottomNav) {
                            PurrytifyBottomNavigation(
                                currentRoute = currentRoute,
                                onNavItemClick = { route ->
                                    navController.navigate(route) {
                                        popUpTo(Routes.HOME) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        PurrytifyNavHost(
                            navController = navController,
                            isNetworkAvailable = isNetworkAvailable,
                            startDestination = startDestination
                        )
                    }
                }
            }
        }
    }
}