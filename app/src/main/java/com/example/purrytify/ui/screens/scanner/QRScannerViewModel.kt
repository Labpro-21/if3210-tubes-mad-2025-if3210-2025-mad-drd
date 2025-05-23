package com.example.purrytify.ui.screens.scanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.util.DeepLinkHandler
import com.example.purrytify.util.QRCodeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for QR Scanner screen
 */
@HiltViewModel
class QRScannerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deepLinkHandler: DeepLinkHandler
) : ViewModel() {

    private val TAG = "QRScannerViewModel"

    // UI States
    private val _uiState = MutableStateFlow<QRScannerUiState>(QRScannerUiState.Scanning)
    val uiState: StateFlow<QRScannerUiState> = _uiState.asStateFlow()

    // Camera permission state
    private val _hasCameraPermission = MutableStateFlow(false)
    val hasCameraPermission: StateFlow<Boolean> = _hasCameraPermission.asStateFlow()

    // Manual input state
    private val _manualInputText = MutableStateFlow("")
    val manualInputText: StateFlow<String> = _manualInputText.asStateFlow()

    // Error messages
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Success navigation
    private val _navigateToPlayer = MutableStateFlow<String?>(null)
    val navigateToPlayer: StateFlow<String?> = _navigateToPlayer.asStateFlow()

    init {
        checkCameraPermission()
    }

    /**
     * Check if camera permission is granted
     */
    private fun checkCameraPermission() {
        _hasCameraPermission.value = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Update camera permission status
     */
    fun updateCameraPermission(granted: Boolean) {
        _hasCameraPermission.value = granted
        if (granted) {
            _uiState.value = QRScannerUiState.Scanning
        }
    }

    /**
     * Process scanned QR code
     */
    fun processScannedCode(code: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Processing scanned code: $code")
                
                // Validate if it's a valid Purrytify song link
                if (QRCodeUtils.isValidSongDeepLink(code)) {
                    val songId = QRCodeUtils.extractSongIdFromDeepLink(code)
                    if (songId != null) {
                        Log.d(TAG, "Valid Purrytify song link detected, song ID: $songId")
                        _uiState.value = QRScannerUiState.Processing
                        
                        // Process the deep link
                        processDeepLink(code)
                    } else {
                        _errorMessage.value = "Invalid song link format"
                        _uiState.value = QRScannerUiState.Scanning
                    }
                } else {
                    _errorMessage.value = "This QR code is not a valid Purrytify song link"
                    _uiState.value = QRScannerUiState.Scanning
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing scanned code: ${e.message}", e)
                _errorMessage.value = "Failed to process QR code"
                _uiState.value = QRScannerUiState.Scanning
            }
        }
    }

    /**
     * Process manually input link
     */
    fun processManualInput() {
        viewModelScope.launch {
            val inputText = _manualInputText.value.trim()
            
            if (inputText.isEmpty()) {
                _errorMessage.value = "Please enter a song link"
                return@launch
            }
            
            try {
                Log.d(TAG, "Processing manual input: $inputText")
                
                // Validate and sanitize the input
                val sanitizedLink = QRCodeUtils.validateAndSanitizeInput(inputText)
                
                if (sanitizedLink != null) {
                    val songId = QRCodeUtils.extractSongIdFromDeepLink(sanitizedLink)
                    if (songId != null) {
                        Log.d(TAG, "Valid Purrytify song link from manual input, song ID: $songId")
                        _uiState.value = QRScannerUiState.Processing
                        
                        // Process the deep link
                        processDeepLink(sanitizedLink)
                    } else {
                        _errorMessage.value = "Invalid song link format"
                    }
                } else {
                    _errorMessage.value = QRCodeUtils.getValidationErrorMessage(inputText)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing manual input: ${e.message}", e)
                _errorMessage.value = "Failed to process song link"
            }
        }
    }

    /**
     * Process deep link using the existing deep link handler
     */
    private fun processDeepLink(link: String) {
        try {
            // Create a mock intent with the deep link
            val mockIntent = android.content.Intent().apply {
                action = android.content.Intent.ACTION_VIEW
                data = android.net.Uri.parse(link)
            }
            
            // Use the existing deep link handler
            deepLinkHandler.handleDeepLink(
                intent = mockIntent,
                onNavigateToPlayer = { songId ->
                    Log.d(TAG, "Successfully processed deep link, navigating to player for song: $songId")
                    _navigateToPlayer.value = songId
                    _uiState.value = QRScannerUiState.Success
                },
                onShowError = { errorMessage ->
                    Log.e(TAG, "Deep link processing failed: $errorMessage")
                    _errorMessage.value = errorMessage
                    _uiState.value = QRScannerUiState.Scanning
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating deep link intent: ${e.message}", e)
            _errorMessage.value = "Failed to process song link"
            _uiState.value = QRScannerUiState.Scanning
        }
    }

    /**
     * Update manual input text
     */
    fun updateManualInputText(text: String) {
        _manualInputText.value = text
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Clear navigation state
     */
    fun clearNavigation() {
        _navigateToPlayer.value = null
    }

    /**
     * Switch to manual input mode
     */
    fun switchToManualInput() {
        _uiState.value = QRScannerUiState.ManualInput
    }

    /**
     * Switch to scanning mode
     */
    fun switchToScanning() {
        if (_hasCameraPermission.value) {
            _uiState.value = QRScannerUiState.Scanning
        } else {
            _errorMessage.value = "Camera permission is required for scanning"
        }
    }

    /**
     * Reset to initial scanning state
     */
    fun resetToScanning() {
        if (_hasCameraPermission.value) {
            _uiState.value = QRScannerUiState.Scanning
        }
        _manualInputText.value = ""
        _errorMessage.value = null
    }
}

/**
 * UI states for QR Scanner
 */
sealed class QRScannerUiState {
    data object Scanning : QRScannerUiState()
    data object ManualInput : QRScannerUiState()
    data object Processing : QRScannerUiState()
    data object Success : QRScannerUiState()
}