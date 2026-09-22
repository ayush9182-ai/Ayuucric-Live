package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class CricketApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                // Initialize default FirebaseApp securely via google-services.json if present
                FirebaseApp.initializeApp(this)
                Log.d("CricketApplication", "FirebaseApp initialized successfully from configuration")
            }
        } catch (e: Throwable) {
            // Graceful fallback for local development or when google-services.json is absent
            Log.i("CricketApplication", "FirebaseApp init skipped (no google-services.json): ${e.message}")
        }
    }
}
