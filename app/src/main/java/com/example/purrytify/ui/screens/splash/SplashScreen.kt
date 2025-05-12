package com.example.purrytify.ui.screens.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.purrytify.R
import com.example.purrytify.ui.theme.PurrytifyBlack

/**
 * Splash screen with animation and auth status check
 */
@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    // Animation state
    var startAnimation by remember { mutableStateOf(false) }
    val alphaAnimation = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "Alpha Animation"
    )
    
    // Navigation state
    val navigationState by viewModel.navigationState.collectAsState()
    
    // Start animation when the composable is first created
    LaunchedEffect(key1 = true) {
        startAnimation = true
    }
    
    // Navigation effect
    LaunchedEffect(key1 = navigationState) {
        when (navigationState) {
            SplashNavigationState.NavigateToLogin -> onNavigateToLogin()
            SplashNavigationState.NavigateToHome -> onNavigateToHome()
            else -> {} // Still loading
        }
    }
    
    // Splash screen content
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PurrytifyBlack),
        contentAlignment = Alignment.Center
    ) {
        // Logo with fade-in animation
        Image(
            painter = painterResource(id = R.drawable.logo_3), // Make sure to add this image
            contentDescription = "Purrytify Logo",
            modifier = Modifier
                .size(150.dp)
                .alpha(alphaAnimation.value)
        )
        
        // Loading indicator at the bottom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 50.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(30.dp)
                    .alpha(alphaAnimation.value),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp
            )
        }
    }
}