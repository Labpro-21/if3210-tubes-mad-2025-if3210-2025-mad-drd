package com.example.purrytify.ui.components

import android.content.Context
import android.location.Geocoder
import android.os.Build
import android.util.Log
import android.view.ViewGroup.LayoutParams
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.purrytify.ui.theme.PurrytifyBlack
import com.example.purrytify.ui.theme.PurrytifyDarkGray
import com.example.purrytify.ui.theme.PurrytifyGreen
import com.example.purrytify.ui.theme.PurrytifyLightGray
import com.example.purrytify.ui.theme.PurrytifyLighterBlack
import com.example.purrytify.ui.theme.PurrytifyWhite
import com.example.purrytify.ui.theme.Typography
import com.example.purrytify.util.CountryUtils
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.util.Locale

@Composable
fun MapPickerDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onLocationSelected: (String) -> Unit
) {
    if (!isVisible) return
    
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Set up map configuration
    Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
    
    // Set up state
    var selectedLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var selectedCountryCode by remember { mutableStateOf<String?>(null) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    
    // Show the dialog fullscreen but with decorations (system bars)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = PurrytifyBlack
        ) {
            // Handle lifecycle events for the map
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> mapView?.onResume()
                        Lifecycle.Event.ON_PAUSE -> mapView?.onPause()
                        else -> { /* Do nothing */ }
                    }
                }
                
                lifecycleOwner.lifecycle.addObserver(observer)
                
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    mapView?.onDetach()
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Title
                    Text(
                        text = "Select Location",
                        style = Typography.titleLarge,
                        color = PurrytifyWhite,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Close button
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = PurrytifyWhite,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                // Instructions
                Text(
                    text = "Tap on the map to select your location",
                    style = Typography.bodyMedium,
                    color = PurrytifyLightGray,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    textAlign = TextAlign.Center
                )
                
                // Selection info
                selectedCountryCode?.let { countryCode ->
                    val countryName = CountryUtils.getCountryNameFromCode(countryCode) ?: "Unknown"
                    val flagEmoji = CountryUtils.getFlagEmoji(countryCode)
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = PurrytifyLighterBlack
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = flagEmoji,
                                style = MaterialTheme.typography.headlineMedium
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = countryName,
                                style = Typography.titleMedium,
                                color = PurrytifyWhite,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Text(
                                text = "($countryCode)",
                                style = Typography.bodyMedium,
                                color = PurrytifyLightGray
                            )
                        }
                    }
                }
                
                // Map
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(PurrytifyLighterBlack)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            MapView(ctx).apply {
                                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                                setTileSource(TileSourceFactory.MAPNIK)
                                setMultiTouchControls(true)
                                
                                controller.apply {
                                    setZoom(3.0) // Initial zoom level
                                    setCenter(GeoPoint(0.0, 0.0)) // Initial center
                                }
                                
                                // Add a tap overlay to detect user taps
                                val mapEventsReceiver = object : MapEventsReceiver {
                                    override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                                        selectedLocation = p
                                        updateMarker(this@apply, p)
                                        getCountryFromLocation(ctx, p) { countryCode ->
                                            selectedCountryCode = countryCode
                                        }
                                        return true
                                    }
                                    
                                    override fun longPressHelper(p: GeoPoint): Boolean {
                                        return false
                                    }
                                }
                                
                                val eventsOverlay = MapEventsOverlay(mapEventsReceiver)
                                overlays.add(eventsOverlay)
                                
                                mapView = this
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Confirm button
                Button(
                    onClick = {
                        selectedCountryCode?.let { countryCode ->
                            onLocationSelected(countryCode)
                        }
                    },
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
                    enabled = selectedCountryCode != null
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Confirm selection",
                        tint = PurrytifyWhite
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = "Confirm Location",
                        style = Typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Cancel button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
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

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Update marker on the map for selected location
 */
private fun updateMarker(mapView: MapView?, geoPoint: GeoPoint) {
    mapView?.overlays?.let { overlays ->
        // Remove existing marker if any
        overlays.removeAll { it is Marker }
        
        // Add new marker
        val marker = Marker(mapView).apply {
            position = geoPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        
        overlays.add(marker)
        
        // Refresh map
        mapView.invalidate()
    }
}

/**
 * Get country code from the selected location using Geocoder
 */
private fun getCountryFromLocation(context: Context, geoPoint: GeoPoint, callback: (String) -> Unit) {
    try {
        val geocoder = Geocoder(context, Locale.getDefault())
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // API 33+ async method
            geocoder.getFromLocation(geoPoint.latitude, geoPoint.longitude, 1) { addresses ->
                val countryCode = if (addresses.isNotEmpty()) {
                    addresses[0].countryCode ?: ""
                } else {
                    ""
                }
                callback(countryCode)
            }
        } else {
            // Older synchronous method
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(geoPoint.latitude, geoPoint.longitude, 1)
            val countryCode = if (addresses != null && addresses.isNotEmpty()) {
                addresses[0].countryCode ?: ""
            } else {
                ""
            }
            callback(countryCode)
        }
    } catch (e: Exception) {
        Log.e("MapPicker", "Error getting country: ${e.message}", e)
        callback("")
    }
}