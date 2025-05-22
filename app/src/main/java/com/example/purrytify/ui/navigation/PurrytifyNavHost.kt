package com.example.purrytify.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.purrytify.ui.screens.auth.LoginScreen
import com.example.purrytify.ui.screens.home.HomeScreen
import com.example.purrytify.ui.screens.library.LibraryScreen
import com.example.purrytify.ui.screens.player.PlayerScreen
import com.example.purrytify.ui.screens.profile.ProfileScreen
import com.example.purrytify.ui.screens.topsongs.TopSongsScreen
import com.example.purrytify.ui.screens.playlist.DailyPlaylistScreen

@Composable
fun PurrytifyNavHost(
    navController: NavHostController,
    isNetworkAvailable: Boolean,
    modifier: Modifier = Modifier,
    startDestination: String
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
                    navController.navigate(Routes.PLAYER.replace("{songId}", songId))
                },
                onNavigateToTopSongs = { type ->
                    when (type) {
                        "global" -> navController.navigate(Routes.TOP_SONGS_GLOBAL)
                        "country" -> navController.navigate(Routes.TOP_SONGS_COUNTRY)
                        "daily" -> navController.navigate(Routes.DAILY_PLAYLIST)
                    }
                }
            )
        }
        
        // Library Screen
        composable(Routes.LIBRARY) {
            LibraryScreen(
                onNavigateToPlayer = { songId ->
                    navController.navigate(Routes.PLAYER.replace("{songId}", songId))
                }
            )
        }
        
        // Profile Screen
        composable(Routes.PROFILE) {
            ProfileScreen(
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
                isNetworkAvailable = isNetworkAvailable
            )
        }
        
        // Player Screen
        composable(
            route = Routes.PLAYER,
            arguments = listOf(navArgument("songId") { type = NavType.StringType })
        ) { backStackEntry ->
            val songId = backStackEntry.arguments?.getString("songId") ?: ""
            PlayerScreen(
                songId = songId, 
                onBackPressed = { navController.popBackStack() }
            )
        }
        
        // Top Songs Global Screen
        composable(Routes.TOP_SONGS_GLOBAL) {
            TopSongsScreen(
                isGlobal = true,
                onBack = { navController.popBackStack() }
            )
        }
        
        // Top Songs Country Screen
        composable(Routes.TOP_SONGS_COUNTRY) {
            TopSongsScreen(
                isGlobal = false,
                onBack = { navController.popBackStack() }
            )
        }
        
        // Daily Playlist Screen
        composable(Routes.DAILY_PLAYLIST) {
            DailyPlaylistScreen(
                onBack = { navController.popBackStack() },
                onNavigateToPlayer = { songId ->
                    navController.navigate(Routes.PLAYER.replace("{songId}", songId))
                }
            )
        }
    }
}