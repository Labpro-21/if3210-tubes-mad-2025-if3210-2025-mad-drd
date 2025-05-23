package com.example.purrytify.util

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.purrytify.domain.model.AudioDeviceType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simplified and more reliable AudioOutputManager
 */
@Singleton
class AudioOutputManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val externalScope: CoroutineScope
) {
    private val TAG = "AudioOutputManager"
    
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    
    // Available audio devices
    private val _availableDevices = MutableStateFlow<List<com.example.purrytify.domain.model.AudioDeviceInfo>>(emptyList())
    val availableDevices: StateFlow<List<com.example.purrytify.domain.model.AudioDeviceInfo>> = _availableDevices.asStateFlow()
    
    // Currently active device
    private val _activeDevice = MutableStateFlow<com.example.purrytify.domain.model.AudioDeviceInfo?>(null)
    val activeDevice: StateFlow<com.example.purrytify.domain.model.AudioDeviceInfo?> = _activeDevice.asStateFlow()
    
    // Error messages
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // Device change receiver
    private val deviceChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED,
                BluetoothDevice.ACTION_ACL_DISCONNECTED,
                AudioManager.ACTION_AUDIO_BECOMING_NOISY,
                AudioManager.ACTION_HEADSET_PLUG -> {
                    Log.d(TAG, "Audio device state changed: ${intent.action}")
                    refreshAvailableDevices()
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
     * Refresh available audio devices
     */
    fun refreshAvailableDevices() {
        externalScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "=== Refreshing Available Devices ===")
                
                val devices = mutableListOf<com.example.purrytify.domain.model.AudioDeviceInfo>()
                
                // Always add built-in speaker
                val speakerDevice = createBuiltInSpeakerDevice()
                devices.add(speakerDevice)
                
                // Add other connected devices
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    addModernAudioDevices(devices)
                } else {
                    addLegacyAudioDevices(devices)
                }
                
                // Determine active device
                val activeDevice = determineActiveDevice(devices)
                
                // Update state
                _availableDevices.value = devices
                _activeDevice.value = activeDevice
                
                Log.d(TAG, "Found ${devices.size} devices, active: ${activeDevice?.name}")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing devices: ${e.message}", e)
                _errorMessage.value = "Failed to scan audio devices"
            }
        }
    }
    
    /**
     * Create built-in speaker device info
     */
    private fun createBuiltInSpeakerDevice(): com.example.purrytify.domain.model.AudioDeviceInfo {
        return com.example.purrytify.domain.model.AudioDeviceInfo(
            id = 0,
            name = "Phone Speaker",
            type = AudioDeviceType.BUILT_IN_SPEAKER,
            isConnected = true,
            isActive = false // Will be determined later
        )
    }
    
    /**
     * Add audio devices using modern API (API 23+)
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun addModernAudioDevices(devices: MutableList<com.example.purrytify.domain.model.AudioDeviceInfo>) {
        try {
            val audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            
            audioDevices.forEach { device ->
                when (device.type) {
                    AudioDeviceInfo.TYPE_WIRED_HEADSET, 
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> {
                        devices.add(createDeviceInfo(device, AudioDeviceType.WIRED_HEADSET))
                    }
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> {
                        devices.add(createDeviceInfo(device, AudioDeviceType.BLUETOOTH_SPEAKER))
                    }
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> {
                        devices.add(createDeviceInfo(device, AudioDeviceType.BLUETOOTH_HEADSET))
                    }
                    AudioDeviceInfo.TYPE_USB_DEVICE, 
                    AudioDeviceInfo.TYPE_USB_HEADSET -> {
                        devices.add(createDeviceInfo(device, AudioDeviceType.USB_DEVICE))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding modern audio devices: ${e.message}")
        }
    }
    
    /**
     * Add audio devices using legacy API
     */
    private fun addLegacyAudioDevices(devices: MutableList<com.example.purrytify.domain.model.AudioDeviceInfo>) {
        // Wired headset
        if (audioManager.isWiredHeadsetOn) {
            devices.add(
                com.example.purrytify.domain.model.AudioDeviceInfo(
                    id = 1,
                    name = "Wired Headset",
                    type = AudioDeviceType.WIRED_HEADSET,
                    isConnected = true,
                    isActive = false
                )
            )
        }
        
        // Bluetooth devices (simplified detection)
        if (audioManager.isBluetoothA2dpOn || audioManager.isBluetoothScoOn) {
            devices.add(
                com.example.purrytify.domain.model.AudioDeviceInfo(
                    id = 2,
                    name = "Bluetooth Audio",
                    type = AudioDeviceType.BLUETOOTH_SPEAKER,
                    isConnected = true,
                    isActive = false
                )
            )
        }
    }
    
    /**
     * Create device info from AudioDeviceInfo (API 23+)
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun createDeviceInfo(
        device: AudioDeviceInfo, 
        type: AudioDeviceType
    ): com.example.purrytify.domain.model.AudioDeviceInfo {
        val deviceName = device.productName?.toString() ?: type.getDisplayName()
        
        return com.example.purrytify.domain.model.AudioDeviceInfo(
            id = device.id,
            name = deviceName,
            type = type,
            isConnected = true,
            isActive = false // Will be determined later
        )
    }
    
    /**
     * Determine which device is currently active
     */
    private fun determineActiveDevice(devices: List<com.example.purrytify.domain.model.AudioDeviceInfo>): com.example.purrytify.domain.model.AudioDeviceInfo? {
        return when {
            // Check if Bluetooth SCO is active
            audioManager.isBluetoothScoOn -> {
                devices.find { it.type == AudioDeviceType.BLUETOOTH_HEADSET }
            }
            // Check if Bluetooth A2DP is active
            audioManager.isBluetoothA2dpOn -> {
                devices.find { it.type == AudioDeviceType.BLUETOOTH_SPEAKER }
            }
            // Check if wired headset is connected
            audioManager.isWiredHeadsetOn -> {
                devices.find { it.type == AudioDeviceType.WIRED_HEADSET }
            }
            // Default to speaker
            else -> {
                devices.find { it.type == AudioDeviceType.BUILT_IN_SPEAKER }
            }
        }?.copy(isActive = true)
    }
    
    /**
     * Switch to selected audio device
     */
    fun switchToDevice(deviceInfo: com.example.purrytify.domain.model.AudioDeviceInfo) {
        externalScope.launch(Dispatchers.Main) {
            try {
                Log.d(TAG, "Switching to device: ${deviceInfo.name} (${deviceInfo.type})")
                
                when (deviceInfo.type) {
                    AudioDeviceType.BUILT_IN_SPEAKER -> {
                        switchToSpeaker()
                    }
                    AudioDeviceType.WIRED_HEADSET -> {
                        switchToWiredHeadset()
                    }
                    AudioDeviceType.BLUETOOTH_SPEAKER -> {
                        switchToBluetoothA2dp()
                    }
                    AudioDeviceType.BLUETOOTH_HEADSET -> {
                        switchToBluetoothSco()
                    }
                    AudioDeviceType.USB_DEVICE -> {
                        switchToUsb()
                    }
                    AudioDeviceType.UNKNOWN -> {
                        _errorMessage.value = "Cannot switch to unknown device"
                        return@launch
                    }
                }
                
                // Wait a moment for the switch to take effect
                kotlinx.coroutines.delay(300)
                
                // Refresh devices to update active status
                refreshAvailableDevices()
                
                Log.d(TAG, "Successfully switched to ${deviceInfo.name}")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error switching to device: ${e.message}", e)
                _errorMessage.value = "Failed to switch to ${deviceInfo.name}"
            }
        }
    }
    
    /**
     * Switch to built-in speaker
     */
    private fun switchToSpeaker() {
        // Stop any Bluetooth audio
        audioManager.stopBluetoothSco()
        audioManager.isBluetoothScoOn = false
        
        // Enable speaker phone to force audio to speaker
        audioManager.isSpeakerphoneOn = true
        
        // Clear any preferred device (API 23+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                audioManager.clearCommunicationDevice()
            } catch (e: Exception) {
                Log.d(TAG, "clearCommunicationDevice not available: ${e.message}")
            }
        }
        
        Log.d(TAG, "Switched to built-in speaker")
    }
    
    /**
     * Switch to wired headset
     */
    private fun switchToWiredHeadset() {
        // Stop Bluetooth
        audioManager.stopBluetoothSco()
        audioManager.isBluetoothScoOn = false
        
        // Disable speaker phone to allow wired headset
        audioManager.isSpeakerphoneOn = false
        
        Log.d(TAG, "Switched to wired headset")
    }
    
    /**
     * Switch to Bluetooth A2DP
     */
    private fun switchToBluetoothA2dp() {
        // Disable speaker phone
        audioManager.isSpeakerphoneOn = false
        
        // Stop SCO if active
        audioManager.stopBluetoothSco()
        audioManager.isBluetoothScoOn = false
        
        // A2DP should be automatically used if available
        Log.d(TAG, "Switched to Bluetooth A2DP")
    }
    
    /**
     * Switch to Bluetooth SCO
     */
    private fun switchToBluetoothSco() {
        // Disable speaker phone
        audioManager.isSpeakerphoneOn = false
        
        // Enable Bluetooth SCO
        audioManager.isBluetoothScoOn = true
        audioManager.startBluetoothSco()
        
        Log.d(TAG, "Switched to Bluetooth SCO")
    }
    
    /**
     * Switch to USB device
     */
    private fun switchToUsb() {
        // Stop Bluetooth
        audioManager.stopBluetoothSco()
        audioManager.isBluetoothScoOn = false
        
        // Disable speaker phone
        audioManager.isSpeakerphoneOn = false
        
        Log.d(TAG, "Switched to USB device")
    }
    
    /**
     * Handle device disconnection
     */
    fun handleDeviceDisconnection(deviceInfo: com.example.purrytify.domain.model.AudioDeviceInfo) {
        externalScope.launch {
            try {
                Log.d(TAG, "Device disconnected: ${deviceInfo.name}")
                
                // Fallback to built-in speaker
                val builtInSpeaker = createBuiltInSpeakerDevice()
                switchToDevice(builtInSpeaker)
                
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
     * Cleanup method
     */
    fun cleanup() {
        try {
            stopDeviceMonitoring()
            Log.d(TAG, "AudioOutputManager cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup: ${e.message}", e)
        }
    }
}