package com.example.purrytify.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.purrytify.R
import com.example.purrytify.domain.model.PlaylistItem
import com.example.purrytify.domain.model.Song
import com.example.purrytify.ui.components.AudioOutputDialog
import com.example.purrytify.ui.components.DeleteSongConfirmationSheet
import com.example.purrytify.ui.components.EditSongModalBottomSheet
import com.example.purrytify.ui.components.ShareSongBottomSheet
import com.example.purrytify.ui.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    songId: String,
    onBackPressed: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val currentItem by viewModel.currentItem.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()

    val showEditDialog by viewModel.showEditDialog.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()
    val showShareDialog by viewModel.showShareDialog.collectAsState()
    val isDownloading by viewModel.isDownloading.collectAsState()
    val isUpdating by viewModel.isUpdating.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isCurrentSongDownloaded by viewModel.isCurrentSongDownloaded.collectAsState()

    // Audio output states
    val showAudioOutputDialog by viewModel.showAudioOutputDialog.collectAsState()
    val availableAudioDevices by viewModel.availableAudioDevices.collectAsState()
    val activeAudioDevice by viewModel.activeAudioDevice.collectAsState()
    val audioOutputError by viewModel.audioOutputError.collectAsState()

    // Dropdown menu state
    var showDropdown by remember { mutableStateOf(false) }

    // Error snackbar
    val snackbarHostState = remember { SnackbarHostState() }

    // Responsive design
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    
    // Calculate artwork size based on orientation
    val artworkSize = when {
        isLandscape -> minOf(250.dp, screenHeight * 0.6f, screenWidth * 0.3f)
        else -> 320.dp
    }

    // Show error message
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    // Show audio output error message
    LaunchedEffect(audioOutputError) {
        audioOutputError?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearAudioOutputError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Back",
                            tint = PurrytifyWhite
                        )
                    }
                },
                actions = {
                    // More options button - show different options based on song type
                    if (currentItem != null) {
                        Box {
                            IconButton(onClick = { showDropdown = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More options",
                                    tint = PurrytifyWhite
                                )
                            }

                            DropdownMenu(
                                expanded = showDropdown,
                                onDismissRequest = { showDropdown = false },
                                modifier = Modifier.background(PurrytifyLighterBlack)
                            ) {
                                when (val item = currentItem) {
                                    is PlaylistItem.LocalSong -> {
                                        // Local songs: Edit and Delete
                                        DropdownMenuItem(
                                            text = { Text("Edit Song", color = PurrytifyWhite) },
                                            onClick = {
                                                showDropdown = false
                                                viewModel.showEditDialog()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Delete Song", color = PurritifyRed) },
                                            onClick = {
                                                showDropdown = false
                                                viewModel.showDeleteDialog()
                                            }
                                        )
                                    }
                                    is PlaylistItem.OnlineSong -> {
                                        if (isCurrentSongDownloaded) {
                                            // Downloaded online songs: Delete only
                                            DropdownMenuItem(
                                                text = { Text("Delete Song", color = PurritifyRed) },
                                                onClick = {
                                                    showDropdown = false
                                                    viewModel.showDeleteDialog()
                                                }
                                            )
                                        } else {
                                            // Online songs: Share options
                                            DropdownMenuItem(
                                                text = { Text("Share Song", color = PurrytifyWhite) },
                                                onClick = {
                                                    showDropdown = false
                                                    viewModel.showShareDialog()
                                                }
                                            )
                                        }
                                    }
                                    null -> {
                                        // No current item
                                    }
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PurrytifyBlack,
                    navigationIconContentColor = PurrytifyWhite,
                    actionIconContentColor = PurrytifyWhite
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PurrytifyBlack) // Solid black background instead of gradient
                .padding(paddingValues)
        ) {
            currentItem?.let { item ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = 24.dp,
                        vertical = if (isLandscape) 16.dp else 32.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(
                        if (isLandscape) 16.dp else 24.dp
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top spacing
                    item {
                        Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 16.dp))
                    }

                    // Album artwork
                    item {
                        Box(
                            modifier = Modifier
                                .size(artworkSize)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            when (item) {
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
                                                .background(PurrytifyDarkGray),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MusicNote,
                                                contentDescription = "Default artwork",
                                                tint = PurrytifyLightGray,
                                                modifier = Modifier.size(if (isLandscape) 60.dp else 80.dp)
                                            )
                                        }
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
                            }
                        }
                    }

                    // Song title and artist
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = item.title,
                                style = if (isLandscape) Typography.titleLarge else Typography.headlineSmall,
                                color = PurrytifyWhite,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = item.artist,
                                style = Typography.bodyLarge,
                                color = PurrytifyLightGray,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Progress bar and time labels
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Slider(
                                value = progress,
                                onValueChange = { newProgress ->
                                    val newPosition = (newProgress * duration).toLong()
                                    viewModel.seekTo(newPosition)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = SliderDefaults.colors(
                                    thumbColor = PurrytifyGreen,
                                    activeTrackColor = PurrytifyGreen,
                                    inactiveTrackColor = PurrytifyDarkGray
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Time labels
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = viewModel.getFormattedCurrentPosition(),
                                    style = Typography.bodySmall,
                                    color = PurrytifyLightGray
                                )

                                Text(
                                    text = viewModel.getFormattedDuration(),
                                    style = Typography.bodySmall,
                                    color = PurrytifyLightGray
                                )
                            }
                        }
                    }

                    // Control buttons
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Like/Download button
                            when (item) {
                                is PlaylistItem.LocalSong -> {
                                    IconButton(
                                        onClick = { viewModel.toggleLike() },
                                        modifier = Modifier.size(56.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (item.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = if (item.isLiked) "Unlike" else "Like",
                                            tint = if (item.isLiked) PurrytifyGreen else PurrytifyWhite,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                                is PlaylistItem.OnlineSong -> {
                                    IconButton(
                                        onClick = { 
                                            if (!isCurrentSongDownloaded) {
                                                viewModel.downloadSong() 
                                            }
                                        },
                                        modifier = Modifier.size(56.dp),
                                        enabled = !isDownloading && !isCurrentSongDownloaded
                                    ) {
                                        when {
                                            isDownloading -> {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(24.dp),
                                                    color = PurrytifyGreen,
                                                    strokeWidth = 2.dp
                                                )
                                            }
                                            isCurrentSongDownloaded -> {
                                                Icon(
                                                    imageVector = Icons.Default.CloudDone,
                                                    contentDescription = "Downloaded",
                                                    tint = PurrytifyGreen,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                            }
                                            else -> {
                                                Icon(
                                                    imageVector = Icons.Default.Download,
                                                    contentDescription = "Download",
                                                    tint = PurrytifyWhite,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(24.dp))

                            // Previous button
                            IconButton(
                                onClick = { viewModel.previous() },
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Previous",
                                    tint = PurrytifyWhite,
                                    modifier = Modifier.size(40.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(24.dp))

                            // Play/Pause button
                            FloatingActionButton(
                                onClick = { viewModel.playPause() },
                                modifier = Modifier.size(72.dp),
                                containerColor = PurrytifyGreen,
                                contentColor = PurrytifyWhite
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    modifier = Modifier.size(40.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(24.dp))

                            // Next button
                            IconButton(
                                onClick = { viewModel.next() },
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Next",
                                    tint = PurrytifyWhite,
                                    modifier = Modifier.size(40.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(24.dp))

                            // Audio routing button
                            IconButton(
                                onClick = { viewModel.showAudioOutputDialog() },
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speaker,
                                    contentDescription = "Audio output",
                                    tint = if (viewModel.isExternalAudioDeviceActive()) PurrytifyGreen else PurrytifyWhite,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    // Bottom spacing for better scrolling experience
                    item {
                        Spacer(modifier = Modifier.height(if (isLandscape) 16.dp else 32.dp))
                    }
                }
            }
        }
    }

    // Edit song dialog
    currentItem?.let { item ->
        if (showEditDialog && item is PlaylistItem.LocalSong) {
            val localSong = Song(
                id = item.id,
                title = item.title,
                artist = item.artist,
                artworkPath = item.artworkPath,
                filePath = item.filePath,
                duration = item.durationMs,
                userId = 0, // This will be handled by the repository
                isLiked = item.isLiked,
                createdAt = java.time.LocalDateTime.now(),
                updatedAt = java.time.LocalDateTime.now()
            )

            EditSongModalBottomSheet(
                isVisible = true,
                song = localSong,
                isLoading = isUpdating,
                errorMessage = errorMessage,
                onDismiss = { viewModel.hideEditDialog() },
                onSave = { title, artist, audioUri, artworkUri ->
                    viewModel.updateSong(title, artist, audioUri, artworkUri)
                }
            )
        }
    }

    // Delete confirmation dialog
    currentItem?.let { item ->
        if (showDeleteDialog) {
            when (item) {
                is PlaylistItem.LocalSong -> {
                    val localSong = Song(
                        id = item.id,
                        title = item.title,
                        artist = item.artist,
                        artworkPath = item.artworkPath,
                        filePath = item.filePath,
                        duration = item.durationMs,
                        userId = 0,
                        isLiked = item.isLiked,
                        createdAt = java.time.LocalDateTime.now(),
                        updatedAt = java.time.LocalDateTime.now()
                    )

                    DeleteSongConfirmationSheet(
                        isVisible = true,
                        song = localSong,
                        onDismiss = { viewModel.hideDeleteDialog() },
                        onConfirmDelete = { viewModel.deleteSong() }
                    )
                }
                is PlaylistItem.OnlineSong -> {
                    if (isCurrentSongDownloaded) {
                        // Create a temporary Song object for the confirmation dialog
                        val tempSong = Song(
                            id = item.originalId,
                            title = item.title,
                            artist = item.artist,
                            artworkPath = "",
                            filePath = "",
                            duration = 0,
                            userId = 0,
                            createdAt = java.time.LocalDateTime.now(),
                            updatedAt = java.time.LocalDateTime.now()
                        )

                        DeleteSongConfirmationSheet(
                            isVisible = true,
                            song = tempSong,
                            onDismiss = { viewModel.hideDeleteDialog() },
                            onConfirmDelete = { viewModel.deleteSong() }
                        )
                    }
                }
            }
        }
    }

    // Share song dialog
    if (showShareDialog) {
        ShareSongBottomSheet(
            isVisible = true,
            song = viewModel.getCurrentOnlineSong(),
            onDismiss = { viewModel.hideShareDialog() }
        )
    }

    // Audio output dialog
    AudioOutputDialog(
        isVisible = showAudioOutputDialog,
        availableDevices = availableAudioDevices,
        activeDevice = activeAudioDevice,
        onDismiss = { viewModel.hideAudioOutputDialog() },
        onDeviceSelected = { device -> viewModel.switchToAudioDevice(device) },
        onRefreshDevices = { viewModel.refreshAudioDevices() }
    )
}