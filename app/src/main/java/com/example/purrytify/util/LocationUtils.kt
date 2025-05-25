package com.example.purrytify.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

/**
 * Utility functions for handling location-related operations
 */
object LocationUtils {
    private const val TAG = "LocationUtils"
    
    /**
     * Check if location permissions are granted
     */
    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Check if location services are enabled
     */
    fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
    
    /**
     * Open location settings if location is disabled
     */
    fun openLocationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        context.startActivity(intent)
    }
    
    /**
     * Get the last known location
     */
    fun getLastKnownLocation(context: Context): Location? {
        if (!hasLocationPermission(context)) {
            return null
        }
        
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        
        try {
            // Try GPS provider first
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let {
                    return it
                }
            }
            
            // If GPS location is null, try network provider
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)?.let {
                    return it
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting last known location: ${e.message}", e)
        }
        
        return null
    }
    
    /**
     * Get country code from location
     * Returns ISO 3166-1 alpha-2 country code
     */
    fun getCountryCodeFromLocation(context: Context, location: Location): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            
            if (addresses != null && addresses.isNotEmpty()) {
                addresses[0].countryCode ?: "" // Default to Indonesia if null
            } else {
                "" // Default to Indonesia
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting country code: ${e.message}", e)
            "" // Default to Indonesia on error
        }
    }
}