package com.example.purrytify

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PurrytifyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialization code if needed
    }
}