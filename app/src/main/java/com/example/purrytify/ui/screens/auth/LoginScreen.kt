package com.example.purrytify.ui.screens.auth

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.purrytify.ui.theme.*

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val loginUiState by viewModel.loginUiState.collectAsState()
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val emailError by viewModel.emailError.collectAsState()
    val passwordError by viewModel.passwordError.collectAsState()

    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

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
                        style = Typography.titleLarge,
                        color = PurrytifyWhite,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Only on Purrytify.",
                        style = Typography.titleLarge,
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
                        style = Typography.bodyMedium,
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
                        placeholder = { Text("Email") },
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
                            style = Typography.bodySmall,
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
                        style = Typography.bodyMedium,
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
                        placeholder = { Text("Password") },
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
                            style = Typography.bodySmall,
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
                            style = Typography.bodySmall,
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
                                style = Typography.titleMedium
                            )
                        }
                    }
                }
            }
        } else {
            // Portrait layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(0.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Background image
                Image(
                    painter = painterResource(id = R.drawable.bg_login),
                    contentDescription = "Login Background",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                    ,
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
                        style = Typography.titleLarge,
                        color = PurrytifyWhite,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Only on Purrytify.",
                        style = Typography.titleLarge,
                        color = PurrytifyWhite,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Email Field
                    Text(
                        text = "Email",
                        color = PurrytifyWhite,
                        style = Typography.bodyMedium,
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
                        placeholder = { Text("Email") },
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
                            style = Typography.bodySmall,
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
                        style = Typography.bodyMedium,
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
                        placeholder = { Text("Password") },
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
                            style = Typography.bodySmall,
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
                            style = Typography.bodySmall,
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
                                style = Typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }
}