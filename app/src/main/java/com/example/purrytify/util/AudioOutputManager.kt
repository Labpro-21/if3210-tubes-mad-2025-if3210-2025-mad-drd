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
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
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

/**
 * AudioOutputManager focused on media playback routing
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
    
    // Track the user's intended device for media playback
    private val _userSelectedDevice = MutableStateFlow<com.example.purrytify.domain.model.AudioDeviceInfo?>(null)
    
    // Audio focus request for newer APIs
    private var audioFocusRequest: AudioFocusRequest? = null
    
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
        setupAudioFocus()
        
        // Ensure we're using the correct audio mode for media playback
        audioManager.mode = AudioManager.MODE_NORMAL
    }
    
    /**
     * Setup audio focus for media playback
     */
    private fun setupAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setOnAudioFocusChangeListener { focusChange ->
                    Log.d(TAG, "Audio focus changed: $focusChange")
                }
                .build()
        }
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
                
                // Determine active device based on actual system state
                val activeDevice = determineActiveDevice(devices)
                
                // Update state
                withContext(Dispatchers.Main) {
                    _availableDevices.value = devices
                    _activeDevice.value = activeDevice
                    
                    Log.d(TAG, "Found ${devices.size} devices, active: ${activeDevice?.name}")
                    Log.d(TAG, "Audio Manager State - Speaker: ${audioManager.isSpeakerphoneOn}, BT SCO: ${audioManager.isBluetoothScoOn}, BT A2DP: ${audioManager.isBluetoothA2dpOn}, Wired: ${audioManager.isWiredHeadsetOn}")
                    devices.forEach { device ->
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
        if (audioManager.isBluetoothA2dpOn) {
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
        
        if (audioManager.isBluetoothScoOn) {
            devices.add(
                com.example.purrytify.domain.model.AudioDeviceInfo(
                    id = 3,
                    name = "Bluetooth Headset",
                    type = AudioDeviceType.BLUETOOTH_HEADSET,
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
     * Determine which device is currently active based on actual system state
     */
    private fun determineActiveDevice(devices: List<com.example.purrytify.domain.model.AudioDeviceInfo>): com.example.purrytify.domain.model.AudioDeviceInfo? {
        // Check actual system audio routing state
        return when {
            // Priority 1: If speakerphone is explicitly enabled, speaker is active
            audioManager.isSpeakerphoneOn -> {
                Log.d(TAG, "Speaker is active (speakerphone on)")
                devices.find { it.type == AudioDeviceType.BUILT_IN_SPEAKER }
            }
            // Priority 2: If Bluetooth SCO is active (for calls/voice)
            audioManager.isBluetoothScoOn -> {
                Log.d(TAG, "Bluetooth SCO is active")
                devices.find { it.type == AudioDeviceType.BLUETOOTH_HEADSET }
            }
            // Priority 3: If Bluetooth A2DP is active (for music) - most common case
            audioManager.isBluetoothA2dpOn -> {
                Log.d(TAG, "Bluetooth A2DP is active")
                devices.find { it.type == AudioDeviceType.BLUETOOTH_SPEAKER }
            }
            // Priority 4: If wired headset is connected and nothing else is forced
            audioManager.isWiredHeadsetOn -> {
                Log.d(TAG, "Wired headset is active")
                devices.find { it.type == AudioDeviceType.WIRED_HEADSET }
            }
            // Default: Built-in speaker
            else -> {
                Log.d(TAG, "Defaulting to built-in speaker")
                devices.find { it.type == AudioDeviceType.BUILT_IN_SPEAKER }
            }
        }?.copy(isActive = true)
    }
    
    /**
     * Switch to selected audio device - MEDIA PLAYBACK FOCUSED VERSION
     */
    fun switchToDevice(deviceInfo: com.example.purrytify.domain.model.AudioDeviceInfo) {
        externalScope.launch(Dispatchers.Main) {
            try {
                Log.d(TAG, "=== Switching to device: ${deviceInfo.name} (${deviceInfo.type}) ===")
                
                // Store user selection
                _userSelectedDevice.value = deviceInfo
                
                // Ensure we're in the correct audio mode for media playback
                audioManager.mode = AudioManager.MODE_NORMAL
                
                // Request audio focus for media
                requestAudioFocusForMedia()
                
                // Perform the switch based on device type
                when (deviceInfo.type) {
                    AudioDeviceType.BUILT_IN_SPEAKER -> {
                        switchToSpeakerForMedia()
                    }
                    AudioDeviceType.WIRED_HEADSET -> {
                        switchToWiredHeadsetForMedia()
                    }
                    AudioDeviceType.BLUETOOTH_SPEAKER -> {
                        switchToBluetoothA2dpForMedia()
                    }
                    AudioDeviceType.BLUETOOTH_HEADSET -> {
                        switchToBluetoothScoForMedia()
                    }
                    AudioDeviceType.USB_DEVICE -> {
                        switchToUsbForMedia()
                    }
                    AudioDeviceType.UNKNOWN -> {
                        _errorMessage.value = "Cannot switch to unknown device"
                        return@launch
                    }
                }
                
                // Wait for the switch to take effect
                delay(1000)
                
                // Refresh devices to update active status
                refreshAvailableDevices()
                
                // Verify the switch worked
                delay(300)
                val currentActive = _activeDevice.value
                if (currentActive?.type == deviceInfo.type) {
                    Log.d(TAG, "✓ Successfully switched to ${deviceInfo.name}")
                } else {
                    Log.w(TAG, "⚠ Switch verification: Expected ${deviceInfo.type}, got ${currentActive?.type}")
                    // Don't show error for expected behavior (e.g., Bluetooth taking precedence)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error switching to device: ${e.message}", e)
                _errorMessage.value = "Failed to switch to ${deviceInfo.name}"
            }
        }
    }
    
    /**
     * Request audio focus specifically for media playback
     */
    private fun requestAudioFocusForMedia() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { request ->
                val result = audioManager.requestAudioFocus(request)
                Log.d(TAG, "Audio focus request result: $result")
            }
        } else {
            @Suppress("DEPRECATION")
            val result = audioManager.requestAudioFocus(
                { focusChange -> Log.d(TAG, "Audio focus changed: $focusChange") },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
            Log.d(TAG, "Audio focus request result (legacy): $result")
        }
    }
    
    /**
     * Switch to built-in speaker for media playback
     */
    private fun switchToSpeakerForMedia() {
        Log.d(TAG, "Switching to built-in speaker for media playback")
        
        // Stop all Bluetooth audio first
        audioManager.stopBluetoothSco()
        audioManager.isBluetoothScoOn = false
        
        // Force audio to speaker - this overrides wired headphones
        audioManager.isSpeakerphoneOn = true
        
        Log.d(TAG, "Speaker routing applied - isSpeakerphoneOn: ${audioManager.isSpeakerphoneOn}")
        Log.d(TAG, "Post-switch state - BT SCO: ${audioManager.isBluetoothScoOn}, BT A2DP: ${audioManager.isBluetoothA2dpOn}")
    }
    
    /**
     * Switch to wired headset for media playback
     */
    private fun switchToWiredHeadsetForMedia() {
        Log.d(TAG, "Switching to wired headset for media playback")
        
        // Stop Bluetooth audio
        audioManager.stopBluetoothSco()
        audioManager.isBluetoothScoOn = false
        
        // Disable speaker phone to allow wired headset
        audioManager.isSpeakerphoneOn = false
        
        Log.d(TAG, "Wired headset routing applied - isSpeakerphoneOn: ${audioManager.isSpeakerphoneOn}")
    }
    
    /**
     * Switch to Bluetooth A2DP for media playback
     */
    private fun switchToBluetoothA2dpForMedia() {
        Log.d(TAG, "Switching to Bluetooth A2DP for media playback")
        
        // Disable speaker phone
        audioManager.isSpeakerphoneOn = false
        
        // Stop SCO to allow A2DP (they're mutually exclusive)
        audioManager.stopBluetoothSco()
        audioManager.isBluetoothScoOn = false
        
        // A2DP should be automatically used for media when available
        Log.d(TAG, "Bluetooth A2DP routing applied - BT A2DP available: ${audioManager.isBluetoothA2dpOn}")
    }
    
    /**
     * Switch to Bluetooth SCO for media playback (less common for music)
     */
    private fun switchToBluetoothScoForMedia() {
        Log.d(TAG, "Switching to Bluetooth SCO for media playback")
        
        // Disable speaker phone
        audioManager.isSpeakerphoneOn = false
        
        // Enable Bluetooth SCO
        audioManager.isBluetoothScoOn = true
        audioManager.startBluetoothSco()
        
        Log.d(TAG, "Bluetooth SCO routing applied - BT SCO: ${audioManager.isBluetoothScoOn}")
    }
    
    /**
     * Switch to USB device for media playback
     */
    private fun switchToUsbForMedia() {
        Log.d(TAG, "Switching to USB device for media playback")
        
        // Stop Bluetooth audio
        audioManager.stopBluetoothSco()
        audioManager.isBluetoothScoOn = false
        
        // Disable speaker phone
        audioManager.isSpeakerphoneOn = false
        
        Log.d(TAG, "USB routing applied")
    }
    
    /**
     * Handle device disconnection
     */
    fun handleDeviceDisconnection(deviceInfo: com.example.purrytify.domain.model.AudioDeviceInfo) {
        externalScope.launch {
            try {
                Log.d(TAG, "Device disconnected: ${deviceInfo.name}")
                
                // Clear user selection if it was the disconnected device
                if (_userSelectedDevice.value?.type == deviceInfo.type) {
                    _userSelectedDevice.value = null
                }
                
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
            // Release audio focus
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { request ->
                    audioManager.abandonAudioFocusRequest(request)
                }
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus { /* focus change listener */ }
            }
            
            stopDeviceMonitoring()
            Log.d(TAG, "AudioOutputManager cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup: ${e.message}", e)
        }
    }
}