package com.example.purrytify.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.purrytify.domain.model.OnlineSong
import com.example.purrytify.ui.theme.*
import com.example.purrytify.util.QRCodeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareSongBottomSheet(
    isVisible: Boolean,
    song: OnlineSong?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var qrCodeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isGeneratingQR by remember { mutableStateOf(false) }
    
    if (isVisible && song != null) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = PurrytifyLighterBlack,
            contentColor = PurrytifyWhite,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(PurrytifyLightGray, RoundedCornerShape(2.dp))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Text(
                    text = "Share Song",
                    style = Typography.titleLarge,
                    color = PurrytifyWhite,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Song info
                Text(
                    text = song.title,
                    style = Typography.bodyLarge,
                    color = PurrytifyWhite,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(
                    text = song.artist,
                    style = Typography.bodyMedium,
                    color = PurrytifyLightGray,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Share via URL button
                Button(
                    onClick = {
                        shareViaURL(context, song)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurrytifyGreen,
                        contentColor = PurrytifyWhite
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Share via URL",
                        style = Typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Share via QR Code button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            generateAndShareQR(context, song) { bitmap ->
                                qrCodeBitmap = bitmap
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurrytifyLighterBlack,
                        contentColor = PurrytifyWhite
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isGeneratingQR) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = PurrytifyWhite,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isGeneratingQR) "Generating QR..." else "Share via QR Code",
                        style = Typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Show QR code if generated
                qrCodeBitmap?.let { bitmap ->
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = PurrytifyWhite
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "QR Code for ${song.title}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f),
                                contentScale = ContentScale.Fit
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Scan to play ${song.title}",
                                style = Typography.bodySmall,
                                color = PurrytifyBlack,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
        
        // Reset QR code when dialog is opened
        LaunchedEffect(isVisible) {
            if (isVisible) {
                qrCodeBitmap = null
                isGeneratingQR = false
            }
        }
    }
}

private fun shareViaURL(context: Context, song: OnlineSong) {
    val deepLink = QRCodeUtils.createSongDeepLink(song.id.toString())
    
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "Listen to \"${song.title}\" by ${song.artist} on Purrytify: $deepLink")
        putExtra(Intent.EXTRA_SUBJECT, "Check out this song on Purrytify!")
    }
    
    val chooser = Intent.createChooser(shareIntent, "Share song")
    context.startActivity(chooser)
}

private suspend fun generateAndShareQR(
    context: Context, 
    song: OnlineSong,
    onQRGenerated: (Bitmap?) -> Unit
) = withContext(Dispatchers.IO) {
    try {
        val deepLink = QRCodeUtils.createSongDeepLink(song.id.toString())
        val qrBitmap = QRCodeUtils.generateQRCode(deepLink, 512, 512)
        
        withContext(Dispatchers.Main) {
            onQRGenerated(qrBitmap)
        }
        
        qrBitmap?.let { bitmap ->
            // Save QR code to cache directory
            val cachePath = File(context.cacheDir, "shared_qr")
            cachePath.mkdirs()
            
            val qrFile = File(cachePath, "song_${song.id}_qr.png")
            val stream = FileOutputStream(qrFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()
            
            // Create file URI for sharing
            val qrUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                qrFile
            )
            
            // Share QR code image
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, qrUri)
                putExtra(Intent.EXTRA_TEXT, "Scan this QR code to listen to \"${song.title}\" by ${song.artist} on Purrytify!")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            withContext(Dispatchers.Main) {
                val chooser = Intent.createChooser(shareIntent, "Share QR Code")
                context.startActivity(chooser)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        withContext(Dispatchers.Main) {
            onQRGenerated(null)
        }
    }
}