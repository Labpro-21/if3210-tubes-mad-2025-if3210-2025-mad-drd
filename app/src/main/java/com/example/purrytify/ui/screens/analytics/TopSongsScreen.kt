package com.example.purrytify.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.purrytify.domain.model.SongAnalytics
import com.example.purrytify.domain.model.TopSong
import com.example.purrytify.ui.components.LoadingView
import com.example.purrytify.ui.theme.*

/**
 * Top Songs screen showing detailed song analytics for a specific month/year
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopSongsScreen(
    year: Int,
    month: Int,
    onBackPressed: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val songAnalytics by viewModel.songAnalytics.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    // Load song analytics for the specific month when screen opens
    LaunchedEffect(year, month) {
        viewModel.loadSongAnalytics(year, month)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Top songs",
                        color = PurrytifyWhite
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = PurrytifyWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PurrytifyBlack
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            PurrytifyBlack,
                            PurrytifyLighterBlack.copy(alpha = 0.8f),
                            PurrytifyBlack
                        )
                    )
                )
                .padding(paddingValues)
        ) {
            if (isLoading) {
                LoadingView()
            } else {
                songAnalytics?.let { analytics ->
                    if (analytics.hasData) {
                        TopSongsContent(analytics = analytics)
                    } else {
                        NoDataContent(year, month)
                    }
                } ?: run {
                    NoDataContent(year, month)
                }
            }
        }
    }
}

@Composable
private fun TopSongsContent(
    analytics: SongAnalytics,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        // Header with month/year
        Text(
            text = analytics.displayName,
            style = Typography.headlineSmall,
            color = PurrytifyWhite,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Song count info
        Text(
            text = "You played ${analytics.totalSongs} different songs this month.",
            style = Typography.bodyLarge,
            color = PurrytifyLightGray,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Songs list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            itemsIndexed(analytics.songs) { index, song ->
                TopSongItem(
                    song = song,
                    rank = index + 1
                )
            }
        }
    }
}

@Composable
private fun TopSongItem(
    song: TopSong,
    rank: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = PurrytifyLighterBlack
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank number
            Text(
                text = String.format("%02d", rank),
                style = Typography.titleMedium,
                color = if (rank <= 3) PurrytifyGreen else PurrytifyLightGray,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(40.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Song artwork placeholder
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = PurrytifyDarkGray,
                        shape = RoundedCornerShape(4.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Song",
                    tint = PurrytifyLightGray,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Song info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.title,
                    style = Typography.bodyLarge,
                    color = PurrytifyWhite,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = song.artist,
                    style = Typography.bodyMedium,
                    color = PurrytifyLightGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = song.formattedDuration,
                        style = Typography.bodySmall,
                        color = PurrytifyGreen
                    )
                    
                    Text(
                        text = "•",
                        style = Typography.bodySmall,
                        color = PurrytifyLightGray
                    )
                    
                    Text(
                        text = "${song.playCount} plays",
                        style = Typography.bodySmall,
                        color = PurrytifyLightGray
                    )
                }
            }
        }
    }
}

@Composable
private fun NoDataContent(
    year: Int,
    month: Int,
    modifier: Modifier = Modifier
) {
    val monthName = java.time.Month.of(month).name.lowercase()
        .replaceFirstChar { it.uppercase() }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = "No songs",
            tint = PurrytifyDarkGray,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No data available",
            style = Typography.headlineSmall,
            color = PurrytifyWhite,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "No songs played in $monthName $year",
            style = Typography.bodyLarge,
            color = PurrytifyLightGray,
            textAlign = TextAlign.Center
        )
    }
}