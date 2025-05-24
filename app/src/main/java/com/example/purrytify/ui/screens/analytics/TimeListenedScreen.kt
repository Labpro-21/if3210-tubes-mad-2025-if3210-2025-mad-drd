package com.example.purrytify.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.purrytify.domain.model.MonthlyAnalytics
import com.example.purrytify.ui.components.LoadingView
import com.example.purrytify.ui.theme.*

/**
 * Time Listened detail screen showing daily chart and listening statistics
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeListenedScreen(
    onBackPressed: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val currentMonthAnalytics by viewModel.currentMonthAnalytics.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Time listened",
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
                currentMonthAnalytics?.let { analytics ->
                    TimeListenedContent(analytics = analytics)
                } ?: run {
                    NoDataContent()
                }
            }
        }
    }
}

@Composable
private fun TimeListenedContent(
    analytics: MonthlyAnalytics,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Month header
        Text(
            text = "${analytics.monthName} ${analytics.year}",
            style = Typography.headlineSmall,
            color = PurrytifyWhite,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Main listening time display
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = PurrytifyLighterBlack
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "You listened to music for",
                    style = Typography.bodyLarge,
                    color = PurrytifyLightGray
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = analytics.formattedListeningTime,
                    style = Typography.displaySmall,
                    color = PurrytifyGreen,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "this month.",
                    style = Typography.bodyLarge,
                    color = PurrytifyLightGray
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Daily average
        val totalMinutes = analytics.totalListeningTimeMs / (1000 * 60)
        val daysInMonth = java.time.YearMonth.of(analytics.year, analytics.month).lengthOfMonth()
        val dailyAverage = if (totalMinutes > 0) totalMinutes / daysInMonth else 0
        
        Text(
            text = "Daily average: $dailyAverage min",
            style = Typography.bodyLarge,
            color = PurrytifyLightGray
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Daily Chart placeholder
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            colors = CardDefaults.cardColors(
                containerColor = PurrytifyLighterBlack.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Daily Chart",
                        style = Typography.titleMedium,
                        color = PurrytifyWhite,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Minutes",
                        style = Typography.bodySmall,
                        color = PurrytifyLightGray
                    )
                    
                    // Simple chart representation
                    if (analytics.dailyData.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(0.8f),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            repeat(7) { day ->
                                val hasData = analytics.dailyData.size > day
                                val height = if (hasData) {
                                    (analytics.dailyData[day].totalDurationMs / (1000 * 60) / 10).toInt().coerceAtMost(40).dp
                                } else {
                                    4.dp
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .width(8.dp)
                                        .height(height.coerceAtLeast(4.dp))
                                        .background(
                                            color = if (hasData) PurrytifyGreen else PurrytifyDarkGray,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "day",
                            style = Typography.bodySmall,
                            color = PurrytifyLightGray
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoDataContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No data available",
            style = Typography.headlineSmall,
            color = PurrytifyWhite,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Start listening to music to see your time analytics",
            style = Typography.bodyLarge,
            color = PurrytifyLightGray,
            textAlign = TextAlign.Center
        )
    }
}