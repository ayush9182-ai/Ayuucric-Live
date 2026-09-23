package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class CricketApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                // Do not create a Firebase app with fake API keys.
                // Production builds must use a valid google-services.json from the real Firebase project.
                Log.w(
                    "CricketApplication",
                    "Firebase is not configured. Add a valid google-services.json for real auth/firestore features. " +
                        "This app will run only in demo mode without Firebase."
                )
            }
        } catch (e: Throwable) {
            Log.w("CricketApplication", "Firebase startup check failed: ${e.message}")
        }
    }
}
