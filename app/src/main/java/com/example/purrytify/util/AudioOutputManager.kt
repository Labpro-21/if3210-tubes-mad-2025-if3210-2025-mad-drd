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
 * Refactored audio output manager with improved device switching and routing
 */
@Singleton
class AudioOutputManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val externalScope: CoroutineScope
) {
    private val TAG = "AudioOutputManager"
    
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    
    // Audio routing state tracking
    private var currentAudioRoute = AudioRoute.SPEAKER
    private var previousAudioMode = AudioManager.MODE_NORMAL
    
    // Available audio devices
    private val _availableDevices = MutableStateFlow<List<com.example.purrytify.domain.model.AudioDeviceInfo>>(emptyList())
    val availableDevices: StateFlow<List<com.example.purrytify.domain.model.AudioDeviceInfo>> = _availableDevices.asStateFlow()
    
    // Currently active device
    private val _activeDevice = MutableStateFlow<com.example.purrytify.domain.model.AudioDeviceInfo?>(null)
    val activeDevice: StateFlow<com.example.purrytify.domain.model.AudioDeviceInfo?> = _activeDevice.asStateFlow()
    
    // Error messages
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Internal audio routing enum for cleaner state management
    private enum class AudioRoute {
        SPEAKER, WIRED_HEADSET, BLUETOOTH_A2DP, BLUETOOTH_SCO, USB
    }
    
    // Simplified device change receiver
    private val deviceChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED,
                BluetoothDevice.ACTION_ACL_DISCONNECTED,
                AudioManager.ACTION_AUDIO_BECOMING_NOISY,
                AudioManager.ACTION_HEADSET_PLUG -> {
                    Log.d(TAG, "Audio device state changed: ${intent.action}")
                    scanAvailableDevices()
                }
            }
        }
    }
    
    // Modern audio device callback for API 23+
    @RequiresApi(Build.VERSION_CODES.M)
    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            Log.d(TAG, "Audio devices added: ${addedDevices?.size}")
            scanAvailableDevices()
        }
        
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            Log.d(TAG, "Audio devices removed: ${removedDevices?.size}")
            scanAvailableDevices()
        }
    }
    
    init {
        initializeAudioRouting()
        startDeviceMonitoring()
        scanAvailableDevices()
    }
    
    /**
     * Initialize audio routing state
     */
    private fun initializeAudioRouting() {
        previousAudioMode = audioManager.mode
        currentAudioRoute = determineCurrentAudioRoute()
        Log.d(TAG, "Initialized with route: $currentAudioRoute, mode: $previousAudioMode")
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
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)
            }
            
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
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
            }
            
            Log.d(TAG, "Stopped device monitoring")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping device monitoring: ${e.message}", e)
        }
    }
    
    /**
     * Determine current audio routing based on AudioManager state
     */
    private fun determineCurrentAudioRoute(): AudioRoute {
        return when {
            audioManager.isBluetoothScoOn -> AudioRoute.BLUETOOTH_SCO
            audioManager.isBluetoothA2dpOn -> AudioRoute.BLUETOOTH_A2DP
            audioManager.isWiredHeadsetOn -> AudioRoute.WIRED_HEADSET
            else -> AudioRoute.SPEAKER
        }
    }
    
    /**
     * Scan and update available audio devices
     */
    fun scanAvailableDevices() {
        externalScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "=== Scanning Available Devices ===")
                logCurrentAudioState()
                
                val devices = mutableListOf<com.example.purrytify.domain.model.AudioDeviceInfo>()
                
                // Always add built-in speaker
                val speakerActive = currentAudioRoute == AudioRoute.SPEAKER
                devices.add(createBuiltInSpeakerDevice(speakerActive))
                
                // Scan for connected devices
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    scanModernAudioDevices(devices)
                } else {
                    scanLegacyAudioDevices(devices)
                }
                
                // Update state
                _availableDevices.value = devices
                updateActiveDevice(devices)
                
                Log.d(TAG, "=== Device Scan Complete ===")
                Log.d(TAG, "Total devices: ${devices.size}, Active: ${_activeDevice.value?.name}")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error scanning devices: ${e.message}", e)
                _errorMessage.value = "Failed to scan audio devices"
            }
        }
    }
    
    /**
     * Create built-in speaker device info
     */
    private fun createBuiltInSpeakerDevice(isActive: Boolean) = 
        com.example.purrytify.domain.model.AudioDeviceInfo(
            id = 0,
            name = "Phone Speaker",
            type = AudioDeviceType.BUILT_IN_SPEAKER,
            isConnected = true,
            isActive = isActive
        )
    
    /**
     * Log current audio manager state for debugging
     */
    private fun logCurrentAudioState() {
        Log.d(TAG, "Audio State - Speaker: ${audioManager.isSpeakerphoneOn}, " +
                  "Wired: ${audioManager.isWiredHeadsetOn}, " +
                  "BT A2DP: ${audioManager.isBluetoothA2dpOn}, " +
                  "BT SCO: ${audioManager.isBluetoothScoOn}, " +
                  "Mode: ${audioManager.mode}")
    }
    
    /**
     * Scan audio devices using modern API (API 23+)
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun scanModernAudioDevices(devices: MutableList<com.example.purrytify.domain.model.AudioDeviceInfo>) {
        try {
            val audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            
            audioDevices.forEach { device ->
                createDeviceInfo(device)?.let { deviceInfo ->
                    devices.add(deviceInfo)
                    Log.d(TAG, "Found: ${deviceInfo.name} (${deviceInfo.type}, active: ${deviceInfo.isActive})")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning modern audio devices: ${e.message}")
        }
    }
    
    /**
     * Scan audio devices using legacy API
     */
    private fun scanLegacyAudioDevices(devices: MutableList<com.example.purrytify.domain.model.AudioDeviceInfo>) {
        // Wired headset
        if (audioManager.isWiredHeadsetOn) {
            devices.add(
                com.example.purrytify.domain.model.AudioDeviceInfo(
                    id = 1,
                    name = "Wired Headset",
                    type = AudioDeviceType.WIRED_HEADSET,
                    isConnected = true,
                    isActive = currentAudioRoute == AudioRoute.WIRED_HEADSET
                )
            )
        }
        
        // Bluetooth A2DP
        if (audioManager.isBluetoothA2dpOn) {
            devices.add(
                com.example.purrytify.domain.model.AudioDeviceInfo(
                    id = 2,
                    name = "Bluetooth Audio",
                    type = AudioDeviceType.BLUETOOTH_SPEAKER,
                    isConnected = true,
                    isActive = currentAudioRoute == AudioRoute.BLUETOOTH_A2DP
                )
            )
        }
    }
    
    /**
     * Create device info from AudioDeviceInfo (API 23+)
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun createDeviceInfo(device: AudioDeviceInfo): com.example.purrytify.domain.model.AudioDeviceInfo? {
        val type = when (device.type) {
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> return null // Skip built-in speaker
            AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> AudioDeviceType.WIRED_HEADSET
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> AudioDeviceType.BLUETOOTH_SPEAKER
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> AudioDeviceType.BLUETOOTH_HEADSET
            AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET -> AudioDeviceType.USB_DEVICE
            else -> AudioDeviceType.UNKNOWN
        }
        
        val deviceName = device.productName?.toString() ?: type.getDisplayName()
        val isActive = determineDeviceActiveStatus(device)
        
        return com.example.purrytify.domain.model.AudioDeviceInfo(
            id = device.id,
            name = deviceName,
            type = type,
            isConnected = true,
            isActive = isActive
        )
    }
    
    /**
     * Determine if a specific device is currently active
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun determineDeviceActiveStatus(device: AudioDeviceInfo): Boolean {
        return when (device.type) {
            AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> 
                currentAudioRoute == AudioRoute.WIRED_HEADSET
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> 
                currentAudioRoute == AudioRoute.BLUETOOTH_A2DP
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> 
                currentAudioRoute == AudioRoute.BLUETOOTH_SCO
            AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET -> 
                currentAudioRoute == AudioRoute.USB
            else -> false
        }
    }
    
    /**
     * Update active device in state
     */
    private fun updateActiveDevice(devices: List<com.example.purrytify.domain.model.AudioDeviceInfo>) {
        val activeDevice = devices.find { it.isActive }
        _activeDevice.value = activeDevice
    }
    
    /**
     * Main device switching method - this is the core fix for bluetooth-to-speaker switching
     */
    fun switchToDevice(deviceInfo: com.example.purrytify.domain.model.AudioDeviceInfo) {
        externalScope.launch(Dispatchers.Main) {
            try {
                Log.d(TAG, "=== Switching to: ${deviceInfo.name} (${deviceInfo.type}) ===")
                logCurrentAudioState()
                
                // First, completely reset audio routing to ensure clean switch
                resetAudioRouting()
                
                // Apply new routing based on device type
                val targetRoute = when (deviceInfo.type) {
                    AudioDeviceType.BUILT_IN_SPEAKER -> {
                        applyBuiltInSpeakerRouting()
                        AudioRoute.SPEAKER
                    }
                    AudioDeviceType.WIRED_HEADSET -> {
                        applyWiredHeadsetRouting()
                        AudioRoute.WIRED_HEADSET
                    }
                    AudioDeviceType.BLUETOOTH_SPEAKER -> {
                        applyBluetoothA2dpRouting()
                        AudioRoute.BLUETOOTH_A2DP
                    }
                    AudioDeviceType.BLUETOOTH_HEADSET -> {
                        applyBluetoothScoRouting()
                        AudioRoute.BLUETOOTH_SCO
                    }
                    AudioDeviceType.USB_DEVICE -> {
                        applyUsbRouting()
                        AudioRoute.USB
                    }
                    AudioDeviceType.UNKNOWN -> {
                        Log.w(TAG, "Cannot switch to unknown device type")
                        _errorMessage.value = "Cannot switch to unknown device"
                        return@launch
                    }
                }
                
                // Update internal state
                currentAudioRoute = targetRoute
                
                // Wait for audio routing to stabilize
                kotlinx.coroutines.delay(500)
                
                // Verify and log the switch result
                logCurrentAudioState()
                scanAvailableDevices()
                
                Log.d(TAG, "Successfully switched to ${deviceInfo.name}")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error switching to device: ${e.message}", e)
                _errorMessage.value = "Failed to switch to ${deviceInfo.name}"
            }
        }
    }
    
    /**
     * Reset all audio routing to a clean state
     */
    private fun resetAudioRouting() {
        Log.d(TAG, "Resetting audio routing...")
        
        // Stop all Bluetooth routing
        audioManager.stopBluetoothSco()
        audioManager.isBluetoothScoOn = false
        
        // Reset speaker phone
        audioManager.isSpeakerphoneOn = false
        
        // Reset audio mode
        audioManager.mode = AudioManager.MODE_NORMAL
        
        // Clear any communication device preferences (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                audioManager.clearCommunicationDevice()
            } catch (e: Exception) {
                Log.d(TAG, "clearCommunicationDevice not available: ${e.message}")
            }
        }
    }
    
    /**
     * Apply built-in speaker routing - THE KEY FIX
     */
    private fun applyBuiltInSpeakerRouting() {
        Log.d(TAG, "Applying built-in speaker routing")
        
        // CRITICAL: This is the main fix for the bluetooth-to-speaker issue
        // We need to aggressively force audio to the speaker
        
        // Step 1: Completely disable Bluetooth audio
        audioManager.stopBluetoothSco()
        audioManager.isBluetoothScoOn = false
        
        // Step 2: Set audio mode that doesn't prefer Bluetooth
        audioManager.mode = AudioManager.MODE_NORMAL
        
        // Step 3: Force speaker phone on - this overrides Bluetooth A2DP routing
        audioManager.isSpeakerphoneOn = true
        
        // Step 4: For modern APIs, try to set communication device to built-in speaker
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val outputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                val speakerDevice = outputDevices.find { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                speakerDevice?.let { device ->
                    audioManager.setCommunicationDevice(device)
                    Log.d(TAG, "Set communication device to built-in speaker")
                }
            } catch (e: Exception) {
                Log.d(TAG, "Failed to set communication device: ${e.message}")
            }
        }
        
        Log.d(TAG, "Built-in speaker routing applied")
    }
    
    /**
     * Apply wired headset routing
     */
    private fun applyWiredHeadsetRouting() {
        Log.d(TAG, "Applying wired headset routing")
        // Wired headset should be automatically preferred when connected
        audioManager.mode = AudioManager.MODE_NORMAL
    }
    
    /**
     * Apply Bluetooth A2DP routing
     */
    private fun applyBluetoothA2dpRouting() {
        Log.d(TAG, "Applying Bluetooth A2DP routing")
        audioManager.mode = AudioManager.MODE_NORMAL
        // A2DP should route automatically when available
    }
    
    /**
     * Apply Bluetooth SCO routing
     */
    private fun applyBluetoothScoRouting() {
        Log.d(TAG, "Applying Bluetooth SCO routing")
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isBluetoothScoOn = true
        audioManager.startBluetoothSco()
    }
    
    /**
     * Apply USB device routing
     */
    private fun applyUsbRouting() {
        Log.d(TAG, "Applying USB routing")
        audioManager.mode = AudioManager.MODE_NORMAL
        // USB should route automatically when connected
    }
    
    /**
     * Handle device disconnection - automatically fallback to speaker
     */
    fun handleDeviceDisconnection(deviceInfo: com.example.purrytify.domain.model.AudioDeviceInfo) {
        externalScope.launch {
            try {
                Log.d(TAG, "Device disconnected: ${deviceInfo.name}")
                
                // Fallback to built-in speaker
                val builtInSpeaker = createBuiltInSpeakerDevice(true)
                switchToDevice(builtInSpeaker)
                
                _errorMessage.value = "${deviceInfo.name} disconnected. Switched to phone speaker."
                
            } catch (e: Exception) {
                Log.e(TAG, "Error handling device disconnection: ${e.message}", e)
            }
        }
    }
    
    /**
     * Force refresh of available devices (renamed for compatibility)
     */
    fun refreshAvailableDevices() {
        scanAvailableDevices()
    }
    
    /**
     * Stop device monitoring (renamed for compatibility)
     */
    fun stopListening() {
        stopDeviceMonitoring()
    }
    
    
    /**
     * Clear any error messages
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * Cleanup method to call when the manager is no longer needed
     */
    fun cleanup() {
        try {
            stopDeviceMonitoring()
            // Reset to normal audio mode on cleanup
            audioManager.mode = previousAudioMode
            Log.d(TAG, "AudioOutputManager cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup: ${e.message}", e)
        }
    }
}