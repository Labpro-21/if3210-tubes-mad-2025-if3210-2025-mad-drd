package com.example.purrytify.ui.screens.auth

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.purrytify.R
import com.example.purrytify.ui.components.NoInternetScreen
import com.example.purrytify.ui.theme.*

/**
 * Login screen UI with Poppins typography
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    isNetworkAvailable: Boolean,
    viewModel: LoginViewModel = hiltViewModel()
) {
    // If no network
    if (!isNetworkAvailable) {
        NoInternetScreen()
        return
    }

    val loginUiState by viewModel.loginUiState.collectAsState()
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val emailError by viewModel.emailError.collectAsState()
    val passwordError by viewModel.passwordError.collectAsState()

    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(loginUiState) {
        if (loginUiState is LoginUiState.Success) {
            onLoginSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PurrytifyBlack)
    ) {
        if (isLandscape) {
            // Landscape layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left section with image and title
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    // Logo
                    Image(
                        painter = painterResource(id = R.drawable.logo_3),
                        contentDescription = "Purrytify Logo",
                        modifier = Modifier.size(92.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // App Title
                    Text(
                        text = "Millions of Songs.",
                        style = MaterialTheme.typography.headlineMedium,
                        color = PurrytifyWhite,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Only on Purrytify.",
                        style = MaterialTheme.typography.headlineMedium,
                        color = PurrytifyWhite,
                        textAlign = TextAlign.Center
                    )
                }

                // Right section with login form
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    // Email Field
                    Text(
                        text = "Email",
                        color = PurrytifyWhite,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { viewModel.updateEmail(it) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = PurrytifyLighterBlack,
                            focusedContainerColor = PurrytifyLighterBlack,
                            errorContainerColor = PurrytifyLighterBlack,
                            unfocusedBorderColor = PurrytifyDarkGray,
                            focusedBorderColor = PurrytifyGreen,
                            unfocusedTextColor = PurrytifyWhite,
                            focusedTextColor = PurrytifyWhite,
                            errorTextColor = PurrytifyWhite,
                            unfocusedPlaceholderColor = PurrytifyLightGray,
                            focusedPlaceholderColor = PurrytifyLightGray,
                            errorPlaceholderColor = PurrytifyLightGray,
                            cursorColor = PurrytifyWhite,
                        ),
                        placeholder = { 
                            Text(
                                text = "Email",
                                style = MaterialTheme.typography.bodyMedium
                            ) 
                        },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(6.dp),
                        isError = emailError != null
                    )

                    // Email Error
                    emailError?.let { error ->
                        Text(
                            text = error,
                            color = PurritifyRed,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Field
                    Text(
                        text = "Password",
                        color = PurrytifyWhite,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { viewModel.updatePassword(it) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = PurrytifyLighterBlack,
                            focusedContainerColor = PurrytifyLighterBlack,
                            errorContainerColor = PurrytifyLighterBlack,
                            unfocusedBorderColor = PurrytifyDarkGray,
                            focusedBorderColor = PurrytifyGreen,
                            unfocusedTextColor = PurrytifyWhite,
                            focusedTextColor = PurrytifyWhite,
                            errorTextColor = PurrytifyWhite,
                            unfocusedPlaceholderColor = PurrytifyLightGray,
                            focusedPlaceholderColor = PurrytifyLightGray,
                            errorPlaceholderColor = PurrytifyLightGray,
                            cursorColor = PurrytifyWhite,
                        ),
                        placeholder = { 
                            Text(
                                text = "Password",
                                style = MaterialTheme.typography.bodyMedium
                            ) 
                        },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                viewModel.login()
                            }
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(6.dp),
                        isError = passwordError != null,
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = PurrytifyLightGray
                                )
                            }
                        }
                    )

                    // Password Error
                    passwordError?.let { error ->
                        Text(
                            text = error,
                            color = PurritifyRed,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, start = 4.dp)
                        )
                    }

                    // Login API Error
                    if (loginUiState is LoginUiState.Error) {
                        Text(
                            text = (loginUiState as LoginUiState.Error).message,
                            color = PurritifyRed,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Login Button
                    Button(
                        onClick = { viewModel.login() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PurrytifyGreen,
                            contentColor = PurrytifyWhite
                        ),
                        enabled = loginUiState !is LoginUiState.Loading
                    ) {
                        if (loginUiState is LoginUiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = PurrytifyWhite,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Log In",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        } else {
            // Portrait layout
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Background image with albums
                Image(
                    painter = painterResource(id = R.drawable.bg_login),
                    contentDescription = "Login Background",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Logo
                Image(
                    painter = painterResource(id = R.drawable.logo_3),
                    contentDescription = "Purrytify Logo",
                    modifier = Modifier.size(92.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // App Title
                    Text(
                        text = "Millions of Songs.",
                        style = MaterialTheme.typography.headlineSmall,
                        color = PurrytifyWhite,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Only on Purrytify.",
                        style = MaterialTheme.typography.headlineSmall,
                        color = PurrytifyWhite,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Email Field
                    Text(
                        text = "Email",
                        color = PurrytifyWhite,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { viewModel.updateEmail(it) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = PurrytifyLighterBlack,
                            focusedContainerColor = PurrytifyLighterBlack,
                            errorContainerColor = PurrytifyLighterBlack,
                            unfocusedBorderColor = PurrytifyDarkGray,
                            focusedBorderColor = PurrytifyGreen,
                            unfocusedTextColor = PurrytifyWhite,
                            focusedTextColor = PurrytifyWhite,
                            errorTextColor = PurrytifyWhite,
                            unfocusedPlaceholderColor = PurrytifyLightGray,
                            focusedPlaceholderColor = PurrytifyLightGray,
                            errorPlaceholderColor = PurrytifyLightGray,
                            cursorColor = PurrytifyWhite,
                        ),
                        placeholder = { 
                            Text(
                                text = "Email",
                                style = MaterialTheme.typography.bodyMedium
                            ) 
                        },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(6.dp),
                        isError = emailError != null
                    )

                    // Email Error
                    emailError?.let { error ->
                        Text(
                            text = error,
                            color = PurritifyRed,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Field
                    Text(
                        text = "Password",
                        color = PurrytifyWhite,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { viewModel.updatePassword(it) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = PurrytifyLighterBlack,
                            focusedContainerColor = PurrytifyLighterBlack,
                            errorContainerColor = PurrytifyLighterBlack,
                            unfocusedBorderColor = PurrytifyDarkGray,
                            focusedBorderColor = PurrytifyGreen,
                            unfocusedTextColor = PurrytifyWhite,
                            focusedTextColor = PurrytifyWhite,
                            errorTextColor = PurrytifyWhite,
                            unfocusedPlaceholderColor = PurrytifyLightGray,
                            focusedPlaceholderColor = PurrytifyLightGray,
                            errorPlaceholderColor = PurrytifyLightGray,
                            cursorColor = PurrytifyWhite,
                        ),
                        placeholder = { 
                            Text(
                                text = "Password",
                                style = MaterialTheme.typography.bodyMedium
                            ) 
                        },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                viewModel.login()
                            }
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(6.dp),
                        isError = passwordError != null,
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = PurrytifyLightGray
                                )
                            }
                        }
                    )

                    // Password Error
                    passwordError?.let { error ->
                        Text(
                            text = error,
                            color = PurritifyRed,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, start = 4.dp)
                        )
                    }

                    // Login API Error
                    if (loginUiState is LoginUiState.Error) {
                        Text(
                            text = (loginUiState as LoginUiState.Error).message,
                            color = PurritifyRed,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Login Button
                    Button(
                        onClick = { viewModel.login() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PurrytifyGreen,
                            contentColor = PurrytifyWhite,
                            disabledContainerColor = PurrytifyGreen.copy(alpha = 0.5f),
                            disabledContentColor = PurrytifyWhite.copy(alpha = 0.7f)
                        ),
                        enabled = loginUiState !is LoginUiState.Loading
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (loginUiState is LoginUiState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = PurrytifyWhite.copy(alpha = 0.7f),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Text(
                                text = "Log In",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }
}