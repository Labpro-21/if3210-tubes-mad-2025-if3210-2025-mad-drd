package com.example.purrytify.ui.screens.analytics

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.purrytify.domain.model.MonthlyAnalytics
import com.example.purrytify.ui.components.LoadingView
import com.example.purrytify.ui.theme.*
import kotlin.math.max

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
                .background(PurrytifyBlack)
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
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val screenHeight = configuration.screenHeightDp.dp
    
    // Calculate chart height based on orientation and screen size
    val chartHeight = when {
        isLandscape -> maxOf(300.dp, screenHeight * 0.4f)
        else -> 400.dp
    }
    
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Month header
        item {
            Text(
                text = analytics.displayName,
                style = Typography.headlineSmall,
                color = PurrytifyWhite,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
        
        // Main listening time display card
        item {
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
        }
        
        // Daily average info
        item {
            val totalMinutes = analytics.totalListeningTimeMs / (1000 * 60)
            val daysInMonth = java.time.YearMonth.of(analytics.year, analytics.month).lengthOfMonth()
            val dailyAverage = if (totalMinutes > 0) totalMinutes / daysInMonth else 0
            
            Text(
                text = "Daily average: $dailyAverage min",
                style = Typography.bodyLarge,
                color = PurrytifyLightGray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
        
        // Daily Chart
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight),
                colors = CardDefaults.cardColors(
                    containerColor = PurrytifyLighterBlack.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = 16.dp,
                            vertical = if (isLandscape) 16.dp else 32.dp,
                        )
                ) {
                    Text(
                        text = "Daily Chart",
                        style = Typography.titleMedium,
                        color = PurrytifyWhite,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Chart content
                    if (analytics.dailyData.isNotEmpty()) {
                        val daysInMonth = java.time.YearMonth.of(analytics.year, analytics.month).lengthOfMonth()
                        CustomBarChart(
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
        
        // Add bottom spacing for better scrolling experience
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CustomBarChart(
    dailyData: List<com.example.purrytify.domain.model.DailyListeningData>,
    daysInMonth: Int,
    modifier: Modifier = Modifier
) {
    // Create a map for quick lookup of daily data
    val dataMap = dailyData.associateBy { it.dayOfMonth }
    val maxDurationMs = dailyData.maxOfOrNull { it.totalDurationMs } ?: 1L
    val maxMinutes = max(1, (maxDurationMs / (1000 * 60)).toInt())
    
    // Calculate Y-axis tick values
    val yAxisTicks = when {
        maxMinutes <= 5 -> listOf(0, 1, 2, 3, 4, 5)
        maxMinutes <= 10 -> listOf(0, 2, 4, 6, 8, 10)
        maxMinutes <= 30 -> listOf(0, 5, 10, 15, 20, 25, 30)
        maxMinutes <= 60 -> listOf(0, 10, 20, 30, 40, 50, 60)
        maxMinutes <= 120 -> listOf(0, 20, 40, 60, 80, 100, 120)
        else -> {
            val step = (maxMinutes / 5) / 10 * 10 // Round to nearest 10
            (0..5).map { it * step }
        }
    }
    
    val actualMaxMinutes = yAxisTicks.maxOrNull() ?: maxMinutes
    
    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        
        // Chart area dimensions
        val chartLeft = 50f
        val chartTop = 20f
        val chartRight = canvasWidth - 20f
        val chartBottom = canvasHeight - 40f
        val chartWidth = chartRight - chartLeft
        val chartHeight = chartBottom - chartTop
        
        // Draw Y-axis labels
        yAxisTicks.reversed().forEachIndexed { index, tick ->
            val y = chartTop + (index * chartHeight / (yAxisTicks.size - 1))

            val paint = android.graphics.Paint().apply {
                color = PurrytifyLightGray.hashCode()
                textSize = 32f
                isAntiAlias = true
            }

            val tickText = tick.toString()
            val textWidth = paint.measureText(tickText)

            val rightAlignPosition = 25f
            val x = rightAlignPosition - textWidth

            drawContext.canvas.nativeCanvas.drawText(
                tickText,
                x,
                y + 5f,
                paint
            )
        }
        
        // Draw bars
        val barWidth = chartWidth / daysInMonth
        val barSpacing = barWidth * 0.1f
        val actualBarWidth = barWidth - barSpacing
        
        for (day in 1..daysInMonth) {
            val dayData = dataMap[day]
            val durationMinutes = (dayData?.totalDurationMs ?: 0L) / (1000 * 60)
            
            if (durationMinutes > 0) {
                val barHeight = (durationMinutes.toFloat() / actualMaxMinutes.toFloat()) * chartHeight
                val x = chartLeft + (day - 1) * barWidth + barSpacing / 2
                val y = chartBottom - barHeight
                
                // Draw bar with rounded top corners
                drawRoundRect(
                    color = PurrytifyGreen,
                    topLeft = Offset(x, y),
                    size = Size(actualBarWidth, barHeight),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }
        }
        
        // Draw X-axis labels (show every 5th day)
        for (day in 1..daysInMonth step 5) {
            val x = chartLeft + (day - 1) * barWidth + actualBarWidth / 2
            if (day <= daysInMonth) {
                drawContext.canvas.nativeCanvas.drawText(
                    day.toString(),
                    x - 10f,
                    chartBottom + 45f,
                    android.graphics.Paint().apply {
                        color = PurrytifyLightGray.hashCode()
                        textSize = 32f
                        isAntiAlias = true
                    }
                )
            }
        }
        
        // Draw axis lines
        drawLine(
            color = PurrytifyDarkGray,
            start = Offset(chartLeft, chartTop),
            end = Offset(chartLeft, chartBottom),
            strokeWidth = 2f
        )
        
        drawLine(
            color = PurrytifyDarkGray,
            start = Offset(chartLeft, chartBottom),
            end = Offset(chartRight, chartBottom),
            strokeWidth = 2f
        )
        
        // Draw axis labels
        drawContext.canvas.nativeCanvas.drawText(
            "Minutes",
            0f,
            chartTop - 60f,
            android.graphics.Paint().apply {
                color = PurrytifyLightGray.hashCode()
                textSize = 32f
                isAntiAlias = true
            }
        )
        
        drawContext.canvas.nativeCanvas.drawText(
            "Day",
            chartRight - 30f,
            chartBottom + 85f,
            android.graphics.Paint().apply {
                color = PurrytifyLightGray.hashCode()
                textSize = 32f
                isAntiAlias = true
            }
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
    }
}