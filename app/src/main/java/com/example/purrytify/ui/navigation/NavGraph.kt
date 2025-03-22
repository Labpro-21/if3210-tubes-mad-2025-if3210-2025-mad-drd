package com.example.purrytify.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.purrytify.ui.screens.auth.LoginScreen
import com.example.purrytify.ui.screens.home.HomeScreen
import com.example.purrytify.ui.screens.library.LibraryScreen
import com.example.purrytify.ui.screens.player.PlayerScreen
import com.example.purrytify.ui.screens.profile.ProfileScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object Library : Screen("library")
    object Profile : Screen("profile")
    object Player : Screen("player")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Login.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToPlayer = { songId ->
                    navController.navigate("${Screen.Player.route}/$songId")
                }
            )
        }
        
        composable(Screen.Library.route) {
            LibraryScreen(
                onNavigateToPlayer = { songId ->
                    navController.navigate("${Screen.Player.route}/$songId")
                }
            )
        }
        
        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable("${Screen.Player.route}/{songId}") { backStackEntry ->
            val songId = backStackEntry.arguments?.getString("songId")
            PlayerScreen(
                songId = songId,
                onNavigateBack = {
                    navController.navigateUp()
                }
            )
        }
    }
}