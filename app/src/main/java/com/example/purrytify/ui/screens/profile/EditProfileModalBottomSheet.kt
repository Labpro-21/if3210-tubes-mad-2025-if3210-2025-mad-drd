package com.example.purrytify.ui.components

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.purrytify.domain.model.Profile
import com.example.purrytify.ui.theme.PurritifyRed
import com.example.purrytify.ui.theme.PurrytifyBlack
import com.example.purrytify.ui.theme.PurrytifyDarkGray
import com.example.purrytify.ui.theme.PurrytifyGreen
import com.example.purrytify.ui.theme.PurrytifyLighterBlack
import com.example.purrytify.ui.theme.PurrytifyWhite
import com.example.purrytify.ui.theme.Typography
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
        var username by remember { mutableStateOf(profile.username) }
        var email by remember { mutableStateOf(profile.email) }
        var location by remember { mutableStateOf(profile.location) }
        
        var usernameError by remember { mutableStateOf<String?>(null) }
        var emailError by remember { mutableStateOf<String?>(null) }
        
        val sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )
        val scope = rememberCoroutineScope()
        
        fun validateUsername(): Boolean {
            return if (username.isBlank()) {
                usernameError = "Username cannot be empty"
                false
            } else {
                usernameError = null
                true
            }
        }
        
        fun validateEmail(): Boolean {
            return if (email.isBlank()) {
                emailError = "Email cannot be empty"
                false
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailError = "Invalid email format"
                false
            } else {
                emailError = null
                true
            }
        }
        
        fun validateAndSave() {
            val isUsernameValid = validateUsername()
            val isEmailValid = validateEmail()
            
            if (isUsernameValid && isEmailValid) {
                onSave(username, email, location)
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
                
                // Username field
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
                    value = username,
                    onValueChange = { 
                        username = it
                        if (usernameError != null) validateUsername()
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
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    isError = usernameError != null
                )
                
                // Username error
                usernameError?.let { error ->
                    Text(
                        text = error,
                        color = PurritifyRed,
                        style = Typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Email field
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
                    value = email,
                    onValueChange = { 
                        email = it
                        if (emailError != null) validateEmail()
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
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    isError = emailError != null
                )
                
                // Email error
                emailError?.let { error ->
                    Text(
                        text = error,
                        color = PurritifyRed,
                        style = Typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Location field (optional)
                Text(
                    text = "Location (optional)",
                    style = Typography.bodyMedium,
                    color = PurrytifyWhite,
                    modifier = Modifier
                        .fillMaxWidth(),
                    textAlign = TextAlign.Start
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = PurrytifyBlack,
                        focusedContainerColor = PurrytifyBlack,
                        unfocusedBorderColor = PurrytifyDarkGray,
                        focusedBorderColor = PurrytifyGreen,
                        unfocusedTextColor = PurrytifyWhite,
                        focusedTextColor = PurrytifyWhite,
                        cursorColor = PurrytifyWhite
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(48.dp))
                
                // Save button
                Button(
                    onClick = { validateAndSave() },
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