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
 * Time Listened detail screen showing daily chart and listening statistics for a specific month
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeListenedScreen(
    year: Int,
    month: Int,
    onBackPressed: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val specificMonthAnalytics by viewModel.specificMonthAnalytics.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    // Load analytics for the specific month when screen opens
    LaunchedEffect(year, month) {
        viewModel.loadAnalyticsForMonth(year, month)
    }
    
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
                specificMonthAnalytics?.let { analytics ->
                    if (analytics.hasData) {
                        TimeListenedContent(analytics = analytics)
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
            text = analytics.displayName,
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
        
        // Daily Chart
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            colors = CardDefaults.cardColors(
                containerColor = PurrytifyLighterBlack.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Daily Chart",
                    style = Typography.titleMedium,
                    color = PurrytifyWhite,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Chart content
                if (analytics.dailyData.isNotEmpty()) {
                    DailyChart(
                        dailyData = analytics.dailyData,
                        daysInMonth = daysInMonth,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No daily data available",
                                style = Typography.bodyMedium,
                                color = PurrytifyLightGray
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = "Start listening to see daily charts",
                                style = Typography.bodySmall,
                                color = PurrytifyDarkGray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyChart(
    dailyData: List<com.example.purrytify.domain.model.DailyListeningData>,
    daysInMonth: Int,
    modifier: Modifier = Modifier
) {
    // Create a map for quick lookup of daily data
    val dataMap = dailyData.associateBy { it.dayOfMonth }
    val maxDuration = dailyData.maxOfOrNull { it.totalDurationMs } ?: 1L
    
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Y-axis label
        Text(
            text = "Minutes",
            style = Typography.bodySmall,
            color = PurrytifyLightGray,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(bottom = 8.dp)
        )
        
        // Chart area
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 8.dp, top = 20.dp, bottom = 20.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            // Show bars for each day of the month
            val daysToShow = minOf(daysInMonth, 31)
            
            repeat(daysToShow) { index ->
                val day = index + 1
                val dayData = dataMap[day]
                val hasData = dayData != null
                val duration = dayData?.totalDurationMs ?: 0L
                
                // Calculate bar height (minimum 4dp, maximum 80% of available height)
                val heightFraction = if (hasData && maxDuration > 0) {
                    (duration.toFloat() / maxDuration.toFloat()).coerceIn(0.05f, 0.8f)
                } else {
                    0.05f
                }
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    // Bar
                    Box(
                        modifier = Modifier
                            .width(8.dp)
                            .fillMaxHeight(heightFraction)
                            .background(
                                color = if (hasData) PurrytifyGreen else PurrytifyDarkGray.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
                            )
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Day label (show every 5th day to avoid crowding)
                    if (day % 5 == 1 || day == daysInMonth) {
                        Text(
                            text = day.toString(),
                            style = Typography.bodySmall,
                            color = PurrytifyLightGray,
                            fontSize = Typography.bodySmall.fontSize * 0.8f
                        )
                    }
                }
            }
        }
        
        // X-axis label
        Text(
            text = "Day",
            style = Typography.bodySmall,
            color = PurrytifyLightGray,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(top = 8.dp)
        )
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
        Text(
            text = "No data available",
            style = Typography.headlineSmall,
            color = PurrytifyWhite,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No listening data for $monthName $year",
            style = Typography.bodyLarge,
            color = PurrytifyLightGray,
            textAlign = TextAlign.Center
        )
    }
}