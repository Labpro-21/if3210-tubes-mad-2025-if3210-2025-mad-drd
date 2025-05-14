package com.example.purrytify.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.purrytify.ui.components.PurrytifyBottomNavigation

/**
 * Main app scaffold that handles bottom navigation and hosts the NavHost
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
    
    // Determine if we should show the bottom navigation
    val shouldShowBottomNav = remember(currentRoute) {
        when (currentRoute) {
            Routes.LOGIN, Routes.PLAYER -> false
            else -> true
        }
    }
    
    Scaffold(
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