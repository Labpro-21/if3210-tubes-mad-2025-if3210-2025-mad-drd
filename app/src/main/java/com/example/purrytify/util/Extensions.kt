package com.example.purrytify.util

import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

// Context Extensions
fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

// Uri Extensions
suspend fun Uri.getAudioMetadata(context: Context): Map<String, String> {
    return withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        val metadata = mutableMapOf<String, String>()
        
        try {
            retriever.setDataSource(context, this@getAudioMetadata)
            
            // Get title
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.let {
                metadata["title"] = it
            }
            
            // Get artist
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.let {
                metadata["artist"] = it
            }
            
            // Get duration
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.let {
                metadata["duration"] = it
            }
            
            // Get album art if available
            val albumArt = retriever.embeddedPicture
            if (albumArt != null) {
                val artworkFile = saveAlbumArtToFile(context, albumArt)
                metadata["artworkPath"] = artworkFile.absolutePath
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            retriever.release()
        }
        
        metadata
    }
}

suspend fun Uri.getAudioDuration(context: Context): Long {
    return withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        var duration = 0L
        
        try {
            retriever.setDataSource(context, this@getAudioDuration)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.let {
                duration = it.toLong()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            retriever.release()
        }
        
        duration
    }
}

suspend fun Uri.copyToAppStorage(context: Context): String? {
    return withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(this@copyToAppStorage)
            val fileName = "${UUID.randomUUID()}.mp3"
            val file = File(context.filesDir, fileName)
            
            FileOutputStream(file).use { outputStream ->
                inputStream?.copyTo(outputStream)
            }
            
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

private fun saveAlbumArtToFile(context: Context, albumArt: ByteArray): File {
    val fileName = "artwork_${UUID.randomUUID()}.jpg"
    val file = File(context.filesDir, fileName)
    
    FileOutputStream(file).use { outputStream ->
        outputStream.write(albumArt)
    }
    
    return file
}