package com.example.purrytify.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.purrytify.R
import com.example.purrytify.domain.model.PlaylistItem
import com.example.purrytify.ui.screens.player.PlayerViewModel
import com.example.purrytify.ui.theme.*
import java.io.File

/**
 * Mini player that appears at the bottom of screens when music is playing
 */
@Composable
fun MiniPlayer(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val currentItem by viewModel.currentItem.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val activeAudioDevice by viewModel.activeAudioDevice.collectAsState()
    
    if (currentItem != null) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .height(72.dp)
                .clickable { onClick() },
            colors = CardDefaults.cardColors(
                containerColor = PurrytifyLighterBlack
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            ),
            shape = RoundedCornerShape(0.dp)
        ) {
            Column {
                // Progress bar at the top
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = PurrytifyGreen,
                    trackColor = PurrytifyDarkGray.copy(alpha = 0.3f)
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Artwork
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(4.dp))
                    ) {
                        when (val item = currentItem) {
                            is PlaylistItem.LocalSong -> {
                                if (item.artworkPath.isNotEmpty() && File(item.artworkPath).exists()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data("file://${item.artworkPath}")
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Song Artwork",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(PurrytifyDarkGray)
                                    )
                                }
                            }
                            is PlaylistItem.OnlineSong -> {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(item.artworkUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Song Artwork",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                    error = painterResource(id = R.drawable.ic_launcher_foreground)
                                )
                            }
                            null -> {
                                // This shouldn't happen, but just in case
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(PurrytifyDarkGray)
                                )
                            }
                        }
                    }
                    
                    // Song info
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = currentItem?.title ?: "",
                            style = Typography.bodyMedium,
                            color = PurrytifyWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Text(
                            text = currentItem?.artist ?: "",
                            style = Typography.bodySmall,
                            color = PurrytifyLightGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    // Audio device indicator (show if not using built-in speaker)
                    if (activeAudioDevice != null && activeAudioDevice?.name != "Phone Speaker") {
                        Icon(
                            imageVector = Icons.Default.Speaker,
                            contentDescription = "External audio device active",
                            tint = PurrytifyGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    // Like button (only for local songs)
                    if (currentItem is PlaylistItem.LocalSong) {
                        IconButton(
                            onClick = { viewModel.toggleLike() }
                        ) {
                            Icon(
                                imageVector = if (viewModel.isCurrentSongLiked()) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (viewModel.isCurrentSongLiked()) "Unlike" else "Like",
                                tint = if (viewModel.isCurrentSongLiked()) PurrytifyGreen else PurrytifyLightGray
                            )
                        }
                    }
                    
                    // Play/Pause button
                    IconButton(
                        onClick = { viewModel.playPause() }
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = PurrytifyWhite,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}