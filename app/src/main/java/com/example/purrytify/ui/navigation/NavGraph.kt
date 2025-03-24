package com.example.purrytify.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.purrytify.ui.screens.auth.LoginScreen
import com.example.purrytify.ui.screens.home.HomeScreen
import com.example.purrytify.ui.screens.library.LibraryScreen
import com.example.purrytify.ui.screens.library.LibraryViewModel
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
    startDestination: String = Screen.Login.route,
    viewModel: LibraryViewModel? = null,
    isNetworkAvailable: Boolean = true
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Uses internet
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                isNetworkAvailable = isNetworkAvailable
            )
        }
        
        // Doesn't use internet (local db)
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToPlayer = { songId ->
                    navController.navigate("${Screen.Player.route}/$songId")
                },
            )
        }
        
        // Doesn't use internet (local db)
        composable(Screen.Library.route) {
            if (viewModel != null) {
                LibraryScreen(
                    onNavigateToPlayer = { songId ->
                        navController.navigate("${Screen.Player.route}/$songId")
                    },
                    viewModel = viewModel,
                )
            } else {
                LibraryScreen(
                    onNavigateToPlayer = { songId ->
                        navController.navigate("${Screen.Player.route}/$songId")
                    },
                )
            }
        }
        
        // Uses internet
        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                isNetworkAvailable = isNetworkAvailable
            )
        }
        
        // Doesn't use internet (local db)
        composable("${Screen.Player.route}/{songId}") { backStackEntry ->
            val songId = backStackEntry.arguments?.getString("songId")
            if (viewModel != null) {
                PlayerScreen(
                    songId = songId,
                    onNavigateBack = {
                        navController.navigateUp()
                    },
                    viewModel = viewModel,
                )
            } else {
                PlayerScreen(
                    songId = songId,
                    onNavigateBack = {
                        navController.navigateUp()
                    },
                )
            }
        }
    }
}