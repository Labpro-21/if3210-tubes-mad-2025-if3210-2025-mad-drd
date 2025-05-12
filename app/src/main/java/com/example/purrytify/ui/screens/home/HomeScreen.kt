package com.example.purrytify.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.purrytify.ui.theme.PurrytifyBlack
import com.example.purrytify.ui.theme.PurrytifyWhite

/**
 * Home screen displaying recently played songs and new songs
 * This is a placeholder until we implement the full home screen
 */
@Composable
fun HomeScreen(
    onNavigateToPlayer: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PurrytifyBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Welcome to Purrytify",
                style = MaterialTheme.typography.headlineMedium,
                color = PurrytifyWhite,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Home screen coming soon!",
                style = MaterialTheme.typography.bodyLarge,
                color = PurrytifyWhite.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}