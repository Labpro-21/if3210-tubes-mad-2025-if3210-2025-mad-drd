package com.example.purrytify.domain.model

/**
 * Represents an audio output device
 */
data class AudioDeviceInfo(
    val id: Int,
    val name: String,
    val type: AudioDeviceType,
    val isConnected: Boolean,
    val isActive: Boolean = false
)

/**
 * Types of audio devices
 */
enum class AudioDeviceType {
    BUILT_IN_SPEAKER,
    WIRED_HEADSET,
    BLUETOOTH_SPEAKER,
    BLUETOOTH_HEADSET,
    USB_DEVICE,
    UNKNOWN;
    
    fun getDisplayName(): String {
        return when (this) {
            BUILT_IN_SPEAKER -> "Phone Speaker"
            WIRED_HEADSET -> "Wired Headset"
            BLUETOOTH_SPEAKER -> "Bluetooth Speaker"
            BLUETOOTH_HEADSET -> "Bluetooth Headset"
            USB_DEVICE -> "USB Audio"
            UNKNOWN -> "Unknown Device"
        }
    }
}