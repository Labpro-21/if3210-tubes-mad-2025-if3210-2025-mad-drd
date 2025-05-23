package com.example.purrytify.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.purrytify.domain.model.AudioDeviceInfo
import com.example.purrytify.domain.model.AudioDeviceType
import com.example.purrytify.ui.theme.*

/**
 * Bottom sheet dialog for selecting audio output device
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
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = PurrytifyLighterBlack,
            contentColor = PurrytifyWhite,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            dragHandle = {
                // Custom drag handle to make it more obvious
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(PurrytifyLightGray, RoundedCornerShape(2.dp))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Audio Output",
                        style = Typography.titleLarge,
                        color = PurrytifyWhite,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Refresh button
                    IconButton(
                        onClick = onRefreshDevices,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh devices",
                            tint = PurrytifyGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Divider(
                    color = PurrytifyDarkGray,
                    thickness = 1.dp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Device list
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(availableDevices) { device ->
                        AudioDeviceItem(
                            device = device,
                            isActive = device.id == activeDevice?.id,
                            onClick = { 
                                if (device.isConnected) {
                                    onDeviceSelected(device)
                                }
                            }
                        )
                    }
                    
                    // Bottom spacing
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
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
            .clickable(
                enabled = device.isConnected,
            ) {
                onClick() 
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) PurrytifyGreen.copy(alpha = 0.2f) else PurrytifyBlack,
            contentColor = PurrytifyWhite
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isActive) 4.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Device icon and info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = getDeviceIcon(device.type),
                    contentDescription = null,
                    tint = if (device.isConnected) {
                        if (isActive) PurrytifyGreen else PurrytifyWhite
                    } else {
                        PurrytifyLightGray
                    },
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = device.name,
                        style = Typography.bodyLarge,
                        color = if (device.isConnected) {
                            if (isActive) PurrytifyGreen else PurrytifyWhite
                        } else {
                            PurrytifyLightGray
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                    )
                    
                    Text(
                        text = if (device.isConnected) {
                            if (isActive) "Active" else "Connected"
                        } else {
                            "Not connected"
                        },
                        style = Typography.bodySmall,
                        color = if (device.isConnected) {
                            if (isActive) PurrytifyGreen else PurrytifyLightGray
                        } else {
                            PurrytifyLightGray.copy(alpha = 0.7f)
                        }
                    )
                }
            }
            
            // Active indicator
            if (isActive) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Active device",
                    tint = PurrytifyGreen,
                    modifier = Modifier.size(20.dp)
                )
            } else if (!device.isConnected) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Disconnected",
                    tint = PurrytifyLightGray.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Get icon for device type
 */
private fun getDeviceIcon(type: AudioDeviceType): ImageVector {
    return when (type) {
        AudioDeviceType.BUILT_IN_SPEAKER -> Icons.Default.Speaker
        AudioDeviceType.WIRED_HEADSET -> Icons.Default.Headset
        AudioDeviceType.BLUETOOTH_SPEAKER -> Icons.Default.SpeakerGroup
        AudioDeviceType.BLUETOOTH_HEADSET -> Icons.Default.BluetoothAudio
        AudioDeviceType.USB_DEVICE -> Icons.Default.Usb
        AudioDeviceType.UNKNOWN -> Icons.Default.AudioFile
    }
}