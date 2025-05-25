package com.example.purrytify.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.purrytify.ui.model.NavItem
import com.example.purrytify.ui.theme.PurrytifyGreen
import com.example.purrytify.ui.theme.PurrytifyLightGray
import com.example.purrytify.ui.theme.PurrytifyLighterBlack

/**
 * Side navigation for landscape orientation
 */
@Composable
fun PurrytifySideNavigation(
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
    
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .background(PurrytifyLighterBlack)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
    ) {
        navItems.forEach { item ->
            // Check if current route is home or top songs-related
            val selected = when {
                // If the current route is home or top songs related, highlight home
                item.route == Routes.HOME && (
                    currentRoute == Routes.HOME || 
                    currentRoute == Routes.TOP_SONGS_GLOBAL || 
                    currentRoute == Routes.TOP_SONGS_COUNTRY
                ) -> true
                // Otherwise do exact matching
                currentRoute == item.route -> true
                else -> false
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = { onNavItemClick(item.route) },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.title,
                        tint = if (selected) PurrytifyGreen else PurrytifyLightGray,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = if (selected) PurrytifyGreen else PurrytifyLightGray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}