package com.example.data.firebase

import android.util.Log
import com.example.data.model.CricHeroesProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Production-ready User Directory Service backed exclusively by Firebase Firestore (`users/{uid}`).
 * Completely eliminates dependence on public mock REST endpoints.
 */
object UserDirectorySyncService {

    private const val TAG = "UserDirectorySync"
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    /**
     * Resolves the current immutable user UID.
     * Uses FirebaseAuth current user UID if signed in, or falls back to profile UID.
     */
    fun getEffectiveUid(fallbackProfile: CricHeroesProfile): String {
        return try {
            auth.currentUser?.uid?.ifBlank { null } ?: fallbackProfile.uid.ifBlank { fallbackProfile.id }
        } catch (_: Throwable) {
            fallbackProfile.uid.ifBlank { fallbackProfile.id }
        }
    }

    /**
     * Registers or updates a user profile in Firestore under `users/{uid}`.
     */
    fun registerUserProfile(profile: CricHeroesProfile) {
        val uid = getEffectiveUid(profile)
        try {
            val userData = hashMapOf(
                "uid" to uid,
                "username" to profile.username.trim().removePrefix("@").lowercase(),
                "fullName" to profile.fullName.trim(),
                "jerseyName" to profile.jerseyName.trim(),
                "jerseyNumber" to profile.jerseyNumber,
                "primaryRole" to profile.primaryRole,
                "battingStyle" to profile.battingStyle,
                "bowlingStyle" to profile.bowlingStyle,
                "teamName" to profile.teamName.trim(),
                "city" to profile.city.trim(),
                "avatarEmoji" to profile.avatarEmoji,
                "badgeTitle" to profile.badgeTitle,
                "matchesPlayed" to profile.matchesPlayed,
                "runs" to profile.runs,
                "wickets" to profile.wickets,
                "strikeRate" to profile.strikeRate,
                "isVerified" to profile.isVerified,
                "isOnline" to true,
                "updatedAt" to System.currentTimeMillis()
            )

            firestore.collection("users")
                .document(uid)
                .set(userData)
                .addOnSuccessListener {
                    Log.d(TAG, "User profile synchronized in Firestore for uid: $uid")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to synchronize profile in Firestore: ${e.message}")
                }
        } catch (e: Throwable) {
            Log.w(TAG, "Error registering user profile: ${e.message}")
        }
    }

    /**
     * Observes real-time community users from Firestore `users` collection.
     */
    fun observeCommunityUsers(): Flow<List<CricHeroesProfile>> = callbackFlow {
        val listener = try {
            firestore.collection("users")
                .limit(100)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "observeCommunityUsers error: ${error.message}")
                        return@addSnapshotListener
                    }

                    val users = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            val uid = doc.getString("uid") ?: doc.id
                            val username = doc.getString("username") ?: ""
                            if (username.isBlank()) return@mapNotNull null

                            CricHeroesProfile(
                                id = uid,
                                uid = uid,
                                username = username,
                                fullName = doc.getString("fullName") ?: "",
                                jerseyName = doc.getString("jerseyName") ?: "",
                                jerseyNumber = (doc.getLong("jerseyNumber") ?: 0L).toInt(),
                                primaryRole = doc.getString("primaryRole") ?: "All-Rounder",
                                battingStyle = doc.getString("battingStyle") ?: "Right-hand Bat",
                                bowlingStyle = doc.getString("bowlingStyle") ?: "Right-arm Medium",
                                teamName = doc.getString("teamName") ?: "",
                                city = doc.getString("city") ?: "",
                                avatarEmoji = doc.getString("avatarEmoji") ?: "🏏",
                                badgeTitle = doc.getString("badgeTitle") ?: "AYUUCRIC PLAYER",
                                matchesPlayed = (doc.getLong("matchesPlayed") ?: 0L).toInt(),
                                runs = (doc.getLong("runs") ?: 0L).toInt(),
                                wickets = (doc.getLong("wickets") ?: 0L).toInt(),
                                strikeRate = doc.getDouble("strikeRate") ?: 0.0,
                                isVerified = doc.getBoolean("isVerified") ?: true,
                                isOnline = doc.getBoolean("isOnline") ?: true
                            )
                        } catch (_: Throwable) {
                            null
                        }
                    } ?: emptyList()

                    trySend(users)
                }
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to start observeCommunityUsers listener: ${e.message}")
            null
        }

        awaitClose { listener?.remove() }
    }
}
