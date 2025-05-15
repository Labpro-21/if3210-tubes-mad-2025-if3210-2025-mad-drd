package com.example.purrytify.ui.screens.topsongs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.purrytify.ui.components.LoadingView
import com.example.purrytify.ui.components.NoInternetScreen
import com.example.purrytify.ui.components.TopSongListItem
import com.example.purrytify.ui.theme.*
import com.example.purrytify.util.CountryUtils

/**
 * Screen for displaying Top Songs (Global or Country)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopSongsScreen(
    isGlobal: Boolean,
    onBack: () -> Unit,
    viewModel: TopSongsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentPlayingSong by viewModel.currentPlayingSong.collectAsState()
    val downloadingState by viewModel.downloadingState.collectAsState()
    val userCountry by viewModel.userCountry.collectAsState()
    val areAllSongsDownloaded by viewModel.areAllSongsDownloaded.collectAsState()
    
    // Load songs when screen is first displayed
    LaunchedEffect(isGlobal) {
        if (isGlobal) {
            viewModel.loadGlobalTopSongs()
        } else {
            viewModel.loadCountryTopSongs()
        }
    }
    
    // Download dialog
    var showDownloadDialog by remember { mutableStateOf(false) }
    
    // Get the title based on the type
    val title = if (isGlobal) "Top 50 Global" else "Top 10 ${CountryUtils.getCountryNameFromCode(userCountry)}"
    val screenType = if (isGlobal) "Global" else CountryUtils.getCountryNameFromCode(userCountry) ?: userCountry
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
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
                    // Download All action - only show if we have songs and not in loading state
                    if (uiState is TopSongsUiState.Success) {
                        when (downloadingState) {
                            // Show download complete icon if all songs downloaded
                            is DownloadState.Success, 
                            is DownloadState.Idle -> {
                                IconButton(
                                    onClick = { 
                                        if (!areAllSongsDownloaded) {
                                            showDownloadDialog = true 
                                        }
                                    }
                                ) {
                                    if (areAllSongsDownloaded) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "All songs downloaded",
                                            tint = PurrytifyGreen
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.CloudDownload,
                                            contentDescription = "Download all songs"
                                        )
                                    }
                                }
                            }
                            // Show progress indicator if downloading
                            is DownloadState.InProgress -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = PurrytifyWhite,
                                    strokeWidth = 2.dp
                                )
                            }
                            else -> { /* Don't show anything for error state */ }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        // Main content with plain black background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PurrytifyBlack)
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is TopSongsUiState.Loading -> {
                    LoadingView()
                }
                
                is TopSongsUiState.Empty -> {
                    EmptyContent(screenType)
                }
                
                is TopSongsUiState.Success -> {
                    SongsList(
                        songs = state.songs,
                        currentPlayingSong = currentPlayingSong,
                        onSongClick = { viewModel.playSong(it) }
                    )
                }
                
                is TopSongsUiState.Error -> {
                    ErrorContent(message = state.message, onRetry = {
                        if (isGlobal) viewModel.loadGlobalTopSongs()
                        else viewModel.loadCountryTopSongs()
                    })
                }
                
                is TopSongsUiState.NoInternet -> {
                    NoInternetScreen()
                }
                
                is TopSongsUiState.UnavailableForCountry -> {
                    UnavailableContent(state.countryCode)
                }
            }
            
            // Show download dialog if requested
            if (showDownloadDialog) {
                DownloadConfirmationDialog(
                    onConfirm = {
                        viewModel.downloadAllSongs()
                        showDownloadDialog = false
                    },
                    onDismiss = { showDownloadDialog = false }
                )
            }
            
            // Show download progress if downloading
            if (downloadingState is DownloadState.InProgress) {
                val progress = (downloadingState as DownloadState.InProgress)
                DownloadProgressOverlay(
                    current = progress.progress,
                    total = progress.total
                )
            }
        }
    }
}

/**
 * Display the list of songs
 */
@Composable
fun SongsList(
    songs: List<com.example.purrytify.domain.model.OnlineSong>,
    currentPlayingSong: com.example.purrytify.domain.model.OnlineSong?,
    onSongClick: (com.example.purrytify.domain.model.OnlineSong) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Additional info text
        item { 
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Your daily update of the most played tracks right now.",
                style = Typography.bodyMedium,
                color = PurrytifyLightGray,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
        
        // Songs list
        itemsIndexed(songs) { index, song ->
            TopSongListItem(
                song = song,
                position = index + 1,
                isPlaying = currentPlayingSong?.id == song.id,
                onClick = { onSongClick(song) }
            )
        }
        
        // Space at the bottom for any player UI
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

/**
 * Empty content display
 */
@Composable
fun EmptyContent(type: String) {
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
                text = "The $type charts is currently empty",
                style = Typography.bodyLarge,
                color = PurrytifyLightGray,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Error content display
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
                )
            ) {
                Text("Retry")
            }
        }
    }
}

/**
 * Content for when top songs are not available for a country
 */
@Composable
fun UnavailableContent(countryCode: String) {
    val countryName = CountryUtils.getCountryNameFromCode(countryCode) ?: countryCode
    val flagEmoji = CountryUtils.getFlagEmoji(countryCode)
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = flagEmoji,
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Not Available",
                style = Typography.titleLarge,
                color = PurrytifyWhite,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Top songs for $countryName are not available",
                style = Typography.bodyLarge,
                color = PurrytifyLightGray,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Top songs are available for: Indonesia, Malaysia, USA, UK, Switzerland, Germany, and Brazil",
                style = Typography.bodyMedium,
                color = PurrytifyLightGray,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Dialog for download confirmation
 */
@Composable
fun DownloadConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Download All Songs")
        },
        text = {
            Text("Do you want to download all songs in this playlist to your device? This may take a while and use significant storage space.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PurrytifyGreen,
                    contentColor = PurrytifyWhite
                )
            ) {
                Text("Download")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = PurrytifyLighterBlack,
        titleContentColor = PurrytifyWhite,
        textContentColor = PurrytifyLightGray
    )
}

/**
 * Overlay showing download progress
 */
@Composable
fun DownloadProgressOverlay(
    current: Int,
    total: Int
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PurrytifyBlack.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(300.dp)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = PurrytifyLighterBlack
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Downloading Songs",
                    style = Typography.titleMedium,
                    color = PurrytifyWhite,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = PurrytifyGreen
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Downloading $current of $total songs",
                    style = Typography.bodyMedium,
                    color = PurrytifyLightGray
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LinearProgressIndicator(
                    progress = { current.toFloat() / total.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                    color = PurrytifyGreen,
                    trackColor = PurrytifyDarkGray
                )
            }
        }
    }
}