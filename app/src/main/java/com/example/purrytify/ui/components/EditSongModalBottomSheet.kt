package com.example.purrytify.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.purrytify.domain.model.Song
import com.example.purrytify.ui.theme.*
import com.example.purrytify.util.AudioUtils.getAudioDuration
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSongModalBottomSheet(
    isVisible: Boolean,
    song: Song,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onSave: (title: String, artist: String, audioUri: Uri?, artworkUri: Uri?) -> Unit
) {
    if (isVisible) {
        var title by remember { mutableStateOf(song.title) }
        var artist by remember { mutableStateOf(song.artist) }
        var audioUri by remember { mutableStateOf<Uri?>(null) }
        var artworkUri by remember { mutableStateOf<Uri?>(null) }
        var duration by remember { mutableStateOf<Long?>(song.duration) }
        
        var titleError by remember { mutableStateOf<String?>(null) }
        var artistError by remember { mutableStateOf<String?>(null) }
        
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        
        // Audio file picker
        val audioFilePicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                audioUri = it
                
                // Extract metadata from audio file
                scope.launch {
                    duration = it.getAudioDuration(context)
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
            
            return isValid
        }
        
        fun handleSave() {
            if (validateInputs()) {
                onSave(title, artist, audioUri, artworkUri)
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Title
                Text(
                    text = "Edit Song",
                    style = Typography.titleLarge,
                    color = PurrytifyWhite,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Error message from API
                if (!errorMessage.isNullOrEmpty()) {
                    Text(
                        text = errorMessage,
                        color = PurritifyRed,
                        style = Typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )
                }
                
                // Upload sections
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Artwork upload
                    Column(
                        modifier = Modifier
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
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
                                // New artwork selected
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(artworkUri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Song Artwork",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else if (song.artworkUri != null) {
                                // Show existing artwork
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(song.artworkUri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Song Artwork",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                // No artwork
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = "Upload Artwork",
                                        tint = PurrytifyLightGray,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Upload Photo",
                                        color = PurrytifyLightGray,
                                        style = Typography.bodySmall
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "Cover Art (Optional)",
                            color = PurrytifyLightGray,
                            style = Typography.bodySmall
                        )
                    }
                    
                    // Audio file upload
                    Column(
                        modifier = Modifier
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .border(
                                    width = 1.dp,
                                    color = PurrytifyDarkGray,
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
                                    contentDescription = "Upload Audio",
                                    tint = if (audioUri != null) PurrytifyGreen else PurrytifyLightGray,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Show text based on new audio or existing
                                val durationText = if (audioUri != null) {
                                    duration?.let { 
                                        val minutes = it / 1000 / 60
                                        val seconds = (it / 1000) % 60
                                        String.format("%02d:%02d", minutes, seconds)
                                    } ?: "Selected"
                                } else {
                                    song.formattedDuration
                                }
                                
                                Text(
                                    text = durationText,
                                    color = if (audioUri != null) PurrytifyGreen else PurrytifyLightGray,
                                    style = Typography.bodySmall
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                            
                        Text(
                            text = "Audio File (Optional)",
                            color = PurrytifyLightGray,
                            style = Typography.bodySmall
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Title field
                Text(
                    text = "Title",
                    style = Typography.bodyMedium,
                    color = PurrytifyWhite,
                    modifier = Modifier
                        .fillMaxWidth(),
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
                    placeholder = { Text("Title", color = PurrytifyLightGray) },
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    isError = titleError != null
                )
                
                // Title error
                if (titleError != null) {
                    Text(
                        text = titleError!!,
                        color = PurritifyRed,
                        style = Typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Artist field
                Text(
                    text = "Artist",
                    style = Typography.bodyMedium,
                    color = PurrytifyWhite,
                    modifier = Modifier
                        .fillMaxWidth(),
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
                    placeholder = { Text("Artist", color = PurrytifyLightGray) },
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    isError = artistError != null
                )
                
                // Artist error
                if (artistError != null) {
                    Text(
                        text = artistError!!,
                        color = PurritifyRed,
                        style = Typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, start = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(48.dp))
                
                // Save button
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
                            text = "Save",
                            style = Typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Cancel button
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
                        text = "Cancel",
                        style = Typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}