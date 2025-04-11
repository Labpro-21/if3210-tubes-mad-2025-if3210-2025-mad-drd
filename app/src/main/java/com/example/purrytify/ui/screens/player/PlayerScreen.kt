package com.example.purrytify.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.purrytify.R
import com.example.purrytify.domain.model.Song
import com.example.purrytify.ui.components.DeleteSongConfirmationSheet
import com.example.purrytify.ui.components.EditSongModalBottomSheet
import com.example.purrytify.ui.screens.library.LibraryViewModel
import com.example.purrytify.ui.theme.*
import com.example.purrytify.ui.screens.library.RepeatMode

@Composable
fun PlayerScreen(
    songId: String?,
    onNavigateBack: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val currentPlayingSong by viewModel.currentPlayingSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()

    val showEditSongDialog by viewModel.showEditSongDialog.collectAsState()
    val showDeleteSongDialog by viewModel.showDeleteSongDialog.collectAsState()
    val currentEditSong by viewModel.currentEditSong.collectAsState()
    val isAddingLoading by viewModel.isAddingLoading.collectAsState()
    val addSongError by viewModel.addSongError.collectAsState()

    var showMenu by remember { mutableStateOf(false) }

    // Get the current song to display
    val song = currentPlayingSong
    if (song == null) {
        LaunchedEffect(Unit) {
            onNavigateBack()
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                // Use album art color or default to red gradient
                color = PurrytifyLighterBlack
            )
    ) {
        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar with back button and options
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Back",
                        tint = PurrytifyWhite,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Options menu
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = PurrytifyWhite,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier
                            .background(PurrytifyLighterBlack)
                            .width(200.dp)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Edit Song",
                                    color = PurrytifyWhite,
                                    style = Typography.bodyMedium
                                )
                            },
                            onClick = {
                                viewModel.showEditSongDialog(song)
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = PurrytifyWhite
                                )
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = PurrytifyWhite,
                                leadingIconColor = PurrytifyWhite
                            )
                        )

                        Divider(
                            color = PurrytifyDarkGray,
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Delete Song",
                                    color = PurritifyRed,
                                    style = Typography.bodyMedium
                                )
                            },
                            onClick = {
                                viewModel.showDeleteSongDialog(song)
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = PurritifyRed
                                )
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = PurritifyRed,
                                leadingIconColor = PurritifyRed
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Album artwork
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(song.artworkUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "${song.title} by ${song.artist}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    error = painterResource(id = R.drawable.ic_launcher_foreground)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Song info
            Text(
                text = song.title,
                style = Typography.titleLarge,
                color = PurrytifyWhite,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = song.artist,
                style = Typography.bodyLarge,
                color = PurrytifyLightGray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Progress slider
            Slider(
                value = progress,
                onValueChange = { viewModel.updateProgress(it) },
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = PurrytifyWhite,
                    activeTrackColor = PurrytifyGreen,
                    inactiveTrackColor = PurrytifyWhite.copy(alpha = 0.3f)
                )
            )

            // Time indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val formattedCurrentTime = remember(currentPosition) {
                    val minutes = currentPosition / 1000 / 60
                    val seconds = (currentPosition / 1000) % 60
                    String.format("%d:%02d", minutes, seconds)
                }

                Text(
                    text = formattedCurrentTime,
                    style = Typography.bodyMedium,
                    color = PurrytifyLightGray
                )

                Text(
                    text = song.formattedDuration,
                    style = Typography.bodyMedium,
                    color = PurrytifyLightGray
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Playback controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle button (NEW)
                IconButton(
                    onClick = { viewModel.toggleShuffle() }, // You'll need to add this method to your ViewModel
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (viewModel.isShuffleEnabled.collectAsState().value) PurrytifyGreen else PurrytifyWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Previous button
                IconButton(
                    onClick = { viewModel.playPreviousSong() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = PurrytifyWhite,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Play/Pause button
                IconButton(
                    onClick = { viewModel.togglePlayPause() },
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(50))
                        .background(PurrytifyWhite)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Next button
                IconButton(
                    onClick = { viewModel.playNextSong() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = PurrytifyWhite,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Repeat button
                IconButton(
                    onClick = { viewModel.toggleRepeatMode() },
                    modifier = Modifier.size(48.dp)
                ) {
                    val repeatMode by viewModel.repeatMode.collectAsState()
                    Icon(
                        imageVector = when (repeatMode) {
                            RepeatMode.OFF -> Icons.Default.RepeatOne // Using RepeatOne icon but will tint differently
                            RepeatMode.ALL -> Icons.Default.Repeat
                            RepeatMode.ONE -> Icons.Default.RepeatOne
                        },
                        contentDescription = "Repeat",
                        tint = when (repeatMode) {
                            RepeatMode.OFF -> PurrytifyWhite
                            RepeatMode.ALL, RepeatMode.ONE -> PurrytifyGreen
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // For handling the smart cast issue with currentEditSong
        currentEditSong?.let { editSong ->
            // Edit song modal
            if (showEditSongDialog) {
                EditSongModalBottomSheet(
                    isVisible = true,
                    song = editSong,
                    isLoading = isAddingLoading,
                    errorMessage = addSongError,
                    onDismiss = { viewModel.dismissEditSongDialog() },
                    onSave = { title, artist, audioUri, artworkUri ->
                        viewModel.updateSong(editSong, title, artist, artworkUri)
                    }
                )
            }

            // Delete song confirmation
            if (showDeleteSongDialog) {
                DeleteSongConfirmationSheet(
                    isVisible = true,
                    song = editSong,
                    onDismiss = { viewModel.dismissDeleteSongDialog() },
                    onConfirmDelete = { songToDelete ->
                        viewModel.deleteSong(songToDelete)
                        onNavigateBack()
                    }
                )
            }
        }
    }
}