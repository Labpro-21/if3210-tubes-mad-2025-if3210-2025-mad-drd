package com.example.purrytify.ui.screens.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.purrytify.domain.model.PlaylistItem
import com.example.purrytify.ui.components.LoadingView
import com.example.purrytify.ui.components.PlaylistItemComponent
import com.example.purrytify.ui.theme.*

/**
 * Screen for displaying the daily playlist
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyPlaylistScreen(
    onBack: () -> Unit,
    onNavigateToPlayer: (String) -> Unit,
    viewModel: DailyPlaylistViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentPlayingItem by viewModel.currentPlayingItem.collectAsState()
    
    // Load playlist when screen is first displayed
    LaunchedEffect(Unit) {
        viewModel.loadDailyPlaylist()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Playlist") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = PurrytifyWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PurrytifyBlack,
                    titleContentColor = PurrytifyWhite,
                    navigationIconContentColor = PurrytifyWhite,
                    actionIconContentColor = PurrytifyWhite
                ),
                actions = {
                    // Refresh action
                    IconButton(onClick = { viewModel.loadDailyPlaylist() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh playlist",
                            tint = PurrytifyWhite
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        // Main content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PurrytifyBlack)
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is PlaylistUiState.Loading -> {
                    LoadingView()
                }
                
                is PlaylistUiState.Empty -> {
                    EmptyContent()
                }
                
                is PlaylistUiState.Success -> {
                    PlaylistContent(
                        items = state.items,
                        currentPlayingItem = currentPlayingItem,
                        onItemClick = { item ->
                            viewModel.playItem(item)

                            onNavigateToPlayer(viewModel.getNavigationId(item))
                        }
                    )
                }
                
                is PlaylistUiState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = { viewModel.loadDailyPlaylist() }
                    )
                }
            }
        }
    }
}

/**
 * Content when the playlist is successfully loaded
 */
@Composable
fun PlaylistContent(
    items: List<PlaylistItem>,
    currentPlayingItem: PlaylistItem?,
    onItemClick: (PlaylistItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Description
        item {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Your personalized daily mix of recommended songs based on your listening habits.",
                style = Typography.bodyMedium,
                color = PurrytifyLightGray,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
        
        // Playlist items
        itemsIndexed(items) { index, item ->
            PlaylistItemComponent(
                item = item,
                position = index + 1,
                isPlaying = item.id == currentPlayingItem?.id,
                onClick = { onItemClick(item) }
            )
        }
        
        // Space at the bottom for any player UI
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

/**
 * Content when the playlist is empty
 */
@Composable
fun EmptyContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "No songs available",
                style = Typography.titleLarge,
                color = PurrytifyWhite,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Your daily playlist is currently empty. Add some songs to your library or check back later.",
                style = Typography.bodyLarge,
                color = PurrytifyLightGray,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Content when there's an error loading the playlist
 */
@Composable
fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Something went wrong",
                style = Typography.titleLarge,
                color = PurrytifyWhite,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = message,
                style = Typography.bodyLarge,
                color = PurrytifyLightGray,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PurrytifyGreen,
                    contentColor = PurrytifyWhite
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Retry")
            }
        }
    }
}