package com.example.purrytify.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.purrytify.domain.model.Song
import com.example.purrytify.ui.components.LoadingView
import com.example.purrytify.ui.components.NewSongItem
import com.example.purrytify.ui.components.SongListItem
import com.example.purrytify.ui.screens.library.LibraryUiState
import com.example.purrytify.ui.screens.library.LibraryViewModel
import com.example.purrytify.ui.theme.PurrytifyBlack
import com.example.purrytify.ui.theme.PurrytifyLightGray
import com.example.purrytify.ui.theme.PurrytifyWhite
import com.example.purrytify.ui.theme.Typography

@Composable
fun HomeScreen(
    onNavigateToPlayer: (Long) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PurrytifyBlack)
    ) {
        when (val state = uiState) {
            is LibraryUiState.Loading -> {
                LoadingView()
            }
            is LibraryUiState.Error -> {
                // Show error message
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        color = PurrytifyLightGray
                    )
                }
            }
            is LibraryUiState.Success, is LibraryUiState.Empty -> {
                // Get songs for "New songs" section
                val allSongs = if (state is LibraryUiState.Success) state.songs else emptyList()

                // Sort songs by addedAt (newest first) for the New Songs section
                val newSongs = allSongs.sortedByDescending { it.addedAt }.take(10)

                // Get songs from view model's managed state
                val recentlyPlayed = viewModel.getRecentlyPlayedSongs()

                HomeContent(
                    newSongs = newSongs,
                    recentlyPlayedSongs = recentlyPlayed,
                    onSongClick = { song ->
                        viewModel.playSong(song)
                        onNavigateToPlayer(song.id)
                    }
                )
            }
        }
    }
}

@Composable
fun HomeContent(
    newSongs: List<Song>,
    recentlyPlayedSongs: List<Song>,
    currentPlayingSongId: Long? = null,
    onSongClick: (Song) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp) // Reduced spacing between items
    ) {
        item {
            Spacer(modifier = Modifier.height(24.dp))

            // New Songs Section
            Text(
                text = "New songs",
                style = Typography.titleLarge,
                color = PurrytifyWhite,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                            onClick = { onSongClick(song) }
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

            Spacer(modifier = Modifier.height(8.dp)) // Reduced spacing
        }

        if (recentlyPlayedSongs.isEmpty()) {
            item {
                Text(
                    text = "No recently played songs",
                    style = Typography.bodyLarge,
                    color = PurrytifyLightGray,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        } else {
            items(recentlyPlayedSongs) { song ->
                SongListItem(
                    song = song,
                    isPlaying = currentPlayingSongId == song.id,
                    onClick = { onSongClick(song) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp)) // Bottom spacing for mini player
        }
    }
}