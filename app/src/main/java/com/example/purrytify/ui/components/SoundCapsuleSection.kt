package com.example.purrytify.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.purrytify.domain.model.MonthlyAnalytics
import com.example.purrytify.ui.theme.*
import java.time.format.TextStyle
import java.util.*

/**
 * Sound Capsule section for the Profile screen
 * Shows current month analytics overview
 */
@Composable
fun SoundCapsuleSection(
    currentMonthAnalytics: MonthlyAnalytics?,
    isLoading: Boolean = false,
    onTimeListenedClick: () -> Unit,
    onTopArtistClick: () -> Unit,
    onTopSongClick: () -> Unit,
    onExportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = PurrytifyLighterBlack
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)

        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = "Sound Capsule",
                        tint = PurrytifyGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = "Your Sound Capsule",
                        style = Typography.titleLarge,
                        color = PurrytifyWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Export button
                IconButton(
                    onClick = onExportClick,
                    enabled = currentMonthAnalytics?.hasData == true
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Export",
                        tint = if (currentMonthAnalytics?.hasData == true) PurrytifyGreen else PurrytifyDarkGray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Current month display
            Text(
                text = currentMonthAnalytics?.let { "${it.monthName} ${it.year}" } ?: "Current Month",
                style = Typography.bodyMedium,
                color = PurrytifyLightGray
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isLoading) {
                // Loading state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = PurrytifyGreen,
                        modifier = Modifier.size(32.dp)
                    )
                }
            } else if (currentMonthAnalytics?.hasData == true) {
                // Analytics data
                Column {
                    // Time listened
                    AnalyticsItem(
                        title = "Time listened",
                        value = currentMonthAnalytics.formattedListeningTime,
                        onClick = onTimeListenedClick
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Top artist
                    AnalyticsItem(
                        title = "Top artist",
                        value = currentMonthAnalytics.topArtist?.name ?: "N/A",
                        onClick = onTopArtistClick
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Top song
                    AnalyticsItem(
                        title = "Top song",
                        value = currentMonthAnalytics.topSong?.title ?: "N/A",
                        onClick = onTopSongClick
                    )
                    
                    // Day streak (if available)
                    currentMonthAnalytics.dayStreak?.let { streak ->
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = PurrytifyDarkGray.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "You had a ${streak.consecutiveDays}-day streak",
                                    style = Typography.bodyMedium,
                                    color = PurrytifyWhite,
                                    fontWeight = FontWeight.Medium
                                )
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Text(
                                    text = "${streak.songTitle} by ${streak.artist}",
                                    style = Typography.bodySmall,
                                    color = PurrytifyLightGray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            } else {
                // No data state
                Column(
                    modifier = Modifier.fillMaxWidth().padding(
                        top = 24.dp,
                        bottom = 40.dp
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "No data",
                        tint = PurrytifyDarkGray,
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "No data available",
                        style = Typography.bodyLarge,
                        color = PurrytifyLightGray
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "Start listening to music to see your analytics",
                        style = Typography.bodySmall,
                        color = PurrytifyDarkGray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Individual analytics item component
 */
@Composable
private fun AnalyticsItem(
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = Typography.bodySmall,
                color = PurrytifyLightGray
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = value,
                style = Typography.bodyLarge,
                color = PurrytifyGreen,
                fontWeight = FontWeight.Medium
            )
        }
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "View details",
            tint = PurrytifyLightGray,
            modifier = Modifier.size(20.dp)
        )
    }
}