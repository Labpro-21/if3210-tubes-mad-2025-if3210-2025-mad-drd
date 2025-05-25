package com.example.purrytify.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Utility class for audio-related operations
 */
object AudioUtils {
    private const val TAG = "AudioUtils"
    
    /**
     * Extract metadata from an audio file
     * @param context Application context
     * @param uri URI of the audio file
     * @return Map of metadata keys to values
     */
    suspend fun getAudioMetadata(context: Context, uri: Uri): Map<String, String> = withContext(Dispatchers.IO) {
        val metadata = mutableMapOf<String, String>()
        val retriever = MediaMetadataRetriever()
        
        try {
            retriever.setDataSource(context, uri)
            
            // Extract basic metadata
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.let {
                metadata["title"] = it
            }
            
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.let {
                metadata["artist"] = it
            }
            
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.let {
                metadata["duration"] = it
            }
            
            // Extract album art
            val albumArt = retriever.embeddedPicture
            if (albumArt != null) {
                // Save the album art to a temporary file
                val albumArtFile = FileUtils.saveBytesToTempFile(context, albumArt, "temp_albumart", ".jpg")
                albumArtFile?.let {
                    metadata["albumArt"] = Uri.fromFile(it).toString()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting metadata: ${e.message}", e)
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing metadata retriever: ${e.message}", e)
            }
        }
        
        metadata
    }
    
    /**
     * Get the duration of an audio file in milliseconds
     * @param context Application context
     * @param uri URI of the audio file
     * @return Duration in milliseconds, or 0 if unable to determine
     */
    suspend fun getAudioDuration(context: Context, uri: Uri): Long = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        var duration = 0L
        
        try {
            retriever.setDataSource(context, uri)
            
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.let {
                duration = it.toLongOrNull() ?: 0L
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting audio duration: ${e.message}", e)
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing metadata retriever: ${e.message}", e)
            }
        }
        
        duration
    }

    /**
    * Extract metadata from an audio file URI
    * @param context Application context
    * @return Map of metadata keys to values
    */
    suspend fun Uri.getAudioMetadata(context: Context): Map<String, String> = withContext(Dispatchers.IO) {
        val metadata = mutableMapOf<String, String>()
        val retriever = MediaMetadataRetriever()
        
        try {
            retriever.setDataSource(context, this@getAudioMetadata)
            
            // Extract basic metadata
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.let {
                metadata["title"] = it
            }
            
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.let {
                metadata["artist"] = it
            }
            
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.let {
                metadata["duration"] = it
            }
            
            // Extract album art
            val albumArt = retriever.embeddedPicture
            if (albumArt != null) {
                // Save the album art to a temporary file
                val albumArtFile = FileUtils.saveBytesToTempFile(context, albumArt, "temp_albumart", ".jpg")
                albumArtFile?.let {
                    metadata["albumArt"] = Uri.fromFile(it).toString()
                }
            }
        } catch (e: Exception) {
            Log.e("AudioUtils", "Error extracting metadata: ${e.message}", e)
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                Log.e("AudioUtils", "Error releasing metadata retriever: ${e.message}", e)
            }
        }
        
        metadata
    }

    /**
    * Get the duration of an audio file in milliseconds
    * @param context Application context
    * @return Duration in milliseconds, or 0 if unable to determine
    */
    suspend fun Uri.getAudioDuration(context: Context): Long = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        var duration = 0L
        
        try {
            retriever.setDataSource(context, this@getAudioDuration)
            
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.let {
                duration = it.toLongOrNull() ?: 0L
            }
        } catch (e: Exception) {
            Log.e("AudioUtils", "Error getting audio duration: ${e.message}", e)
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                Log.e("AudioUtils", "Error releasing metadata retriever: ${e.message}", e)
            }
        }
        
        duration
    }
}