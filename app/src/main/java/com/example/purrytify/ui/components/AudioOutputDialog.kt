package com.example.purrytify.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.purrytify.domain.model.AudioDeviceInfo
import com.example.purrytify.domain.model.AudioDeviceType
import com.example.purrytify.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioOutputDialog(
    isVisible: Boolean,
    availableDevices: List<AudioDeviceInfo>,
    activeDevice: AudioDeviceInfo?,
    onDismiss: () -> Unit,
    onDeviceSelected: (AudioDeviceInfo) -> Unit,
    onRefreshDevices: () -> Unit
) {
    if (isVisible) {
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val screenHeight = configuration.screenHeightDp.dp
        val screenWidth = configuration.screenWidthDp.dp
        
        // Calculate dialog dimensions based on orientation
        val dialogWidth = when {
            isLandscape -> minOf(600.dp, screenWidth * 0.8f)
            else -> minOf(400.dp, screenWidth * 0.9f)
        }
        
        val maxDialogHeight = when {
            isLandscape -> screenHeight * 0.85f
            else -> screenHeight * 0.75f
        }
        
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            Card(
                modifier = Modifier
                    .width(dialogWidth)
                    .heightIn(max = maxDialogHeight)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = PurrytifyLighterBlack
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(
                        if (isLandscape) 12.dp else 16.dp
                    )
                ) {
                    // Header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Audio Output",
                                style = if (isLandscape) Typography.titleMedium else Typography.titleLarge,
                                color = PurrytifyWhite,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Row {
                                // Refresh button
                                IconButton(
                                    onClick = onRefreshDevices,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh devices",
                                        tint = PurrytifyLightGray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                
                                // Close button
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = PurrytifyLightGray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Current active device info
                    if (activeDevice != null) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = PurrytifyGreen.copy(alpha = 0.1f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = getDeviceIcon(activeDevice.type),
                                        contentDescription = null,
                                        tint = PurrytifyGreen,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Currently Playing",
                                            style = Typography.bodySmall,
                                            color = PurrytifyLightGray
                                        )
                                        Text(
                                            text = activeDevice.name,
                                            style = Typography.bodyLarge,
                                            color = PurrytifyGreen,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = "Active",
                                        tint = PurrytifyGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Available devices header
                    item {
                        Text(
                            text = "Available Devices",
                            style = Typography.bodyLarge,
                            color = PurrytifyWhite,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    // Device list or empty state
                    if (availableDevices.isEmpty()) {
                        item {
                            // Empty state
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Speaker,
                                        contentDescription = null,
                                        tint = PurrytifyLightGray,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Text(
                                        text = "No devices found",
                                        style = Typography.bodyMedium,
                                        color = PurrytifyLightGray
                                    )
                                }
                            }
                        }
                    } else {
                        // Device list
                        items(
                            items = availableDevices,
                            key = { device -> device.id }
                        ) { device ->
                            AudioDeviceItem(
                                device = device,
                                isActive = device.id == activeDevice?.id,
                                onClick = { onDeviceSelected(device) },
                                isLandscape = isLandscape
                            )
                        }
                    }
                    
                    // Bottom spacing for better scrolling
                    item {
                        Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioDeviceItem(
    device: AudioDeviceInfo,
    isActive: Boolean,
    onClick: () -> Unit,
    isLandscape: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { if (!isActive) onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) PurrytifyDarkGray else PurrytifyBlack
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = if (isLandscape) 12.dp else 16.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Device icon
            val iconSize = if (isLandscape) 36.dp else 40.dp
            val iconInnerSize = if (isLandscape) 18.dp else 20.dp
            
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .clip(CircleShape)
                    .background(
                        if (isActive) PurrytifyGreen.copy(alpha = 0.2f) 
                        else PurrytifyDarkGray
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getDeviceIcon(device.type),
                    contentDescription = null,
                    tint = if (isActive) PurrytifyGreen else PurrytifyLightGray,
                    modifier = Modifier.size(iconInnerSize)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Device info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = if (isLandscape) Typography.bodyMedium else Typography.bodyLarge,
                    color = if (isActive) PurrytifyGreen else PurrytifyWhite,
                    fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = device.type.getDisplayName(),
                    style = Typography.bodySmall,
                    color = PurrytifyLightGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Status indicator
            if (isActive) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(PurrytifyGreen)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Select",
                    tint = PurrytifyLightGray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun getDeviceIcon(type: AudioDeviceType): ImageVector {
    return when (type) {
        AudioDeviceType.BUILT_IN_SPEAKER -> Icons.Default.Speaker
        AudioDeviceType.WIRED_HEADSET -> Icons.Default.Headphones
        AudioDeviceType.BLUETOOTH_SPEAKER -> Icons.Default.Bluetooth
        AudioDeviceType.BLUETOOTH_HEADSET -> Icons.Default.BluetoothAudio
        AudioDeviceType.USB_DEVICE -> Icons.Default.Usb
        AudioDeviceType.UNKNOWN -> Icons.Default.DeviceUnknown
    }
}