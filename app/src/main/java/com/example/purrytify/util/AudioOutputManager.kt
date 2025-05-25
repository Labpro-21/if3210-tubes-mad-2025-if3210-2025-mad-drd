package com.example.purrytify.util

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo as SystemAudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.purrytify.data.repository.PlayerRepository
import com.example.purrytify.domain.model.AudioDeviceType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import com.example.purrytify.domain.model.AudioDeviceInfo as LocalAudioDeviceInfo

/**
 * AudioOutputManager that uses ExoPlayer's setPreferredAudioDevice for proper audio routing
 */
@Singleton
class AudioOutputManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val externalScope: CoroutineScope,
    private val playerRepository: PlayerRepository
) {
    private val TAG = "AudioOutputManager"
    
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    // Available audio devices
    private val _availableDevices = MutableStateFlow<List<LocalAudioDeviceInfo>>(emptyList())
    val availableDevices: StateFlow<List<LocalAudioDeviceInfo>> = _availableDevices.asStateFlow()
    
    // Currently active device
    private val _activeDevice = MutableStateFlow<LocalAudioDeviceInfo?>(null)
    val activeDevice: StateFlow<LocalAudioDeviceInfo?> = _activeDevice.asStateFlow()
    
    // Error messages
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // Track the current preferred device set in ExoPlayer
    private var currentPreferredDevice: LocalAudioDeviceInfo? = null
    
    // Device change receiver
    private val deviceChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED,
                BluetoothDevice.ACTION_ACL_DISCONNECTED,
                AudioManager.ACTION_AUDIO_BECOMING_NOISY,
                AudioManager.ACTION_HEADSET_PLUG -> {
                    Log.d(TAG, "Audio device state changed: ${intent.action}")
                    externalScope.launch {
                        delay(500) // Give the system time to update
                        refreshAvailableDevices()
                    }
                }
            }
        }
    }
    
    init {
        startDeviceMonitoring()
        refreshAvailableDevices()
    }
    
    /**
     * Start monitoring for device changes
     */
    private fun startDeviceMonitoring() {
        try {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
                addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                addAction(AudioManager.ACTION_HEADSET_PLUG)
            }
            context.registerReceiver(deviceChangeReceiver, filter)
            Log.d(TAG, "Started device monitoring")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting device monitoring: ${e.message}", e)
        }
    }
    
    /**
     * Stop monitoring for device changes
     */
    fun stopDeviceMonitoring() {
        try {
            context.unregisterReceiver(deviceChangeReceiver)
            Log.d(TAG, "Stopped device monitoring")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping device monitoring: ${e.message}", e)
        }
    }
    
    /**
     * Refresh available audio devices using the system AudioManager
     */
    fun refreshAvailableDevices() {
        externalScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "=== Refreshing Available Devices ===")
                
                val allDevices = mutableListOf<LocalAudioDeviceInfo>()
                
                // Add actual audio devices from the system first
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    addSystemAudioDevices(allDevices)
                }
                
                // Check if we found a built-in speaker from system devices
                val hasBuiltInSpeaker = allDevices.any { it.type == AudioDeviceType.BUILT_IN_SPEAKER }
                
                // Only add manual built-in speaker if system didn't provide one
                if (!hasBuiltInSpeaker) {
                    val speakerDevice = createBuiltInSpeakerDevice()
                    allDevices.add(speakerDevice)
                    Log.d(TAG, "Added manual built-in speaker (system didn't provide one)")
                }
                
                // Deduplicate devices (remove duplicates based on name and type)
                val deduplicatedDevices = deduplicateDevices(allDevices)
                
                // Determine which device should be marked as active
                val activeDevice = determineActiveDevice(deduplicatedDevices)
                
                // Update state
                withContext(Dispatchers.Main) {
                    _availableDevices.value = deduplicatedDevices
                    _activeDevice.value = activeDevice
                    
                    Log.d(TAG, "Found ${deduplicatedDevices.size} devices (after deduplication), active: ${activeDevice?.name}")
                    deduplicatedDevices.forEach { device ->
                        Log.d(TAG, "Device: ${device.name} (${device.type}) - Active: ${device.isActive}")
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing devices: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Failed to scan audio devices"
                }
            }
        }
    }
    
    /**
     * Deduplicate devices by removing duplicates based on name and prioritizing certain types
     */
    private fun deduplicateDevices(devices: List<LocalAudioDeviceInfo>): List<LocalAudioDeviceInfo> {
        val deviceGroups = devices.groupBy { device ->
            // Group by name, but normalize it first
            normalizeDeviceName(device.name)
        }
        
        val deduplicatedDevices = mutableListOf<LocalAudioDeviceInfo>()
        
        deviceGroups.forEach { (normalizedName, deviceGroup) ->
            if (deviceGroup.size == 1) {
                // No duplicates, add as-is
                deduplicatedDevices.add(deviceGroup.first())
            } else {
                // Multiple devices with same name - choose the best one
                val bestDevice = chooseBestDevice(deviceGroup)
                deduplicatedDevices.add(bestDevice)
                
                Log.d(TAG, "Deduplicated '$normalizedName': chose ${bestDevice.type} over ${deviceGroup.filter { it != bestDevice }.map { it.type }}")
            }
        }
        
        return deduplicatedDevices.sortedBy { device ->
            // Sort by priority: Speaker, Bluetooth, Wired, USB, Unknown
            when (device.type) {
                AudioDeviceType.BUILT_IN_SPEAKER -> 0
                AudioDeviceType.BLUETOOTH_SPEAKER -> 1
                AudioDeviceType.BLUETOOTH_HEADSET -> 2
                AudioDeviceType.WIRED_HEADSET -> 3
                AudioDeviceType.USB_DEVICE -> 4
                AudioDeviceType.UNKNOWN -> 5
            }
        }
    }
    
    /**
     * Normalize device name for comparison (remove model numbers, etc.)
     */
    private fun normalizeDeviceName(name: String): String {
        return when {
            // Normalize built-in speaker names
            name.contains("Speaker", ignoreCase = true) || 
            name.contains("SM-", ignoreCase = true) ||
            name.equals("Phone Speaker", ignoreCase = true) -> "Phone Speaker"
            
            // Keep other device names as-is, but clean them up
            else -> name.trim()
        }
    }
    
    /**
     * Choose the best device from a group of duplicates
     */
    private fun chooseBestDevice(devices: List<LocalAudioDeviceInfo>): LocalAudioDeviceInfo {
        // Priority order for device types (higher number = higher priority)
        val typePriority = mapOf(
            AudioDeviceType.BUILT_IN_SPEAKER to 5,
            AudioDeviceType.BLUETOOTH_SPEAKER to 4,  // Prefer A2DP over SCO for music
            AudioDeviceType.BLUETOOTH_HEADSET to 3,
            AudioDeviceType.WIRED_HEADSET to 2,
            AudioDeviceType.USB_DEVICE to 1,
            AudioDeviceType.UNKNOWN to 0
        )
        
        // For built-in speaker, prefer the one with systemAudioDeviceInfo (real system device)
        if (devices.all { it.type == AudioDeviceType.BUILT_IN_SPEAKER }) {
            val systemDevice = devices.find { it.systemAudioDeviceInfo != null }
            if (systemDevice != null) {
                return systemDevice.copy(name = "Phone Speaker") // Normalize the name
            }
        }
        
        // For other devices, choose based on type priority
        return devices.maxByOrNull { device ->
            typePriority[device.type] ?: 0
        } ?: devices.first()
    }
    
    /**
     * Create built-in speaker device info
     */
    private fun createBuiltInSpeakerDevice(): LocalAudioDeviceInfo {
        return LocalAudioDeviceInfo(
            id = 0,
            name = "Phone Speaker",
            type = AudioDeviceType.BUILT_IN_SPEAKER,
            isConnected = true,
            isActive = false, // Will be determined later
            systemAudioDeviceInfo = null // null means default system routing for ExoPlayer
        )
    }
    
    /**
     * Add audio devices from system AudioManager (API 23+)
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun addSystemAudioDevices(devices: MutableList<LocalAudioDeviceInfo>) {
        try {
            val audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            
            audioDevices.forEach { device ->
                val deviceType = mapSystemDeviceType(device.type)
                if (deviceType != AudioDeviceType.UNKNOWN) {
                    val deviceInfo = createDeviceInfo(device, deviceType)
                    devices.add(deviceInfo)
                    Log.d(TAG, "Added system device: ${deviceInfo.name} (${deviceInfo.type})")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding system audio devices: ${e.message}")
        }
    }
    
    /**
     * Map system AudioDeviceInfo type to our AudioDeviceType
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun mapSystemDeviceType(systemType: Int): AudioDeviceType {
        return when (systemType) {
            SystemAudioDeviceInfo.TYPE_WIRED_HEADSET, 
            SystemAudioDeviceInfo.TYPE_WIRED_HEADPHONES -> AudioDeviceType.WIRED_HEADSET
            SystemAudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> AudioDeviceType.BLUETOOTH_SPEAKER
            SystemAudioDeviceInfo.TYPE_BLUETOOTH_SCO -> AudioDeviceType.BLUETOOTH_HEADSET
            SystemAudioDeviceInfo.TYPE_USB_DEVICE, 
            SystemAudioDeviceInfo.TYPE_USB_HEADSET -> AudioDeviceType.USB_DEVICE
            SystemAudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> AudioDeviceType.BUILT_IN_SPEAKER
            else -> AudioDeviceType.UNKNOWN
        }
    }
    
    /**
     * Create device info from system AudioDeviceInfo (API 23+)
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun createDeviceInfo(
        systemDevice: SystemAudioDeviceInfo, 
        type: AudioDeviceType
    ): LocalAudioDeviceInfo {
        val deviceName = systemDevice.productName?.toString() ?: type.getDisplayName()
        
        return LocalAudioDeviceInfo(
            id = systemDevice.id,
            name = deviceName,
            type = type,
            isConnected = true,
            isActive = false, // Will be determined later
            systemAudioDeviceInfo = systemDevice // Store reference to actual system device
        )
    }
    
    /**
     * Determine which device should be marked as active
     * This is based on which device we've set as preferred, or system defaults
     */
    private fun determineActiveDevice(devices: List<LocalAudioDeviceInfo>): LocalAudioDeviceInfo? {
        // If we have a preferred device set, mark it as active
        currentPreferredDevice?.let { preferred ->
            val matchingDevice = devices.find { it.id == preferred.id && it.type == preferred.type }
            if (matchingDevice != null) {
                Log.d(TAG, "Active device based on current preference: ${matchingDevice.name}")
                return matchingDevice.copy(isActive = true)
            }
        }
        
        // Otherwise, determine based on system priorities
        val activeDevice = when {
            // Check for Bluetooth A2DP (most common for music)
            devices.any { it.type == AudioDeviceType.BLUETOOTH_SPEAKER } -> {
                devices.find { it.type == AudioDeviceType.BLUETOOTH_SPEAKER }
            }
            // Check for wired headset
            devices.any { it.type == AudioDeviceType.WIRED_HEADSET } -> {
                devices.find { it.type == AudioDeviceType.WIRED_HEADSET }
            }
            // Check for Bluetooth headset
            devices.any { it.type == AudioDeviceType.BLUETOOTH_HEADSET } -> {
                devices.find { it.type == AudioDeviceType.BLUETOOTH_HEADSET }
            }
            // Check for USB device
            devices.any { it.type == AudioDeviceType.USB_DEVICE } -> {
                devices.find { it.type == AudioDeviceType.USB_DEVICE }
            }
            // Default to built-in speaker
            else -> devices.find { it.type == AudioDeviceType.BUILT_IN_SPEAKER }
        }
        
        Log.d(TAG, "Active device based on system priority: ${activeDevice?.name}")
        return activeDevice?.copy(isActive = true)
    }
    
    /**
     * Switch to selected audio device using ExoPlayer's setPreferredAudioDevice
     */
    fun switchToDevice(deviceInfo: LocalAudioDeviceInfo) {
        externalScope.launch(Dispatchers.Main) {
            try {
                Log.d(TAG, "=== Switching to device: ${deviceInfo.name} (${deviceInfo.type}) ===")
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Use ExoPlayer's setPreferredAudioDevice with the actual system AudioDeviceInfo
                    playerRepository.setPreferredAudioDevice(deviceInfo.systemAudioDeviceInfo)
                    
                    // Store the current preference
                    currentPreferredDevice = deviceInfo
                    
                    // Wait for the switch to take effect
                    delay(500)
                    
                    // Update the active device in our state
                    val updatedDevices = _availableDevices.value.map { device ->
                        device.copy(isActive = device.id == deviceInfo.id && device.type == deviceInfo.type)
                    }
                    _availableDevices.value = updatedDevices
                    _activeDevice.value = deviceInfo.copy(isActive = true)
                    
                    Log.d(TAG, "✓ Successfully switched to ${deviceInfo.name}")
                    
                } else {
                    Log.w(TAG, "Audio device switching requires API 23+")
                    _errorMessage.value = "Audio device switching not supported on this Android version"
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error switching to device: ${e.message}", e)
                _errorMessage.value = "Failed to switch to ${deviceInfo.name}"
            }
        }
    }
    
    /**
     * Handle device disconnection by switching to a fallback device
     */
    fun handleDeviceDisconnection(deviceInfo: LocalAudioDeviceInfo) {
        externalScope.launch {
            try {
                Log.d(TAG, "Device disconnected: ${deviceInfo.name}")
                
                // Clear current preference if it was the disconnected device
                if (currentPreferredDevice?.id == deviceInfo.id && currentPreferredDevice?.type == deviceInfo.type) {
                    currentPreferredDevice = null
                }
                
                // Refresh devices and let the system determine the new active device
                refreshAvailableDevices()
                
                // Optionally switch to built-in speaker as fallback
                val builtInSpeaker = _availableDevices.value.find { it.type == AudioDeviceType.BUILT_IN_SPEAKER }
                if (builtInSpeaker != null) {
                    switchToDevice(builtInSpeaker)
                }
                
                _errorMessage.value = "${deviceInfo.name} disconnected. Switched to phone speaker."
                
            } catch (e: Exception) {
                Log.e(TAG, "Error handling device disconnection: ${e.message}", e)
            }
        }
    }
    
    /**
     * Clear error messages
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * Get friendly name for current audio routing
     */
    fun getCurrentAudioRouteName(): String {
        return _activeDevice.value?.name ?: "Phone Speaker"
    }
    
    /**
     * Check if external device is active
     */
    fun isExternalDeviceActive(): Boolean {
        return _activeDevice.value?.type != AudioDeviceType.BUILT_IN_SPEAKER
    }
    
    /**
     * Get current volume for media stream
     */
    fun getCurrentVolume(): Int {
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    }
    
    /**
     * Get max volume for media stream
     */
    fun getMaxVolume(): Int {
        return audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    }
    
    /**
     * Set volume for media stream
     */
    fun setVolume(volume: Int) {
        val maxVolume = getMaxVolume()
        val clampedVolume = volume.coerceIn(0, maxVolume)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, clampedVolume, 0)
    }
    
    /**
     * Check if media stream is muted
     */
    fun isMuted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioManager.isStreamMute(AudioManager.STREAM_MUSIC)
        } else {
            getCurrentVolume() == 0
        }
    }
    
    /**
     * Cleanup method
     */
    fun cleanup() {
        try {
            stopDeviceMonitoring()
            
            // Reset ExoPlayer to default audio routing
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                playerRepository.setPreferredAudioDevice(null)
            }
            
            currentPreferredDevice = null
            Log.d(TAG, "AudioOutputManager cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup: ${e.message}", e)
        }
    }
}