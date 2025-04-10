package com.example.purrytify.ui.screens.library

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.purrytify.domain.model.Song
import com.example.purrytify.ui.components.AddSongModalBottomSheet
import com.example.purrytify.ui.components.LoadingView
import com.example.purrytify.ui.components.MiniPlayerBar
import com.example.purrytify.ui.components.SongListItem
import com.example.purrytify.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateToPlayer: (Long) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val showAddSongDialog by viewModel.showAddSongDialog.collectAsState()
    val isAddingLoading by viewModel.isAddingLoading.collectAsState()
    val addSongError by viewModel.addSongError.collectAsState()
    
    val currentPlayingSong by viewModel.currentPlayingSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progress by viewModel.progress.collectAsState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PurrytifyBlack)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header with title and add button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Library",
                    style = Typography.titleLarge,
                    color = PurrytifyWhite,
                    fontWeight = FontWeight.Bold
                )
                
                // Add song button
                IconButton(
                    onClick = { viewModel.showAddSongDialog() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Song",
                        tint = PurrytifyWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            
            // Tab filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .padding(horizontal = 16.dp)
                ,
                horizontalArrangement = Arrangement.Start
            ) {
                // All tab
                FilterChip(
                    selected = activeTab == LibraryTab.ALL,
                    onClick = { viewModel.switchTab(LibraryTab.ALL) },
                    label = {
                        Text(
                            text = "All",
                            style = Typography.bodyMedium.copy(
                                lineHeight = 0.sp,
                            ),
                            fontWeight = FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = if (activeTab == LibraryTab.ALL) PurrytifyGreen else PurrytifyLighterBlack,
                        labelColor = if (activeTab == LibraryTab.ALL) PurrytifyBlack else PurrytifyWhite,
                        selectedContainerColor = PurrytifyGreen,
                        selectedLabelColor = PurrytifyBlack
                    ),
                    shape = CircleShape,
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (activeTab == LibraryTab.ALL) PurrytifyGreen else PurrytifyLighterBlack,
                        selectedBorderColor = PurrytifyGreen,
                        borderWidth = 1.dp,
                        selectedBorderWidth = 0.dp
                    )
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Liked tab
                FilterChip(
                    selected = activeTab == LibraryTab.LIKED,
                    onClick = { viewModel.switchTab(LibraryTab.LIKED) },
                    label = {
                        Text(
                            text = "Liked",
                            style = Typography.bodyMedium.copy(
                                lineHeight = 0.sp,
                            ),
                            fontWeight = FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = if (activeTab == LibraryTab.LIKED) PurrytifyGreen else PurrytifyLighterBlack,
                        labelColor = if (activeTab == LibraryTab.LIKED) PurrytifyBlack else PurrytifyWhite,
                        selectedContainerColor = PurrytifyGreen,
                        selectedLabelColor = PurrytifyBlack
                    ),
                    shape = CircleShape,
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (activeTab == LibraryTab.LIKED) PurrytifyGreen else PurrytifyLighterBlack,
                        selectedBorderColor = PurrytifyGreen,
                        borderWidth = 1.dp,
                        selectedBorderWidth = 0.dp
                    )
                )
            }

            // Main content based on UI state
            when (uiState) {
                is LibraryUiState.Loading -> {
                    LoadingView()
                }
                
                is LibraryUiState.Empty -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.LibraryMusic,
                                contentDescription = "No Songs",
                                tint = PurrytifyLightGray,
                                modifier = Modifier.size(64.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = "No music available",
                                style = Typography.titleMedium,
                                color = PurrytifyLightGray
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = if (activeTab == LibraryTab.ALL) 
                                    "Tap the + button to add songs" 
                                else 
                                    "Like some songs to see them here",
                                style = Typography.bodyMedium,
                                color = PurrytifyLightGray
                            )
                        }
                    }
                }
                
                is LibraryUiState.Success -> {
                    val songs = (uiState as LibraryUiState.Success).songs
                    
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .weight(1f),
                    ) {
                        items(songs) { song ->
                            SongListItem(
                                song = song,
                                isPlaying = currentPlayingSong?.id == song.id,
                                onClick = { viewModel.playSong(it) }
                            )
                        }
                    }
                }
                
                is LibraryUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (uiState as LibraryUiState.Error).message,
                            style = Typography.bodyLarge,
                            color = PurritifyRed
                        )
                    }
                }
            }
        }
        
        // Add song modal
        AddSongModalBottomSheet(
            isVisible = showAddSongDialog,
            isLoading = isAddingLoading,
            errorMessage = addSongError,
            onDismiss = { viewModel.dismissAddSongDialog() },
            onSave = { audioUri, title, artist, artworkUri ->
                viewModel.addSong(audioUri, title, artist, artworkUri)
            }
        )
    }
}