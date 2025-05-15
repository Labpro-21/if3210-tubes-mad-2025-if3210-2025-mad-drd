package com.example.purrytify.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.purrytify.R
import com.example.purrytify.ui.components.LoadingView
import com.example.purrytify.ui.components.NewSongItem
import com.example.purrytify.ui.components.SongListItem
import com.example.purrytify.ui.theme.*
import com.example.purrytify.util.CountryUtils

/**
 * Home screen with Charts, New Songs, and Recently Played sections
 */
@Composable
fun HomeScreen(
    onNavigateToPlayer: (String) -> Unit,
    onNavigateToTopSongs: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    // Collect state from view model
    val isLoading by viewModel.loadingState.collectAsState()
    val newSongs by viewModel.newSongs.collectAsState()
    val recentlyPlayedSongs by viewModel.recentlyPlayedSongs.collectAsState()
    val currentPlayingSong by viewModel.currentPlayingSong.collectAsState()
    val userCountry by viewModel.userCountry.collectAsState()
    val isCountrySongsAvailable by viewModel.isCountrySongsAvailable.collectAsState()
    
    // Get country name for display
    val countryName = CountryUtils.getCountryNameFromCode(userCountry) ?: userCountry
    
    // Reset home when recomposing
    LaunchedEffect(Unit) {
        viewModel.refreshSongs()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PurrytifyBlack)
    ) {
        if (isLoading) {
            LoadingView()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Charts Section
                    Text(
                        text = "Charts",
                        style = Typography.titleLarge,
                        color = PurrytifyWhite,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Charts cards row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Global Top 50 Card
                        item {
                            ChartCard(
                                title = "Top 50",
                                subtitle = "GLOBAL",
                                backgroundResId = R.drawable.bg_top_global,
                                onClick = { onNavigateToTopSongs("global") }
                            )
                        }
                        
                        // Country Top 10 Card (if available)
                        item {
                            ChartCard(
                                title = "Top 10",
                                subtitle = countryName.uppercase(),
                                backgroundResId = R.drawable.bg_top_local,
                                isAvailable = isCountrySongsAvailable,
                                onClick = { onNavigateToTopSongs("country") }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // New Songs Section
                    Text(
                        text = "New songs",
                        style = Typography.titleLarge,
                        color = PurrytifyWhite,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // New songs horizontal list
                item {
                    if (newSongs.isEmpty()) {
                        Text(
                            text = "No songs uploaded yet",
                            style = Typography.bodyLarge,
                            color = PurrytifyLightGray
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(newSongs) { song ->
                                NewSongItem(
                                    song = song,
                                    isPlaying = currentPlayingSong?.id == song.id,
                                    onClick = { 
                                        viewModel.playSong(song)
                                        onNavigateToPlayer(song.id)
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Recently Played Section
                    Text(
                        text = "Recently played",
                        style = Typography.titleLarge,
                        color = PurrytifyWhite,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Recently played songs vertical list
                if (recentlyPlayedSongs.isEmpty()) {
                    item {
                        Text(
                            text = "No recently played songs",
                            style = Typography.bodyLarge,
                            color = PurrytifyLightGray,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                } else {
                    items(recentlyPlayedSongs) { song ->
                        SongListItem(
                            song = song,
                            isPlaying = currentPlayingSong?.id == song.id,
                            onClick = { 
                                viewModel.playSong(song)
                                onNavigateToPlayer(song.id)
                            }
                        )
                    }
                }
                
                // Bottom spacer for mini player
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

/**
 * Chart card component used in the Charts section
 */
@Composable
fun ChartCard(
    title: String,
    subtitle: String,
    backgroundResId: Int,
    isAvailable: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(140.dp) // Square size
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = isAvailable) { onClick() }
    ) {
        // Background image
        Image(
            painter = painterResource(id = backgroundResId),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
        )
        
        // "Not Available" overlay if country songs aren't available
        if (!isAvailable) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Not Available",
                    style = Typography.bodyMedium,
                    color = PurrytifyWhite,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        // Text content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = title,
                style = Typography.titleMedium,
                color = PurrytifyWhite,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = subtitle,
                style = Typography.bodySmall,
                color = PurrytifyLightGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}