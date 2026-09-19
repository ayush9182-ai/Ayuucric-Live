package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class CricketApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:516353855768:android:crictrack")
                    .setProjectId("ai-studio-crictrack")
                    .setApiKey("AIzaSyFakeKeyForLocalFallbackOnly12345")
                    .build()
                FirebaseApp.initializeApp(this, options)
                Log.d("CricketApplication", "FirebaseApp successfully initialized")
            }
        } catch (e: Throwable) {
            Log.w("CricketApplication", "FirebaseApp init fallback: ${e.message}")
        }
    }
}
