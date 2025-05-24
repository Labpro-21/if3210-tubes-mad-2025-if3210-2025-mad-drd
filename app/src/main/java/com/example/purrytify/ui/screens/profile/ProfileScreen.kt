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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.purrytify.ui.components.LoadingView
import com.example.purrytify.ui.components.LogoutModalBottomSheet
import com.example.purrytify.ui.components.NoInternetScreen
import com.example.purrytify.ui.theme.*
import com.example.purrytify.util.CountryUtils
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToLogin: () -> Unit,
    isNetworkAvailable: Boolean,
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateToTimeListened: () -> Unit,
    onNavigateToTopArtists: () -> Unit,
    onNavigateToTopSongs: () -> Unit,
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
    
    val songsCount by viewModel.songsCount.collectAsState()
    val likedSongsCount by viewModel.likedSongsCount.collectAsState()
    val listenedSongsCount by viewModel.listenedSongsCount.collectAsState()

    val currentMonthAnalytics by viewModel.currentMonthAnalytics.collectAsState()
    val analyticsLoading by viewModel.analyticsLoading.collectAsState()
    
    val context = LocalContext.current

    // Image picker launcher
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val file = uriToFile(context, it)
            viewModel.updateProfilePicture(file)
        }
    }
    
    // Refresh profile when screen is shown
    LaunchedEffect(isNetworkAvailable) {
        viewModel.loadProfile()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PurrytifyBlack)
    ) {
        // Main content
        when (val state = uiState) {
            is ProfileUiState.Loading -> {
                LoadingView()
            }
            is ProfileUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Error",
                            color = PurrytifyWhite,
                            style = Typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = state.message,
                            color = PurrytifyLightGray,
                            style = Typography.bodyLarge
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = { viewModel.loadProfile() },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PurrytifyGreen,
                                contentColor = PurrytifyWhite
                            )
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
            is ProfileUiState.Success -> {
                val profile = state.profile
                
                // Get country information
                val countryCode = profile.location
                val countryName = CountryUtils.getCountryNameFromCode(countryCode) ?: countryCode
                val flagEmoji = CountryUtils.getFlagEmoji(countryCode)
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
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
                    
                    // Location with flag and country name
                    Card(
                        modifier = Modifier
                            .padding(top = 8.dp, bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = PurrytifyLighterBlack
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // Flag emoji
                            Text(
                                text = flagEmoji,
                                style = Typography.titleMedium
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            // Country name and code
                            Text(
                                text = "$countryName ($countryCode)",
                                style = Typography.bodyMedium,
                                color = PurrytifyWhite
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Edit and Logout buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
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
                                text = "Edit Profile",
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
                            count = songsCount,
                            label = "SONGS"
                        )
                        
                        // Liked count
                        StatItem(
                            count = likedSongsCount,
                            label = "LIKED"
                        )
                        
                        // Listened count
                        StatItem(
                            count = listenedSongsCount,
                            label = "LISTENED"
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))

                    // Sound Capsule Section
                    com.example.purrytify.ui.components.SoundCapsuleSection(
                        currentMonthAnalytics = currentMonthAnalytics,
                        isLoading = analyticsLoading,
                        onTimeListenedClick = onNavigateToTimeListened,
                        onTopArtistClick = onNavigateToTopArtists,
                        onTopSongClick = onNavigateToTopSongs,
                        onExportClick = { viewModel.exportAnalytics() }
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
                
                // Logout confirmation dialog
                LogoutModalBottomSheet(
                    isVisible = showLogoutDialog,
                    onDismiss = { viewModel.dismissLogoutDialog() },
                    onConfirm = {
                        viewModel.logout()
                        onNavigateToLogin()
                    }
                )
                
                // Edit profile dialog
                if (showEditDialog) {
                    EditProfileModalBottomSheet(
                        isVisible = true,
                        profile = profile,
                        isLoading = isUpdateLoading,
                        errorMessage = updateError,
                        onDismiss = { viewModel.dismissEditDialog() },
                        onSave = { _, _, location ->
                            viewModel.updateLocation(location)
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