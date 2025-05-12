package com.example.purrytify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
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
    
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            PurrytifyTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                
                // Observe network availability
                val isNetworkAvailable by networkManager.isNetworkAvailable.collectAsState()
                
                // Determine if we should show the bottom navigation
                val shouldShowBottomNav = remember(currentRoute) {
                    when (currentRoute) {
                        Routes.SPLASH, Routes.LOGIN, Routes.PLAYER -> false
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
                                        // Pop up to the start destination of the graph
                                        // to avoid building up a large stack of destinations
                                        popUpTo(Routes.HOME) {
                                            saveState = true
                                        }
                                        // Avoid multiple copies of the same destination
                                        launchSingleTop = true
                                        // Restore state when reselecting a previously selected item
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
                            isNetworkAvailable = isNetworkAvailable
                        )
                    }
                }
            }
        }
    }
}