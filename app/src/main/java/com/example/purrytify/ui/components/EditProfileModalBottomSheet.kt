package com.example.purrytify.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.purrytify.domain.model.Profile
import com.example.purrytify.ui.theme.PurritifyRed
import com.example.purrytify.ui.theme.PurrytifyBlack
import com.example.purrytify.ui.theme.PurrytifyDarkGray
import com.example.purrytify.ui.theme.PurrytifyGreen
import com.example.purrytify.ui.theme.PurrytifyLightGray
import com.example.purrytify.ui.theme.PurrytifyLighterBlack
import com.example.purrytify.ui.theme.PurrytifyWhite
import com.example.purrytify.ui.theme.Typography
import com.example.purrytify.util.LocationUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileModalBottomSheet(
    isVisible: Boolean,
    profile: Profile,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onSave: (username: String, email: String, location: String) -> Unit
) {
    if (isVisible) {
        val context = LocalContext.current
        var location by remember { mutableStateOf(profile.location) }
        
        val sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )
        val scope = rememberCoroutineScope()
        
        // Location permission launcher
        val requestLocationPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                // Get current location if permissions granted
                LocationUtils.getLastKnownLocation(context)?.let { userLocation ->
                    val countryCode = LocationUtils.getCountryCodeFromLocation(context, userLocation)
                    location = countryCode
                }
            }
        }
        
        // Maps intent launcher
        val mapPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.let { data ->
                    data.getStringExtra("selected_location")?.let { selectedLocation ->
                        location = selectedLocation
                    }
                }
            }
        }
        
        fun getCurrentLocation() {
            if (!LocationUtils.hasLocationPermission(context)) {
                // Request location permissions
                requestLocationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            } else if (!LocationUtils.isLocationEnabled(context)) {
                // Open location settings if location is disabled
                LocationUtils.openLocationSettings(context)
            } else {
                // Get current location
                LocationUtils.getLastKnownLocation(context)?.let { userLocation ->
                    val countryCode = LocationUtils.getCountryCodeFromLocation(context, userLocation)
                    location = countryCode
                }
            }
        }
        
        fun openMapPicker() {
            try {
                // Create an Intent to view the map
                val gmmIntentUri = Uri.parse("geo:0,0?q=")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                
                // Specify that we want to use Google Maps
                mapIntent.setPackage("com.google.android.apps.maps")
                
                if (mapIntent.resolveActivity(context.packageManager) != null) {
                    // Google Maps is installed
                    Toast.makeText(
                        context,
                        "Please select a location and use the back button",
                        Toast.LENGTH_SHORT
                    ).show()
                    mapPickerLauncher.launch(mapIntent)
                    
                    // When user returns, we'll just use the last known location as the country code
                    // will be determined from the location the user navigated to in Maps
                    Handler(Looper.getMainLooper()).postDelayed({
                        LocationUtils.getLastKnownLocation(context)?.let { userLocation ->
                            val countryCode = LocationUtils.getCountryCodeFromLocation(context, userLocation)
                            location = countryCode
                        }
                    }, 500) // Small delay to ensure the location is updated
                } else {
                    // Google Maps not installed
                    Toast.makeText(
                        context, 
                        "Google Maps is not installed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Error opening maps: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
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
                    text = "Edit Profile",
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
                
                // Username (read-only)
                Text(
                    text = "Username",
                    style = Typography.bodyMedium,
                    color = PurrytifyWhite,
                    modifier = Modifier
                        .fillMaxWidth(),
                    textAlign = TextAlign.Start
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = profile.username,
                    onValueChange = { /* Read-only */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = PurrytifyBlack,
                        focusedContainerColor = PurrytifyBlack,
                        unfocusedBorderColor = PurrytifyDarkGray,
                        focusedBorderColor = PurrytifyDarkGray,
                        unfocusedTextColor = PurrytifyLightGray,
                        focusedTextColor = PurrytifyLightGray,
                        cursorColor = PurrytifyWhite
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    readOnly = true // Make it read-only
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Email (read-only)
                Text(
                    text = "Email",
                    style = Typography.bodyMedium,
                    color = PurrytifyWhite,
                    modifier = Modifier
                        .fillMaxWidth(),
                    textAlign = TextAlign.Start
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = profile.email,
                    onValueChange = { /* Read-only */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = PurrytifyBlack,
                        focusedContainerColor = PurrytifyBlack,
                        unfocusedBorderColor = PurrytifyDarkGray,
                        focusedBorderColor = PurrytifyDarkGray,
                        unfocusedTextColor = PurrytifyLightGray,
                        focusedTextColor = PurrytifyLightGray,
                        cursorColor = PurrytifyWhite
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    readOnly = true // Make it read-only
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Location field
                Text(
                    text = "Location (Country Code)",
                    style = Typography.bodyMedium,
                    color = PurrytifyWhite,
                    modifier = Modifier
                        .fillMaxWidth(),
                    textAlign = TextAlign.Start
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = location,
                    onValueChange = { /* Read-only */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = PurrytifyBlack,
                        focusedContainerColor = PurrytifyBlack,
                        unfocusedBorderColor = PurrytifyDarkGray,
                        focusedBorderColor = PurrytifyDarkGray,
                        unfocusedTextColor = PurrytifyLightGray,
                        focusedTextColor = PurrytifyLightGray,
                        cursorColor = PurrytifyWhite
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    readOnly = true // Make it read-only
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Help text
                Text(
                    text = "Select your location using one of the methods below:",
                    style = Typography.bodySmall,
                    color = PurrytifyLightGray,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Location buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Use current location button
                    Button(
                        onClick = { getCurrentLocation() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PurrytifyDarkGray,
                            contentColor = PurrytifyWhite
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Use current location",
                            tint = PurrytifyWhite
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = "Current",
                            style = Typography.bodyMedium
                        )
                    }
                    
                    // Pick from map button
                    Button(
                        onClick = { openMapPicker() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PurrytifyDarkGray,
                            contentColor = PurrytifyWhite
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Choose location on map",
                            tint = PurrytifyWhite
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = "Map",
                            style = Typography.bodyMedium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Save button
                Button(
                    onClick = { 
                        onSave(profile.username, profile.email, location) 
                    },
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
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                            onDismiss()
                        }
                    },
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