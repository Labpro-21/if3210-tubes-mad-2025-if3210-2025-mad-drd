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
import com.example.purrytify.ui.screens.scanner.QRScannerScreen
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
                },
                onNavigateToQRScanner = {
                    navController.navigate(Routes.QR_SCANNER)
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
                onNavigateToTimeListened = { year, month ->
                    navController.navigate(Routes.createTimeListenedRoute(year, month))
                },
                onNavigateToTopArtists = { year, month ->
                    navController.navigate(Routes.createTopArtistsRoute(year, month))
                },
                onNavigateToTopSongs = { year, month ->
                    navController.navigate(Routes.createTopSongsRoute(year, month))
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
        
        // QR Scanner Screen
        composable(Routes.QR_SCANNER) {
            QRScannerScreen(
                onBackPressed = { navController.popBackStack() },
                onNavigateToPlayer = { songId ->
                    navController.navigate(Routes.PLAYER.replace("{songId}", songId)) {
                        // Pop the scanner screen from the back stack
                        popUpTo(Routes.QR_SCANNER) { inclusive = true }
                    }
                }
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
        
        // Analytics Screens with year/month parameters
        composable(
            route = Routes.TIME_LISTENED,
            arguments = listOf(
                navArgument("year") { type = NavType.IntType },
                navArgument("month") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val year = backStackEntry.arguments?.getInt("year") ?: 2025
            val month = backStackEntry.arguments?.getInt("month") ?: 1
            
            com.example.purrytify.ui.screens.analytics.TimeListenedScreen(
                year = year,
                month = month,
                onBackPressed = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Routes.TOP_ARTISTS,
            arguments = listOf(
                navArgument("year") { type = NavType.IntType },
                navArgument("month") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val year = backStackEntry.arguments?.getInt("year") ?: 2025
            val month = backStackEntry.arguments?.getInt("month") ?: 1
            
            com.example.purrytify.ui.screens.analytics.TopArtistsScreen(
                year = year,
                month = month,
                onBackPressed = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Routes.TOP_SONGS,
            arguments = listOf(
                navArgument("year") { type = NavType.IntType },
                navArgument("month") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val year = backStackEntry.arguments?.getInt("year") ?: 2025
            val month = backStackEntry.arguments?.getInt("month") ?: 1
            
            com.example.purrytify.ui.screens.analytics.TopSongsScreen(
                year = year,
                month = month,
                onBackPressed = { navController.popBackStack() }
            )
        }
    }
}