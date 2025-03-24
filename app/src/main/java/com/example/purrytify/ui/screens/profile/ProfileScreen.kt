package com.example.purrytify.ui.screens.profile

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.purrytify.R
import com.example.purrytify.ui.components.EditProfileModalBottomSheet
import com.example.purrytify.ui.components.LogoutModalBottomSheet
import com.example.purrytify.ui.components.NoInternetScreen
import com.example.purrytify.ui.theme.*
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToLogin: () -> Unit,
    isNetworkAvailable: Boolean,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    // If no network
    if (!isNetworkAvailable) {
        NoInternetScreen()
        return
    }

    val uiState by viewModel.uiState.collectAsState()
    val showLogoutDialog by viewModel.showLogoutDialog.collectAsState()
    val showEditDialog by viewModel.showEditDialog.collectAsState()
    val isUpdateLoading by viewModel.isUpdateLoading.collectAsState()
    val updateError by viewModel.updateError.collectAsState()
    val context = LocalContext.current

    // Image picker launcher
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.updateProfilePicture(uriToFile(context, it))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PurrytifyBlack)
    ) {
        // Main content
        when (val state = uiState) {
            is ProfileUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = PurrytifyGreen,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            is ProfileUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Error: ${state.message}",
                        color = PurrytifyWhite,
                        style = Typography.bodyLarge
                    )
                }
            }
            is ProfileUiState.Success -> {
                val profile = state.profile
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = 24.dp,
                            vertical = 16.dp
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Profile picture
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .clickable { imagePicker.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(profile.profilePhotoUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Profile Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            error = painterResource(id = R.drawable.ic_launcher_foreground)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Username
                    Text(
                        text = profile.username,
                        style = Typography.titleLarge,
                        color = PurrytifyWhite,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Location
                    Text(
                        text = profile.location,
                        style = Typography.bodyMedium,
                        color = PurrytifyLightGray
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Edit and Logout buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Edit button
                        Button(
                            onClick = { viewModel.onEditProfileClick() },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PurrytifyDarkGray,
                                contentColor = PurrytifyWhite
                            )
                        ) {
                            Text(
                                text = "Edit",
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        // Logout button
                        Button(
                            onClick = { viewModel.onLogoutClick() },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Red,
                                contentColor = PurrytifyWhite
                            )
                        ) {
                            Text(
                                text = "Logout",
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Songs count
                        StatItem(
                            count = viewModel.songsCount,
                            label = "SONGS"
                        )
                        
                        // Liked count
                        StatItem(
                            count = viewModel.likedSongsCount,
                            label = "LIKED"
                        )
                        
                        // Listened count
                        StatItem(
                            count = viewModel.listenedSongsCount,
                            label = "LISTENED"
                        )
                    }
                }
                
                // Logout modal bottom sheet
                LogoutModalBottomSheet(
                    isVisible = showLogoutDialog,
                    onDismiss = { viewModel.dismissLogoutDialog() },
                    onConfirm = {
                        viewModel.logout()
                        onNavigateToLogin()
                    }
                )
                
                // Edit profile modal bottom sheet
                if (showEditDialog) {
                    EditProfileModalBottomSheet(
                        isVisible = true,
                        profile = profile,
                        isLoading = isUpdateLoading,
                        errorMessage = updateError,
                        onDismiss = { viewModel.dismissEditDialog() },
                        onSave = { username, email, location ->
                            viewModel.updateProfile(username, email, location)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StatItem(count: Int, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            style = Typography.titleLarge,
            color = PurrytifyWhite,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = label,
            style = Typography.bodySmall,
            color = PurrytifyLightGray
        )
    }
}

// Helper function to convert Uri to File
private fun uriToFile(context: Context, uri: Uri): File {
    val inputStream = context.contentResolver.openInputStream(uri)
    val tempFile = File.createTempFile("profile", ".jpg", context.cacheDir)
    
    inputStream?.use { input ->
        FileOutputStream(tempFile).use { output ->
            input.copyTo(output)
        }
    }
    
    return tempFile
}