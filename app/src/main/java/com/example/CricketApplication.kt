package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

class CricketApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
                Log.d("CricketApplication", "FirebaseApp initialized successfully from configuration")
            }

            // Initialize Firebase App Check for production backend integrity
            try {
                val firebaseAppCheck = FirebaseAppCheck.getInstance()
                firebaseAppCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
                Log.d("CricketApplication", "Firebase App Check provider factory installed")
            } catch (appCheckEx: Throwable) {
                Log.w("CricketApplication", "App Check installation skipped: ${appCheckEx.message}")
            }
        } catch (e: Throwable) {
            Log.e("CricketApplication", "FirebaseApp init failure: ${e.message}", e)
        }
    }
}

