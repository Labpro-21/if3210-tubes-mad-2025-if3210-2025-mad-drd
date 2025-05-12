package com.example.purrytify.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.purrytify.ui.screens.auth.LoginScreen
import com.example.purrytify.ui.screens.home.HomeScreen

@Composable
fun PurrytifyNavHost(
    navController: NavHostController,
    isNetworkAvailable: Boolean,
    modifier: Modifier = Modifier,
    startDestination: String = Routes.HOME
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Login Screen
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                isNetworkAvailable = isNetworkAvailable
            )
        }
        
        // Home Screen
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToPlayer = { songId ->
                    navController.navigate("${Routes.PLAYER}/$songId")
                }
            )
        }
        
        // Library Screen placeholder
        composable(Routes.LIBRARY) {
            // Implement when we get to the Library feature
        }
        
        // Profile Screen placeholder
        composable(Routes.PROFILE) {
            // Implement when we get to the Profile feature
        }
        
        // Add more routes for other screens as they're implemented
    }
}