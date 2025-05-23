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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.purrytify.domain.model.AudioDeviceInfo
import com.example.purrytify.domain.model.AudioDeviceType
import com.example.purrytify.ui.theme.*

/**
 * Improved audio output device selection dialog
 */
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
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = PurrytifyLighterBlack
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Audio Output",
                            style = Typography.titleLarge,
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
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Current active device info
                    if (activeDevice != null) {
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
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // Available devices list
                    Text(
                        text = "Available Devices",
                        style = Typography.bodyLarge,
                        color = PurrytifyWhite,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (availableDevices.isEmpty()) {
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
                    } else {
                        // Device list
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 300.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(availableDevices) { device ->
                                AudioDeviceItem(
                                    device = device,
                                    isActive = device.id == activeDevice?.id,
                                    onClick = { onDeviceSelected(device) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual audio device item
 */
@Composable
private fun AudioDeviceItem(
    device: AudioDeviceInfo,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Device icon
            Box(
                modifier = Modifier
                    .size(40.dp)
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
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Device info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = Typography.bodyLarge,
                    color = if (isActive) PurrytifyGreen else PurrytifyWhite,
                    fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
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

/**
 * Get appropriate icon for device type
 */
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