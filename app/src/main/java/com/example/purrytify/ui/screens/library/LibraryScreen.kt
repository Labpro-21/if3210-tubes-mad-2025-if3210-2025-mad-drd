package com.example.purrytify.ui.screens.library

import android.content.res.Configuration
import android.net.Uri
import android.util.Log
import android.view.ContextThemeWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.purrytify.R
import com.example.purrytify.ui.components.AddSongModalBottomSheet
import com.example.purrytify.ui.components.LoadingView
import com.example.purrytify.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateToPlayer: (String) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val showAddSongDialog by viewModel.showAddSongDialog.collectAsState()
    val isAddingLoading by viewModel.isAddingLoading.collectAsState()
    val addSongError by viewModel.addSongError.collectAsState()
    val currentPlayingSong by viewModel.currentPlayingSong.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Use different padding values for landscape and portrait
    val topPadding = if (isLandscape) 16.dp else 24.dp
    val horizontalPadding = if (isLandscape) 24.dp else 16.dp

    // Reset state when screen is disposed (navigating away)
    DisposableEffect(key1 = Unit) {
        onDispose {
            viewModel.resetState()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PurrytifyBlack)
    ) {
        // Use a Column with a Box layout to make the header sticky
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header section with fixed position
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PurrytifyBlack)
                    .zIndex(1f)
                    .shadow(elevation = 4.dp)
            ) {
                // Header with title and add button
                Row(
                    modifier = Modifier
                        .padding(top = topPadding)
                        .padding(horizontal = horizontalPadding)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.your_library),
                        style = MaterialTheme.typography.titleLarge,
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
                            contentDescription = stringResource(R.string.add_song),
                            tint = PurrytifyWhite,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Search Bar
                SearchBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Tab filters
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .padding(horizontal = horizontalPadding),
                    horizontalArrangement = Arrangement.Start
                ) {
                    // Custom chip buttons
                    TabChip(
                        text = stringResource(R.string.all),
                        isSelected = activeTab == LibraryTab.ALL,
                        onClick = { viewModel.switchTab(LibraryTab.ALL) }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    TabChip(
                        text = stringResource(R.string.liked),
                        isSelected = activeTab == LibraryTab.LIKED,
                        onClick = { viewModel.switchTab(LibraryTab.LIKED) }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    TabChip(
                        text = stringResource(R.string.downloaded),
                        isSelected = activeTab == LibraryTab.DOWNLOADED,
                        onClick = { viewModel.switchTab(LibraryTab.DOWNLOADED) }
                    )
                }
            }

            // Content area that scrolls underneath the header
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PurrytifyBlack)
            ) {
                // Main content based on UI state
                when (uiState) {
                    is LibraryUiState.Loading -> {
                        LoadingView()
                    }

                    is LibraryUiState.Empty -> {
                        EmptyStateContent(
                            activeTab = activeTab,
                            searchQuery = searchQuery
                        )
                    }

                    is LibraryUiState.Success -> {
                        val songs = (uiState as LibraryUiState.Success).songs

                        AndroidView(
                            factory = { context ->
                                try {
                                    val themedContext = ContextThemeWrapper(context, R.style.Theme_Purrytify)
                                    RecyclerView(themedContext).apply {
                                        layoutManager = LinearLayoutManager(themedContext)
                                        adapter = SongsAdapter(songs, currentPlayingSong) { song ->
                                            viewModel.playSong(song)
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("AndroidViewError", "Error inflating RecyclerView", e)
                                    throw e
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = horizontalPadding),
                            update = { recyclerView ->
                                (recyclerView.adapter as? SongsAdapter)?.apply {
                                    updateSongs(songs)
                                    updatePlayingSong(currentPlayingSong)
                                }
                            }
                        )
                    }

                    is LibraryUiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (uiState as LibraryUiState.Error).message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = PurritifyRed
                            )
                        }
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

@Composable
fun TabChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(32.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) PurrytifyGreen else PurrytifyLighterBlack,
            contentColor = if (isSelected) PurrytifyBlack else PurrytifyWhite
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                lineHeight = 0.sp,
            ),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun EmptyStateContent(
    activeTab: LibraryTab,
    searchQuery: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.LibraryMusic,
                contentDescription = null,
                tint = PurrytifyLightGray,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (searchQuery.isNotEmpty())
                    stringResource(R.string.no_matching_songs)
                else
                    stringResource(R.string.no_songs_available),
                style = MaterialTheme.typography.titleMedium,
                color = PurrytifyLightGray
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (searchQuery.isNotEmpty())
                    stringResource(R.string.try_different_search)
                else when (activeTab) {
                    LibraryTab.ALL -> stringResource(R.string.tap_to_add_songs)
                    LibraryTab.LIKED -> stringResource(R.string.like_songs_to_see_them)
                    LibraryTab.DOWNLOADED -> stringResource(R.string.download_songs_to_see_them)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = PurrytifyLightGray
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = {
            Text(
                text = stringResource(R.string.search_songs_or_artists),
                color = PurrytifyLightGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = PurrytifyLightGray
            )
        },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = null,
                        tint = PurrytifyLightGray
                    )
                }
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        colors = TextFieldDefaults.colors(
            focusedTextColor = PurrytifyWhite,
            unfocusedTextColor = PurrytifyWhite,
            focusedContainerColor = PurrytifyLighterBlack,
            unfocusedContainerColor = PurrytifyLighterBlack,
            cursorColor = PurrytifyGreen,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedPlaceholderColor = PurrytifyLightGray,
            unfocusedPlaceholderColor = PurrytifyLightGray,
            focusedLeadingIconColor = PurrytifyLightGray,
            unfocusedLeadingIconColor = PurrytifyLightGray,
            focusedTrailingIconColor = PurrytifyLightGray,
            unfocusedTrailingIconColor = PurrytifyLightGray
        ),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )
}