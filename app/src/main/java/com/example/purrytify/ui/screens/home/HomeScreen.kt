package com.example.purrytify.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
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
import com.example.purrytify.domain.model.PlaylistItem
import com.example.purrytify.domain.player.PlaybackContext
import com.example.purrytify.domain.player.PlayerBridge
import com.example.purrytify.ui.components.LoadingView
import com.example.purrytify.ui.components.NewSongItem
import com.example.purrytify.ui.components.SongListItem
import com.example.purrytify.ui.theme.*
import com.example.purrytify.util.CountryUtils
import javax.inject.Inject

/**
 * Home screen with Charts, New Songs, and Recently Played sections
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToPlayer: (String) -> Unit,
    onNavigateToTopSongs: (String) -> Unit,
    onNavigateToQRScanner: () -> Unit = {},
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
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Home",
                        style = Typography.titleLarge,
                        color = PurrytifyWhite,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // QR Scanner button
                    IconButton(
                        onClick = onNavigateToQRScanner
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan QR Code",
                            tint = PurrytifyWhite,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PurrytifyBlack,
                    titleContentColor = PurrytifyWhite,
                    actionIconContentColor = PurrytifyWhite
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PurrytifyBlack)
                .padding(paddingValues)
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
                                    isGlobal = true,
                                    isAvailable = true,
                                    onClick = { onNavigateToTopSongs("global") }
                                )
                            }
                            
                            // Country Top 10 Card (if available)
                            item {
                                ChartCard(
                                    title = "Top 10",
                                    subtitle = countryName.uppercase(),
                                    isGlobal = false,
                                    isAvailable = isCountrySongsAvailable,
                                    onClick = { onNavigateToTopSongs("country") }
                                )
                            }
                            
                            // Daily Playlist Card
                            item {
                                ChartCard(
                                    title = "Daily",
                                    subtitle = "PLAYLIST",
                                    isDailyPlaylist = true,
                                    isAvailable = true,
                                    onClick = { onNavigateToTopSongs("daily") }
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
                                            viewModel.playFromNewSongs(song)
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
                                    viewModel.playFromRecentlyPlayed(song)
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
}

/**
 * Chart card component used in the Charts section
 */
@Composable
fun ChartCard(
    title: String,
    subtitle: String,
    isGlobal: Boolean = false,
    isDailyPlaylist: Boolean = false,
    isAvailable: Boolean = true,
    onClick: () -> Unit
) {
    // Define gradient colors based on card type
    val gradientColors = when {
        isGlobal -> {
            // Global top 50 gradient: #1f7973 to #071220
            listOf(
                Color(0xFF1F7973),  // Top color: #1f7973
                Color(0xFF071220)   // Bottom color: #071220
            )
        }
        isDailyPlaylist -> {
            // Daily Playlist gradient: #5E35B1 to #140144
            listOf(
                Color(0xFF5E35B1),  // Top color: purple
                Color(0xFF140144)   // Bottom color: deep purple
            )
        }
        else -> {
            // Country top 10 gradient: #d95360 to #51090f
            listOf(
                Color(0xFFD95360),  // Top color: #d95360
                Color(0xFF51090F)   // Bottom color: #51090f
            )
        }
    }
    
    Box(
        modifier = Modifier
            .size(140.dp) // Square size
            .clip(RoundedCornerShape(8.dp))
            .background(brush = Brush.verticalGradient(gradientColors))
            .clickable(enabled = isAvailable) { onClick() }
    ) {
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
        
        // Text content - always at the bottom
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