package com.example.purrytify.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.purrytify.ui.model.NavItem
import com.example.purrytify.ui.theme.PurrytifyGreen
import com.example.purrytify.ui.theme.PurrytifyLightGray
import com.example.purrytify.ui.theme.PurrytifyLighterBlack

@Composable
fun PurrytifyBottomNavigation(
    currentRoute: String?,
    onNavItemClick: (String) -> Unit
) {
    val navItems = listOf(
        NavItem(
            route = Routes.HOME,
            title = "Home",
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home
        ),
        NavItem(
            route = Routes.LIBRARY,
            title = "Library",
            selectedIcon = Icons.Filled.LibraryMusic,
            unselectedIcon = Icons.Outlined.LibraryMusic
        ),
        NavItem(
            route = Routes.PROFILE,
            title = "Profile",
            selectedIcon = Icons.Filled.Person,
            unselectedIcon = Icons.Outlined.Person
        )
    )
    
    NavigationBar(
        containerColor = PurrytifyLighterBlack,
        tonalElevation = 0.dp
    ) {
        navItems.forEach { item ->
            // Check if current route is home or top songs-related
            val selected = when {
                item.route == Routes.HOME && (
                    currentRoute == Routes.HOME || 
                    currentRoute == Routes.TOP_SONGS_GLOBAL || 
                    currentRoute == Routes.TOP_SONGS_COUNTRY
                ) -> true
                currentRoute == item.route -> true
                else -> false
            }
            
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.title
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                selected = selected,
                onClick = { onNavItemClick(item.route) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PurrytifyGreen,
                    selectedTextColor = PurrytifyGreen,
                    unselectedIconColor = PurrytifyLightGray,
                    unselectedTextColor = PurrytifyLightGray,
                    indicatorColor = PurrytifyLighterBlack
                )
            )
        }
    }
}