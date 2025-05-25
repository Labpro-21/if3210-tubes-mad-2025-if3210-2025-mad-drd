package com.example.purrytify.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.purrytify.domain.model.ArtistAnalytics
import com.example.purrytify.domain.model.TopArtist
import com.example.purrytify.ui.components.LoadingView
import com.example.purrytify.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopArtistsScreen(
    year: Int,
    month: Int,
    onBackPressed: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val artistAnalytics by viewModel.artistAnalytics.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    // Load artist analytics for the specific month when screen opens
    LaunchedEffect(year, month) {
        viewModel.loadArtistAnalytics(year, month)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Top artists",
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
                .background(PurrytifyBlack)
                .padding(paddingValues)
        ) {
            if (isLoading) {
                LoadingView()
            } else {
                artistAnalytics?.let { analytics ->
                    if (analytics.hasData) {
                        TopArtistsContent(analytics = analytics)
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
private fun TopArtistsContent(
    analytics: ArtistAnalytics,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = analytics.displayName,
                    style = Typography.headlineSmall,
                    color = PurrytifyWhite,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "You listened to ${analytics.totalArtists} artists this month.",
                    style = Typography.bodyLarge,
                    color = PurrytifyLightGray
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        
        // Artists list
        itemsIndexed(analytics.artists) { index, artist ->
            TopArtistItem(
                artist = artist,
                rank = index + 1
            )
        }
        
        // Add bottom spacing for better scrolling experience
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TopArtistItem(
    artist: TopArtist,
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
            
            // Artist avatar placeholder
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = PurrytifyDarkGray,
                        shape = RoundedCornerShape(28.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Artist",
                    tint = PurrytifyLightGray,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Artist info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = artist.name,
                    style = Typography.bodyLarge,
                    color = PurrytifyWhite,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = artist.formattedDuration,
                        style = Typography.bodySmall,
                        color = PurrytifyGreen
                    )
                    
                    Text(
                        text = "•",
                        style = Typography.bodySmall,
                        color = PurrytifyLightGray
                    )
                    
                    Text(
                        text = "${artist.playCount} plays",
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
    
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "No artists",
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
                    text = "No artists played in $monthName $year",
                    style = Typography.bodyLarge,
                    color = PurrytifyLightGray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}