package com.example.purrytify.ui.model

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Data class for navigation items
 */
data class NavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)