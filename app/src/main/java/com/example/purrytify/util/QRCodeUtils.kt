package com.example.purrytify.util

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.util.EnumMap

/**
 * Utility class for generating and processing QR codes
 */
object QRCodeUtils {
    private const val TAG = "QRCodeUtils"
    
    /**
     * Generate a QR code bitmap from the given text
     * @param text The text to encode in the QR code
     * @param width The width of the QR code bitmap
     * @param height The height of the QR code bitmap
     * @return Bitmap containing the QR code, or null if generation fails
     */
    fun generateQRCode(
        text: String,
        width: Int = 512,
        height: Int = 512
    ): Bitmap? {
        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
            hints[EncodeHintType.MARGIN] = 1
            
            val qrCodeWriter = QRCodeWriter()
            val bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hints)
            
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            
            Log.d(TAG, "Successfully generated QR code for: $text")
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error generating QR code: ${e.message}", e)
            null
        }
    }
    
    /**
     * Generate a QR code for a song deep link
     * @param songId The ID of the song to create a deep link for
     * @param width The width of the QR code bitmap
     * @param height The height of the QR code bitmap
     * @return Bitmap containing the QR code, or null if generation fails
     */
    fun generateSongQRCode(
        songId: String,
        width: Int = 512,
        height: Int = 512
    ): Bitmap? {
        val deepLink = createSongDeepLink(songId)
        return generateQRCode(deepLink, width, height)
    }
    
    /**
     * Create a deep link URL for a song
     * @param songId The ID of the song
     * @return The deep link URL
     */
    fun createSongDeepLink(songId: String): String {
        return "purrytify://song/$songId"
    }
    
    /**
     * Validate if a deep link is a valid Purrytify song link
     * @param deepLink The deep link to validate
     * @return True if valid, false otherwise
     */
    fun isValidSongDeepLink(deepLink: String): Boolean {
        return try {
            // More comprehensive validation
            when {
                deepLink.isBlank() -> false
                !deepLink.startsWith("purrytify://song/") -> false
                else -> {
                    val songId = deepLink.substring("purrytify://song/".length)
                    songId.isNotEmpty() && songId.trim() == songId && songId.matches(Regex("^[a-zA-Z0-9_-]+$"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error validating deep link: ${e.message}", e)
            false
        }
    }
    
    /**
     * Extract song ID from a deep link
     * @param deepLink The deep link to extract from
     * @return The song ID, or null if extraction fails
     */
    fun extractSongIdFromDeepLink(deepLink: String): String? {
        return try {
            if (isValidSongDeepLink(deepLink)) {
                val songId = deepLink.substring("purrytify://song/".length)
                if (songId.isNotEmpty()) songId else null
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting song ID from deep link: ${e.message}", e)
            null
        }
    }
    
    /**
     * Validate and sanitize a manually input song link
     * @param input The user input to validate
     * @return Sanitized deep link if valid, null otherwise
     */
    fun validateAndSanitizeInput(input: String): String? {
        val trimmedInput = input.trim()
        
        // Handle various input formats
        return when {
            // Already a proper deep link
            trimmedInput.startsWith("purrytify://song/") -> {
                if (isValidSongDeepLink(trimmedInput)) trimmedInput else null
            }
            
            // Just the song ID (create full deep link)
            trimmedInput.matches(Regex("^[a-zA-Z0-9_-]+$")) -> {
                createSongDeepLink(trimmedInput)
            }
            
            // HTTP/HTTPS URLs that might contain song ID (future enhancement)
            trimmedInput.startsWith("http") -> {
                // For now, reject HTTP links, but this could be enhanced
                // to extract song IDs from web URLs in the future
                null
            }
            
            else -> null
        }
    }
    
    /**
     * Get a user-friendly error message for invalid input
     * @param input The invalid input
     * @return User-friendly error message
     */
    fun getValidationErrorMessage(input: String): String {
        val trimmedInput = input.trim()
        
        return when {
            trimmedInput.isEmpty() -> "Please enter a song link"
            trimmedInput.startsWith("http") -> "Web URLs are not supported yet. Please use purrytify:// links"
            trimmedInput.contains(" ") -> "Song links should not contain spaces"
            !trimmedInput.startsWith("purrytify://") -> "Link must start with 'purrytify://song/'"
            !trimmedInput.startsWith("purrytify://song/") -> "Link must be in format 'purrytify://song/SONG_ID'"
            else -> "Invalid song link format"
        }
    }
}