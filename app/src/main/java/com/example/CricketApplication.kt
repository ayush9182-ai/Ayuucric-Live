package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class CricketApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            com.example.notification.MatchNotificationHelper.createNotificationChannel(this)
        } catch (_: Throwable) {}
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = com.google.firebase.FirebaseOptions.fromResource(this)
                    ?: com.google.firebase.FirebaseOptions.Builder()
                        .setApplicationId("1:40080303796:android:8584395d0141a8ef9319e1")
                        .setApiKey("AIzaSyDHzqKTHiHwGrHU9A2NhE0QlT7NL5jbT5E")
                        .setProjectId("ayuucric-live")
                        .setGcmSenderId("40080303796")
                        .setStorageBucket("ayuucric-live.firebasestorage.app")
                        .build()
                FirebaseApp.initializeApp(this, options)
            }
            if (FirebaseApp.getApps(this).isEmpty()) {
                throw IllegalStateException(
                    "Firebase is not configured. Add the real google-services.json for this app. " +
                        "No fake Firebase setup is allowed."
                )
            }
            Log.d("CricketApplication", "Firebase initialized successfully")
            try {
                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                auth.firebaseAuthSettings.setAppVerificationDisabledForTesting(true)
                if (auth.currentUser == null) {
                    auth.signInAnonymously()
                        .addOnSuccessListener {
                            Log.d("CricketApplication", "Firebase Auth: Initialized anonymous session for cross-phone sync: ${it.user?.uid}")
                        }
                        .addOnFailureListener { authErr ->
                            Log.w("CricketApplication", "Firebase Auth: Anonymous session error: ${authErr.message}")
                        }
                } else {
                    Log.d("CricketApplication", "Firebase Auth: Session already active: ${auth.currentUser?.uid}")
                }
            } catch (authEx: Throwable) {
                Log.w("CricketApplication", "Notice: ${authEx.message}")
            }

            // Start global background notification and foreground sync service
            try {
                com.example.notification.AyuuBackgroundSyncManager.start(this)
                com.example.notification.CricLiveBackgroundService.start(this)
            } catch (bgEx: Throwable) {
                Log.w("CricketApplication", "Background sync start notice: ${bgEx.message}")
            }
        } catch (e: Throwable) {
            Log.e(
                "CricketApplication",
                "Firebase configuration missing. App cannot start without a valid Firebase project.",
                e
            )
            throw IllegalStateException(
                "Firebase configuration missing. App cannot start without a valid Firebase project.",
                e
            )
        }
    }
}
