package com.example.purrytify.util

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Utility class for file operations
 */
object FileUtils {
    private const val TAG = "FileUtils"
    
    /**
     * Save content from a URI to a file in the app's files directory
     * @param context Application context
     * @param uri URI to save from
     * @param fileName Desired filename
     * @return The saved File or null if failed
     */
    fun saveUriToFile(context: Context, uri: Uri, fileName: String): File? {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val outputFile = File(context.filesDir, fileName)
            
            inputStream.use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            return outputFile
        } catch (e: IOException) {
            Log.e(TAG, "Error saving URI to file: ${e.message}", e)
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error saving URI to file: ${e.message}", e)
            return null
        }
    }
    
    /**
     * Save byte array to a temporary file
     * @param context Application context
     * @param bytes Byte array to save
     * @param prefix Temporary file prefix
     * @param suffix Temporary file suffix
     * @return The saved File or null if failed
     */
    fun saveBytesToTempFile(context: Context, bytes: ByteArray, prefix: String, suffix: String): File? {
        try {
            val tempFile = File.createTempFile(prefix, suffix, context.cacheDir)
            
            FileOutputStream(tempFile).use { output ->
                output.write(bytes)
            }
            
            return tempFile
        } catch (e: IOException) {
            Log.e(TAG, "Error saving bytes to temp file: ${e.message}", e)
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error saving bytes to temp file: ${e.message}", e)
            return null
        }
    }
    
    /**
     * Delete a file at the specified path
     * @param filePath Path of the file to delete
     * @return True if deleted successfully, false otherwise
     */
    fun deleteFile(filePath: String): Boolean {
        try {
            val file = File(filePath)
            if (file.exists()) {
                return file.delete()
            }
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting file: ${e.message}", e)
            return false
        }
    }
}