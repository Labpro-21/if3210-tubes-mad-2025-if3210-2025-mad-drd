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
import android.media.AudioDeviceCallback
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
 * Manages audio output device detection and switching
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
    
    // Bluetooth receiver for connection events
    private val bluetoothReceiver = object : BroadcastReceiver() {
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
    
    // Audio device callback for API 23+
    @RequiresApi(Build.VERSION_CODES.M)
    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            Log.d(TAG, "Audio devices added: ${addedDevices?.size}")
            refreshAvailableDevices()
        }
        
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            Log.d(TAG, "Audio devices removed: ${removedDevices?.size}")
            refreshAvailableDevices()
        }
    }
    
    init {
        startListening()
        refreshAvailableDevices()
    }
    
    /**
     * Start listening for audio device changes
     */
    private fun startListening() {
        try {
            // Register broadcast receiver for Bluetooth and headset events
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
                addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                addAction(AudioManager.ACTION_HEADSET_PLUG)
            }
            context.registerReceiver(bluetoothReceiver, filter)
            
            // Register audio device callback for API 23+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)
            }
            
            Log.d(TAG, "Started listening for audio device changes")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting audio device listener: ${e.message}", e)
        }
    }
    
    /**
     * Stop listening for audio device changes
     */
    fun stopListening() {
        try {
            context.unregisterReceiver(bluetoothReceiver)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
            }
            
            Log.d(TAG, "Stopped listening for audio device changes")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio device listener: ${e.message}", e)
        }
    }
    
    /**
     * Refresh the list of available audio devices
     */
    fun refreshAvailableDevices() {
        externalScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "=== Refreshing Available Devices ===")
                Log.d(TAG, "Current audio state:")
                Log.d(TAG, "  - SpeakerphoneOn: ${audioManager.isSpeakerphoneOn}")
                Log.d(TAG, "  - WiredHeadsetOn: ${audioManager.isWiredHeadsetOn}")
                Log.d(TAG, "  - BluetoothA2dpOn: ${audioManager.isBluetoothA2dpOn}")
                Log.d(TAG, "  - BluetoothScoOn: ${audioManager.isBluetoothScoOn}")
                Log.d(TAG, "  - Audio Mode: ${audioManager.mode}")
                
                val devices = mutableListOf<com.example.purrytify.domain.model.AudioDeviceInfo>()
                val deviceIds = mutableSetOf<String>() // To prevent duplicates
                
                // Add built-in speaker (always available)
                val builtInId = "builtin_speaker"
                val builtInActive = isUsingBuiltInSpeaker()
                devices.add(
                    com.example.purrytify.domain.model.AudioDeviceInfo(
                        id = 0,
                        name = "Phone Speaker",
                        type = AudioDeviceType.BUILT_IN_SPEAKER,
                        isConnected = true,
                        isActive = builtInActive
                    )
                )
                deviceIds.add(builtInId)
                Log.d(TAG, "Added built-in speaker (active: $builtInActive)")
                
                // Get devices from AudioManager (API 23+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                    Log.d(TAG, "Found ${audioDevices.size} audio devices from AudioManager")
                    
                    audioDevices.forEach { device ->
                        val deviceInfo = mapAudioDeviceInfo(device)
                        if (deviceInfo != null) {
                            val deviceKey = "${deviceInfo.name}_${deviceInfo.type}_${device.id}"
                            if (!deviceIds.contains(deviceKey)) {
                                devices.add(deviceInfo)
                                deviceIds.add(deviceKey)
                                Log.d(TAG, "Added device: ${deviceInfo.name} (${deviceInfo.type}, active: ${deviceInfo.isActive})")
                            } else {
                                Log.d(TAG, "Skipped duplicate device: ${deviceInfo.name}")
                            }
                        }
                    }
                } else {
                    // For older APIs, check basic audio routing
                    if (audioManager.isWiredHeadsetOn) {
                        val wiredKey = "wired_headset"
                        if (!deviceIds.contains(wiredKey)) {
                            devices.add(
                                com.example.purrytify.domain.model.AudioDeviceInfo(
                                    id = 1,
                                    name = "Wired Headset",
                                    type = AudioDeviceType.WIRED_HEADSET,
                                    isConnected = true,
                                    isActive = !audioManager.isSpeakerphoneOn
                                )
                            )
                            deviceIds.add(wiredKey)
                            Log.d(TAG, "Added wired headset (legacy API)")
                        }
                    }
                    
                    if (audioManager.isBluetoothA2dpOn) {
                        val bluetoothKey = "bluetooth_audio"
                        if (!deviceIds.contains(bluetoothKey)) {
                            devices.add(
                                com.example.purrytify.domain.model.AudioDeviceInfo(
                                    id = 2,
                                    name = "Bluetooth Audio",
                                    type = AudioDeviceType.BLUETOOTH_SPEAKER,
                                    isConnected = true,
                                    isActive = true
                                )
                            )
                            deviceIds.add(bluetoothKey)
                            Log.d(TAG, "Added bluetooth audio (legacy API)")
                        }
                    }
                }
                
                // Add Bluetooth devices (with deduplication)
                addBluetoothDevices(devices, deviceIds)
                
                // Update state
                _availableDevices.value = devices
                
                // Update active device
                val currentActive = devices.find { it.isActive }
                _activeDevice.value = currentActive
                
                Log.d(TAG, "=== Device Refresh Complete ===")
                Log.d(TAG, "Total devices: ${devices.size}")
                Log.d(TAG, "Active device: ${currentActive?.name ?: "None"}")
                devices.forEach { device ->
                    Log.d(TAG, "  - ${device.name} (${device.type}): connected=${device.isConnected}, active=${device.isActive}")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing devices: ${e.message}", e)
                _errorMessage.value = "Failed to detect audio devices"
            }
        }
    }
    
    /**
     * Add Bluetooth audio devices to the list
     */
    @SuppressLint("MissingPermission")
    private fun addBluetoothDevices(
        devices: MutableList<com.example.purrytify.domain.model.AudioDeviceInfo>,
        deviceIds: MutableSet<String>
    ) {
        try {
            val bluetoothAdapter = bluetoothManager?.adapter
            if (bluetoothAdapter?.isEnabled == true) {
                val bondedDevices = bluetoothAdapter.bondedDevices
                
                bondedDevices?.forEach { device ->
                    // Check if device supports audio
                    if (isAudioDevice(device)) {
                        val isConnected = isBluetoothDeviceConnected(device)
                        val deviceName = device.name ?: "Unknown Bluetooth Device"
                        
                        if (isConnected) {
                            // For connected devices, create two entries: A2DP and SCO
                            val baseDeviceKey = "${deviceName}_${device.address}"
                            
                            // A2DP entry (for music/media)
                            val a2dpKey = "${baseDeviceKey}_A2DP"
                            if (!deviceIds.contains(a2dpKey)) {
                                devices.add(
                                    com.example.purrytify.domain.model.AudioDeviceInfo(
                                        id = device.address.hashCode(),
                                        name = deviceName,
                                        type = AudioDeviceType.BLUETOOTH_SPEAKER,
                                        isConnected = true,
                                        isActive = audioManager.isBluetoothA2dpOn && !audioManager.isBluetoothScoOn
                                    )
                                )
                                deviceIds.add(a2dpKey)
                            }
                            
                            // SCO entry (for hands-free/calls) - only if device supports it
                            if (device.bluetoothClass?.hasService(android.bluetooth.BluetoothClass.Service.TELEPHONY) == true ||
                                device.bluetoothClass?.hasService(android.bluetooth.BluetoothClass.Service.AUDIO) == true) {
                                
                                val scoKey = "${baseDeviceKey}_SCO"
                                if (!deviceIds.contains(scoKey)) {
                                    devices.add(
                                        com.example.purrytify.domain.model.AudioDeviceInfo(
                                            id = device.address.hashCode() + 1000, // Different ID for SCO
                                            name = "$deviceName (Hands-free)",
                                            type = AudioDeviceType.BLUETOOTH_HEADSET,
                                            isConnected = true,
                                            isActive = audioManager.isBluetoothScoOn
                                        )
                                    )
                                    deviceIds.add(scoKey)
                                }
                            }
                        } else {
                            // For disconnected devices, just show one entry
                            val baseDeviceKey = "DEVICE"
                            val disconnectedKey = "${baseDeviceKey}_DISCONNECTED"
                            if (!deviceIds.contains(disconnectedKey)) {
                                devices.add(
                                    com.example.purrytify.domain.model.AudioDeviceInfo(
                                        id = device.address.hashCode(),
                                        name = deviceName,
                                        type = AudioDeviceType.BLUETOOTH_SPEAKER,
                                        isConnected = false,
                                        isActive = false
                                    )
                                )
                                deviceIds.add(disconnectedKey)
                            }
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Bluetooth permission not granted, skipping Bluetooth devices")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding Bluetooth devices: ${e.message}", e)
        }
    }
    
    /**
     * Check if Bluetooth device supports audio
     */
    private fun isAudioDevice(device: BluetoothDevice): Boolean {
        return try {
            val audioClass = 1024 // Audio/Video device class
            val majorClass = device.bluetoothClass?.majorDeviceClass
            majorClass == audioClass
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if Bluetooth device is connected
     */
    @SuppressLint("MissingPermission")
    private fun isBluetoothDeviceConnected(device: BluetoothDevice): Boolean {
        return try {
            bluetoothManager?.let { manager ->
                val profileProxy = manager.getConnectionState(device, BluetoothProfile.A2DP)
                profileProxy == BluetoothProfile.STATE_CONNECTED
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Map Android AudioDeviceInfo to our domain model
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun mapAudioDeviceInfo(device: AudioDeviceInfo): com.example.purrytify.domain.model.AudioDeviceInfo? {
        val type = when (device.type) {
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> return null // Skip built-in speaker as we add it separately
            AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> AudioDeviceType.WIRED_HEADSET
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> AudioDeviceType.BLUETOOTH_SPEAKER
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> AudioDeviceType.BLUETOOTH_HEADSET
            AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET -> AudioDeviceType.USB_DEVICE
            else -> AudioDeviceType.UNKNOWN
        }
        
        // Get a meaningful name for the device
        val deviceName = when {
            !device.productName.isNullOrEmpty() -> device.productName.toString()
            type != AudioDeviceType.UNKNOWN -> type.getDisplayName()
            else -> "Unknown Device"
        }
        
        return com.example.purrytify.domain.model.AudioDeviceInfo(
            id = device.id,
            name = deviceName,
            type = type,
            isConnected = true, // If it's in the list, it's connected
            isActive = isDeviceActive(device)
        )
    }
    
    /**
     * Check if device is currently active
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun isDeviceActive(device: AudioDeviceInfo): Boolean {
        return try {
            Log.d(TAG, "Checking if device is active: ${device.productName} (type: ${device.type})")
            
            when (device.type) {
                AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> {
                    val isActive = audioManager.isWiredHeadsetOn && !audioManager.isSpeakerphoneOn
                    Log.d(TAG, "Wired headset active: $isActive")
                    isActive
                }
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> {
                    val isActive = audioManager.isBluetoothA2dpOn && !audioManager.isBluetoothScoOn
                    Log.d(TAG, "Bluetooth A2DP active: $isActive")
                    isActive
                }
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> {
                    val isActive = audioManager.isBluetoothScoOn
                    Log.d(TAG, "Bluetooth SCO active: $isActive")
                    isActive
                }
                AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET -> {
                    // USB devices are typically active when connected if no other route is preferred
                    val isActive = !audioManager.isSpeakerphoneOn && 
                                  !audioManager.isWiredHeadsetOn && 
                                  !audioManager.isBluetoothA2dpOn && 
                                  !audioManager.isBluetoothScoOn
                    Log.d(TAG, "USB device active: $isActive")
                    isActive
                }
                else -> {
                    Log.d(TAG, "Unknown device type, assuming inactive")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking device active status: ${e.message}")
            false
        }
    }
    
    /**
     * Check if currently using built-in speaker
     */
    private fun isUsingBuiltInSpeaker(): Boolean {
        return try {
            Log.d(TAG, "Checking built-in speaker status:")
            Log.d(TAG, "  - SpeakerphoneOn: ${audioManager.isSpeakerphoneOn}")
            Log.d(TAG, "  - WiredHeadsetOn: ${audioManager.isWiredHeadsetOn}")
            Log.d(TAG, "  - BluetoothA2dpOn: ${audioManager.isBluetoothA2dpOn}")
            Log.d(TAG, "  - BluetoothScoOn: ${audioManager.isBluetoothScoOn}")
            Log.d(TAG, "  - Audio Mode: ${audioManager.mode}")
            
            // Built-in speaker is active if:
            // 1. Speakerphone is explicitly on, OR
            // 2. No other audio route is active
            val isBuiltInActive = audioManager.isSpeakerphoneOn || 
                (!audioManager.isWiredHeadsetOn && 
                 !audioManager.isBluetoothA2dpOn && 
                 !audioManager.isBluetoothScoOn)
            
            Log.d(TAG, "Built-in speaker active: $isBuiltInActive")
            return isBuiltInActive
        } catch (e: Exception) {
            Log.e(TAG, "Error checking built-in speaker status: ${e.message}")
            true // Default to true if we can't determine
        }
    }
    
    /**
     * Switch to a specific audio device
     */
    fun switchToDevice(deviceInfo: com.example.purrytify.domain.model.AudioDeviceInfo) {
        externalScope.launch(Dispatchers.Main) {
            try {
                Log.d(TAG, "Switching to device: ${deviceInfo.name} (${deviceInfo.type})")
                
                // First, stop all current audio routes
                audioManager.stopBluetoothSco()
                audioManager.isBluetoothScoOn = false
                
                when (deviceInfo.type) {
                    AudioDeviceType.BUILT_IN_SPEAKER -> {
                        Log.d(TAG, "Forcing audio to built-in speaker")
                        
                        // Disable all other audio routes first
                        audioManager.isBluetoothScoOn = false
                        audioManager.stopBluetoothSco()
                        
                        // Set audio mode to normal (this is crucial)
                        audioManager.mode = AudioManager.MODE_NORMAL
                        
                        // Force speakerphone on (this overrides wired headset routing)
                        audioManager.isSpeakerphoneOn = true
                        
                        // For API 23+, try to set preferred device
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            try {
                                // Clear any communication device preference
                                audioManager.clearCommunicationDevice()
                                
                                // Try to get built-in speaker device and set it as preferred
                                val outputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                                val builtInSpeaker = outputDevices.find { 
                                    it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER 
                                }
                                
                                builtInSpeaker?.let { device ->
                                    // This method might not be available in all API levels
                                    try {
                                        val setPreferredMethod = audioManager.javaClass.getMethod(
                                            "setPreferredDevice", 
                                            AudioDeviceInfo::class.java
                                        )
                                        setPreferredMethod.invoke(audioManager, device)
                                        Log.d(TAG, "Set preferred device to built-in speaker")
                                    } catch (e: Exception) {
                                        Log.d(TAG, "setPreferredDevice not available, using fallback")
                                    }
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to set communication device: ${e.message}")
                            }
                        }
                        
                        Log.d(TAG, "Speaker settings - SpeakerOn: ${audioManager.isSpeakerphoneOn}, Mode: ${audioManager.mode}")
                    }
                    
                    AudioDeviceType.WIRED_HEADSET -> {
                        Log.d(TAG, "Switching to wired headset")
                        // Wired headset should be automatically selected when connected
                        audioManager.isSpeakerphoneOn = false
                        audioManager.isBluetoothScoOn = false
                        audioManager.mode = AudioManager.MODE_NORMAL
                    }
                    
                    AudioDeviceType.BLUETOOTH_SPEAKER -> {
                        Log.d(TAG, "Switching to Bluetooth A2DP")
                        // Switch to Bluetooth A2DP (music/media)
                        audioManager.isSpeakerphoneOn = false
                        audioManager.mode = AudioManager.MODE_NORMAL
                        
                        // Don't use SCO for A2DP devices - they should route automatically
                        audioManager.isBluetoothScoOn = false
                        
                        Log.d(TAG, "Bluetooth A2DP settings - A2DP: ${audioManager.isBluetoothA2dpOn}")
                    }
                    
                    AudioDeviceType.BLUETOOTH_HEADSET -> {
                        Log.d(TAG, "Switching to Bluetooth SCO (hands-free)")
                        // Switch to Bluetooth SCO (hands-free/calls)
                        audioManager.isSpeakerphoneOn = false
                        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                        audioManager.isBluetoothScoOn = true
                        audioManager.startBluetoothSco()
                        
                        // Wait for SCO to establish
                        kotlinx.coroutines.delay(1000)
                        
                        Log.d(TAG, "Bluetooth SCO settings - SCO: ${audioManager.isBluetoothScoOn}")
                    }
                    
                    AudioDeviceType.USB_DEVICE -> {
                        Log.d(TAG, "Switching to USB device")
                        audioManager.isSpeakerphoneOn = false
                        audioManager.isBluetoothScoOn = false
                        audioManager.mode = AudioManager.MODE_NORMAL
                    }
                    
                    AudioDeviceType.UNKNOWN -> {
                        Log.w(TAG, "Cannot switch to unknown device type")
                        _errorMessage.value = "Cannot switch to unknown device type"
                        return@launch
                    }
                }
                
                // Wait longer for audio routing to take effect
                kotlinx.coroutines.delay(800)
                
                // Log current audio state for debugging
                Log.d(TAG, "Post-switch audio state:")
                Log.d(TAG, "  - SpeakerphoneOn: ${audioManager.isSpeakerphoneOn}")
                Log.d(TAG, "  - WiredHeadsetOn: ${audioManager.isWiredHeadsetOn}")
                Log.d(TAG, "  - BluetoothA2dpOn: ${audioManager.isBluetoothA2dpOn}")
                Log.d(TAG, "  - BluetoothScoOn: ${audioManager.isBluetoothScoOn}")
                Log.d(TAG, "  - Audio Mode: ${audioManager.mode}")
                
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
     * Handle device disconnection (fallback to built-in speaker)
     */
    fun handleDeviceDisconnection(deviceInfo: com.example.purrytify.domain.model.AudioDeviceInfo) {
        externalScope.launch {
            try {
                Log.d(TAG, "Device disconnected: ${deviceInfo.name}")
                
                // Switch back to built-in speaker
                val builtInSpeaker = com.example.purrytify.domain.model.AudioDeviceInfo(
                    id = 0,
                    name = "Phone Speaker",
                    type = AudioDeviceType.BUILT_IN_SPEAKER,
                    isConnected = true
                )
                
                switchToDevice(builtInSpeaker)
                
                _errorMessage.value = "${deviceInfo.name} disconnected. Switched to phone speaker."
                
            } catch (e: Exception) {
                Log.e(TAG, "Error handling device disconnection: ${e.message}", e)
            }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
}