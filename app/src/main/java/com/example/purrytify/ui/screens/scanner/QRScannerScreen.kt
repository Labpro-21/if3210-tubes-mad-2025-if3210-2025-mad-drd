package com.example.purrytify.ui.screens.scanner

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.purrytify.ui.components.LoadingView
import com.example.purrytify.ui.theme.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CompoundBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory

/**
 * QR Scanner screen with camera scanning and manual input options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRScannerScreen(
    onBackPressed: () -> Unit,
    onNavigateToPlayer: (String) -> Unit,
    viewModel: QRScannerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // State collection
    val uiState by viewModel.uiState.collectAsState()
    val hasCameraPermission by viewModel.hasCameraPermission.collectAsState()
    val manualInputText by viewModel.manualInputText.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val navigateToPlayer by viewModel.navigateToPlayer.collectAsState()

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.updateCameraPermission(isGranted)
    }

    // Handle navigation
    LaunchedEffect(navigateToPlayer) {
        navigateToPlayer?.let { songId ->
            onNavigateToPlayer(songId)
            viewModel.clearNavigation()
        }
    }

    // Show error messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            // Error message will be displayed in the UI
        }
    }

    // Request camera permission on first load if not granted
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan QR Code") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = PurrytifyWhite
                        )
                    }
                },
                actions = {
                    // Toggle between camera and manual input
                    IconButton(
                        onClick = {
                            when (uiState) {
                                is QRScannerUiState.Scanning -> viewModel.switchToManualInput()
                                is QRScannerUiState.ManualInput -> viewModel.switchToScanning()
                                else -> {}
                            }
                        }
                    ) {
                        Icon(
                            imageVector = when (uiState) {
                                is QRScannerUiState.Scanning -> Icons.Default.Edit
                                is QRScannerUiState.ManualInput -> Icons.Default.CameraAlt
                                else -> Icons.Default.QrCode
                            },
                            contentDescription = when (uiState) {
                                is QRScannerUiState.Scanning -> "Manual input"
                                is QRScannerUiState.ManualInput -> "Camera scan"
                                else -> "QR Scanner"
                            },
                            tint = PurrytifyWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PurrytifyBlack,
                    titleContentColor = PurrytifyWhite
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PurrytifyBlack)
                .padding(paddingValues)
        ) {
            when (uiState) {
                is QRScannerUiState.Scanning -> {
                    if (hasCameraPermission) {
                        CameraScannerView(
                            isLandscape = isLandscape,
                            onQRCodeScanned = { code ->
                                viewModel.processScannedCode(code)
                            }
                        )
                    } else {
                        CameraPermissionContent(
                            isLandscape = isLandscape,
                            onRequestPermission = {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        )
                    }
                }

                is QRScannerUiState.ManualInput -> {
                    ManualInputContent(
                        inputText = manualInputText,
                        onInputTextChanged = viewModel::updateManualInputText,
                        onSubmit = {
                            keyboardController?.hide()
                            viewModel.processManualInput()
                        }
                    )
                }

                is QRScannerUiState.Processing -> {
                    LoadingView()
                }

                is QRScannerUiState.Success -> {
                    SuccessContent(isLandscape = isLandscape)
                }
            }

            // Error message overlay
            errorMessage?.let { message ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = PurritifyRed.copy(alpha = 0.9f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = message,
                            style = Typography.bodyMedium,
                            color = PurrytifyWhite,
                            modifier = Modifier.weight(1f)
                        )

                        TextButton(
                            onClick = { viewModel.clearError() }
                        ) {
                            Text(
                                text = "Dismiss",
                                color = PurrytifyWhite
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Camera scanner view using ZXing library - now responsive
 */
@Composable
private fun CameraScannerView(
    isLandscape: Boolean,
    onQRCodeScanned: (String) -> Unit
) {
    var isScanning by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AndroidView(
            factory = { context ->
                CompoundBarcodeView(context).apply {
                    val formats = listOf(BarcodeFormat.QR_CODE)
                    decoderFactory = DefaultDecoderFactory(formats)

                    val callback = object : BarcodeCallback {
                        override fun barcodeResult(result: BarcodeResult?) {
                            if (isScanning && result != null) {
                                isScanning = false
                                onQRCodeScanned(result.text)

                                // Re-enable scanning after a delay to prevent multiple scans
                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                    isScanning = true
                                }, 2000)
                            }
                        }

                        override fun possibleResultPoints(resultPoints: List<ResultPoint>?) {
                            // Handle possible result points if needed
                        }
                    }

                    decodeContinuous(callback)
                    resume()
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                if (isScanning) {
                    view.resume()
                } else {
                    view.pause()
                }
            }
        )

        // Responsive scanning overlay
        if (isLandscape) {
            // Landscape: Position overlay on the right side
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                // Camera area takes 2/3 of the width
                Spacer(modifier = Modifier.weight(2f))

                // Overlay area takes 1/3 of the width
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ScanningInstructionCard(
                        isLandscape = true
                    )
                }
            }
        } else {
            // Portrait: Position overlay at the bottom
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ScanningInstructionCard(
                    isLandscape = false
                )
            }
        }
    }
}

/**
 * Scanning instruction card - responsive component
 */
@Composable
private fun ScanningInstructionCard(
    isLandscape: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = PurrytifyLighterBlack.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = if (isLandscape) 16.dp else 24.dp,
                vertical = if (isLandscape) 16.dp else 24.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.QrCode,
                contentDescription = null,
                tint = PurrytifyGreen,
                modifier = Modifier.size(if (isLandscape) 36.dp else 48.dp)
            )

            Spacer(modifier = Modifier.height(if (isLandscape) 12.dp else 16.dp))

            Text(
                text = if (isLandscape) "Point camera at QR code" else "Point your camera at a Purrytify QR code",
                style = if (isLandscape) Typography.bodyMedium else Typography.bodyLarge,
                color = PurrytifyWhite,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )

            if (!isLandscape) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "The song will automatically start playing when detected",
                    style = Typography.bodyMedium,
                    color = PurrytifyLightGray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Camera permission request content - now responsive
 */
@Composable
private fun CameraPermissionContent(
    isLandscape: Boolean,
    onRequestPermission: () -> Unit
) {
    if (isLandscape) {
        // Landscape layout
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon section
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = PurrytifyLightGray,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.width(32.dp))

            // Content section
            Column(
                modifier = Modifier.weight(2f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Camera Permission Required",
                    style = Typography.titleMedium,
                    color = PurrytifyWhite,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "To scan QR codes, Purrytify needs access to your camera. This permission is only used for scanning QR codes.",
                    style = Typography.bodyMedium,
                    color = PurrytifyLightGray
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurrytifyGreen,
                        contentColor = PurrytifyWhite
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.widthIn(max = 300.dp)
                ) {
                    Text(
                        text = "Grant Camera Permission",
                        style = Typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    } else {
        // Portrait layout (original)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = PurrytifyLightGray,
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Camera Permission Required",
                style = Typography.titleLarge,
                color = PurrytifyWhite,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "To scan QR codes, Purrytify needs access to your camera. This permission is only used for scanning QR codes.",
                style = Typography.bodyLarge,
                color = PurrytifyLightGray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PurrytifyGreen,
                    contentColor = PurrytifyWhite
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Grant Camera Permission",
                    style = Typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

/**
 * Manual input content - with scrollable layout
 */
@Composable
private fun ManualInputContent(
    inputText: String,
    onInputTextChanged: (String) -> Unit,
    onSubmit: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = PurrytifyGreen,
                modifier = Modifier.size(64.dp)
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Text(
                text = "Enter Song Link",
                style = Typography.titleLarge,
                color = PurrytifyWhite,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Text(
                text = "Paste a Purrytify song link to play it directly",
                style = Typography.bodyMedium,
                color = PurrytifyLightGray,
                textAlign = TextAlign.Center
            )
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }

        item {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputTextChanged,
                label = {
                    Text(
                        text = "purrytify://song/...",
                        color = PurrytifyLightGray
                    )
                },
                placeholder = {
                    Text(
                        text = "Enter or paste song link here",
                        color = PurrytifyLightGray
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = PurrytifyWhite,
                    unfocusedTextColor = PurrytifyWhite,
                    focusedBorderColor = PurrytifyGreen,
                    unfocusedBorderColor = PurrytifyLightGray,
                    cursorColor = PurrytifyGreen,
                    focusedLabelColor = PurrytifyGreen,
                    unfocusedLabelColor = PurrytifyLightGray
                ),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Go
                ),
                keyboardActions = KeyboardActions(
                    onGo = { onSubmit() }
                ),
                singleLine = true
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Button(
                onClick = onSubmit,
                enabled = inputText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PurrytifyGreen,
                    contentColor = PurrytifyWhite,
                    disabledContainerColor = PurrytifyDarkGray,
                    disabledContentColor = PurrytifyLightGray
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Play Song",
                    style = Typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

/**
 * Success content shown briefly after successful processing - now responsive
 */
@Composable
private fun SuccessContent(isLandscape: Boolean) {
    if (isLandscape) {
        // Landscape layout
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode,
                    contentDescription = null,
                    tint = PurrytifyGreen,
                    modifier = Modifier.size(64.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Success!",
                    style = Typography.titleMedium,
                    color = PurrytifyWhite,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Opening song...",
                    style = Typography.bodyMedium,
                    color = PurrytifyLightGray
                )

                Spacer(modifier = Modifier.height(20.dp))

                CircularProgressIndicator(
                    color = PurrytifyGreen,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    } else {
        // Portrait layout (original)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.QrCode,
                contentDescription = null,
                tint = PurrytifyGreen,
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Success!",
                style = Typography.titleLarge,
                color = PurrytifyWhite,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Opening song...",
                style = Typography.bodyLarge,
                color = PurrytifyLightGray
            )

            Spacer(modifier = Modifier.height(24.dp))

            CircularProgressIndicator(
                color = PurrytifyGreen,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}