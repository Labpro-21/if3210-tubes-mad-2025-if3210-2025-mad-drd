package com.example.purrytify.ui.navigation

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.purrytify.ui.components.MiniPlayer
import com.example.purrytify.ui.screens.player.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurrytifyAppScaffold(
    navController: NavHostController,
    startDestination: String,
    isNetworkAvailable: Boolean,
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    val shouldShowNav = remember(currentRoute) {
        when (currentRoute) {
            Routes.LOGIN, Routes.PLAYER -> false
            else -> true
        }
    }
    
    // Determine if we should show the mini player
    val shouldShowMiniPlayer = remember(currentRoute) {
        when (currentRoute) {
            Routes.LOGIN -> false
            Routes.PLAYER -> false
            else -> true
        }
    }
    
    // Check if there's currently playing music
    val currentItem by playerViewModel.currentItem.collectAsState()
    val showMiniPlayer = shouldShowMiniPlayer && currentItem != null
    
    if (isLandscape && shouldShowNav) {
        // Landscape with side navigation
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // Left side navigation
            Box(
                modifier = Modifier
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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Main content
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    PurrytifyNavHost(
                        navController = navController,
                        isNetworkAvailable = isNetworkAvailable,
                        startDestination = startDestination
                    )
                }
                
                // Mini player at bottom
                if (showMiniPlayer) {
                    MiniPlayer(
                        onClick = {
                            currentItem?.let { item ->
                                val navigationId = playerViewModel.getNavigationId(item)
                                navController.navigate(Routes.PLAYER.replace("{songId}", navigationId))
                            }
                        },
                        viewModel = playerViewModel
                    )
                }
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Main content
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    PurrytifyNavHost(
                        navController = navController,
                        isNetworkAvailable = isNetworkAvailable,
                        startDestination = startDestination
                    )
                }
                
                // Mini player at bottom (above bottom nav)
                if (showMiniPlayer) {
                    MiniPlayer(
                        onClick = {
                            currentItem?.let { item ->
                                val navigationId = when (item) {
                                    is com.example.purrytify.domain.model.PlaylistItem.LocalSong -> item.id
                                    is com.example.purrytify.domain.model.PlaylistItem.OnlineSong -> item.originalId
                                }
                                navController.navigate(Routes.PLAYER.replace("{songId}", navigationId))
                            }
                        },
                        viewModel = playerViewModel
                    )
                }
            }
        }
    }
}