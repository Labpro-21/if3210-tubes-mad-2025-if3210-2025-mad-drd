package com.example.purrytify.ui.navigation

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

/**
 * Main app scaffold that handles navigation and hosts the NavHost with responsive design
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurrytifyAppScaffold(
    navController: NavHostController,
    startDestination: String,
    isNetworkAvailable: Boolean
) {
    // Get current route to determine if bottom nav should be shown
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    
    // Get device orientation
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    // Determine if we should show the navigation
    val shouldShowNav = remember(currentRoute) {
        when (currentRoute) {
            Routes.LOGIN, Routes.PLAYER -> false
            else -> true
        }
    }
    
    if (isLandscape && shouldShowNav) {
        // Landscape with side navigation
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // Left side navigation
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .fillMaxHeight()
            ) {
                PurrytifySideNavigation(
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
            
            // Content area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                PurrytifyNavHost(
                    navController = navController,
                    isNetworkAvailable = isNetworkAvailable,
                    startDestination = startDestination
                )
            }
        }
    } else {
        // Portrait with bottom navigation or no navigation
        Scaffold(
            bottomBar = {
                if (shouldShowNav) {
                    PurrytifyBottomNavigation(
                        currentRoute = currentRoute ?: "",
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