package com.example.purrytify.util

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.util.EnumMap

/**
 * Utility class for generating QR codes
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
        val deepLink = "purrytify://song/$songId"
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
            deepLink.startsWith("purrytify://song/") && 
            deepLink.substring("purrytify://song/".length).isNotEmpty()
        } catch (e: Exception) {
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
                deepLink.substring("purrytify://song/".length)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting song ID from deep link: ${e.message}", e)
            null
        }
    }
}