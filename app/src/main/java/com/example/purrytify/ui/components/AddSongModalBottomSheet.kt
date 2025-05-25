package com.example.purrytify.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.purrytify.R
import com.example.purrytify.ui.theme.*
import com.example.purrytify.util.AudioUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSongModalBottomSheet(
    isVisible: Boolean,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onSave: (audioUri: Uri, title: String, artist: String, artworkUri: Uri?) -> Unit
) {
    if (isVisible) {
        var title by remember { mutableStateOf("") }
        var artist by remember { mutableStateOf("") }
        var audioUri by remember { mutableStateOf<Uri?>(null) }
        var artworkUri by remember { mutableStateOf<Uri?>(null) }
        var duration by remember { mutableStateOf<Long?>(null) }
        
        var titleError by remember { mutableStateOf<String?>(null) }
        var artistError by remember { mutableStateOf<String?>(null) }
        var audioError by remember { mutableStateOf<String?>(null) }
        
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        
        // Audio file picker
        val audioFilePicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                audioUri = it
                audioError = null
                
                // Extract metadata from audio file
                scope.launch {
                    val metadata = AudioUtils.getAudioMetadata(context, it)
                    
                    // Only update title and artist if they are empty or were auto-filled previously
                    if (title.isEmpty()) {
                        metadata["title"]?.let { newTitle -> title = newTitle }
                    }
                    
                    if (artist.isEmpty()) {
                        metadata["artist"]?.let { newArtist -> artist = newArtist }
                    }
                    
                    // Update duration
                    duration = metadata["duration"]?.toLongOrNull()
                    
                    // Extract album art if available
                    metadata["albumArt"]?.let { artUri ->
                        // Only set artwork if not already set by user
                        if (artworkUri == null) {
                            artworkUri = Uri.parse(artUri)
                        }
                    }
                }
            }
        }
        
        // Image picker for artwork
        val artworkPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                artworkUri = it
            }
        }
        
        val sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )
        
        fun validateInputs(): Boolean {
            var isValid = true
            
            if (title.isBlank()) {
                titleError = "Title cannot be empty"
                isValid = false
            } else {
                titleError = null
            }
            
            if (artist.isBlank()) {
                artistError = "Artist cannot be empty"
                isValid = false
            } else {
                artistError = null
            }
            
            if (audioUri == null) {
                audioError = "Please select an audio file"
                isValid = false
            } else {
                audioError = null
            }
            
            return isValid
        }
        
        fun handleSave() {
            if (validateInputs() && audioUri != null) {
                onSave(audioUri!!, title, artist, artworkUri)
            }
        }
        
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = PurrytifyLighterBlack,
            dragHandle = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(PurrytifyWhite.copy(alpha = 0.6f))
                    )
                }
            }
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    horizontal = 24.dp,
                    vertical = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(
                    if (isLandscape) 12.dp else 16.dp
                ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                item {
                    Text(
                        text = stringResource(R.string.upload_song),
                        style = MaterialTheme.typography.titleLarge,
                        color = PurrytifyWhite,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Error message from API
                if (!errorMessage.isNullOrEmpty()) {
                    item {
                        Text(
                            text = errorMessage,
                            color = PurritifyRed,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                // Upload sections
                item {
                    val uploadSectionModifier = if (isLandscape) {
                        Modifier.fillMaxWidth()
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    }
                    
                    Row(
                        modifier = uploadSectionModifier,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Artwork upload
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val uploadBoxSize = if (isLandscape) 100.dp else 120.dp
                            
                            Box(
                                modifier = Modifier
                                    .size(uploadBoxSize)
                                    .border(
                                        width = 1.dp,
                                        color = PurrytifyDarkGray,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PurrytifyBlack)
                                    .clickable { artworkPicker.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                if (artworkUri != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(artworkUri)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = stringResource(R.string.song_artwork),
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Image,
                                            contentDescription = stringResource(R.string.upload_photo),
                                            tint = PurrytifyLightGray,
                                            modifier = Modifier.size(if (isLandscape) 28.dp else 36.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = stringResource(R.string.upload_photo),
                                            color = PurrytifyLightGray,
                                            style = if (isLandscape) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = stringResource(R.string.cover_art),
                                color = PurrytifyLightGray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        
                        // Audio file upload
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val uploadBoxSize = if (isLandscape) 100.dp else 120.dp
                            
                            Box(
                                modifier = Modifier
                                    .size(uploadBoxSize)
                                    .border(
                                        width = 1.dp,
                                        color = if (audioError != null) PurritifyRed else PurrytifyDarkGray,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PurrytifyBlack)
                                    .clickable { audioFilePicker.launch("audio/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AudioFile,
                                        contentDescription = stringResource(R.string.upload_file),
                                        tint = if (audioUri != null) PurrytifyGreen else PurrytifyLightGray,
                                        modifier = Modifier.size(if (isLandscape) 28.dp else 36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (audioUri != null) {
                                            duration?.let { 
                                                val minutes = it / 1000 / 60
                                                val seconds = (it / 1000) % 60
                                                String.format("%02d:%02d", minutes, seconds)
                                            } ?: stringResource(R.string.selected)
                                        } else {
                                            stringResource(R.string.upload_file)
                                        },
                                        color = if (audioUri != null) PurrytifyGreen else PurrytifyLightGray,
                                        style = if (isLandscape) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            if (audioError != null) {
                                Text(
                                    text = audioError!!,
                                    color = PurritifyRed,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.audio_file),
                                    color = PurrytifyLightGray,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
                
                // Title field
                item {
                    Column {
                        Text(
                            text = stringResource(R.string.title),
                            style = MaterialTheme.typography.bodyMedium,
                            color = PurrytifyWhite,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = title,
                            onValueChange = { 
                                title = it
                                if (titleError != null) titleError = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = PurrytifyBlack,
                                focusedContainerColor = PurrytifyBlack,
                                errorContainerColor = PurrytifyBlack,
                                unfocusedBorderColor = PurrytifyDarkGray,
                                focusedBorderColor = PurrytifyGreen,
                                errorBorderColor = PurritifyRed,
                                unfocusedTextColor = PurrytifyWhite,
                                focusedTextColor = PurrytifyWhite,
                                errorTextColor = PurrytifyWhite,
                                cursorColor = PurrytifyWhite
                            ),
                            placeholder = { Text(stringResource(R.string.title), color = PurrytifyLightGray) },
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            isError = titleError != null
                        )
                        
                        // Title error
                        if (titleError != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = titleError!!,
                                color = PurritifyRed,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }

                // Artist field
                item {
                    Column {
                        Text(
                            text = stringResource(R.string.artist),
                            style = MaterialTheme.typography.bodyMedium,
                            color = PurrytifyWhite,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = artist,
                            onValueChange = { 
                                artist = it
                                if (artistError != null) artistError = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = PurrytifyBlack,
                                focusedContainerColor = PurrytifyBlack,
                                errorContainerColor = PurrytifyBlack,
                                unfocusedBorderColor = PurrytifyDarkGray,
                                focusedBorderColor = PurrytifyGreen,
                                errorBorderColor = PurritifyRed,
                                unfocusedTextColor = PurrytifyWhite,
                                focusedTextColor = PurrytifyWhite,
                                errorTextColor = PurrytifyWhite,
                                cursorColor = PurrytifyWhite
                            ),
                            placeholder = { Text(stringResource(R.string.artist), color = PurrytifyLightGray) },
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            isError = artistError != null
                        )
                        
                        // Artist error
                        if (artistError != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = artistError!!,
                                color = PurritifyRed,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
                
                // Save button
                item {
                    Button(
                        onClick = { handleSave() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PurrytifyGreen,
                            contentColor = PurrytifyWhite,
                            disabledContainerColor = PurrytifyGreen.copy(alpha = 0.5f)
                        ),
                        enabled = !isLoading
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = PurrytifyWhite.copy(alpha = 0.7f),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Text(
                                text = stringResource(R.string.save),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Cancel button
                item {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PurrytifyDarkGray,
                            contentColor = PurrytifyWhite
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}