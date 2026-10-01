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
                "pin" to profile.pin.trim(),
                "mobile" to profile.mobileNumber.trim(),
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
                .set(userData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "User profile synchronized in Firestore for uid: $uid")
                    // Automatic cleanup of legacy or test duplicate documents with same username
                    val cleanUser = profile.username.trim().removePrefix("@").lowercase()
                    if (cleanUser.isNotBlank()) {
                        firestore.collection("users")
                            .whereEqualTo("username", cleanUser)
                            .get()
                            .addOnSuccessListener { snapshot ->
                                for (doc in snapshot.documents) {
                                    if (doc.id != uid) {
                                        Log.i(TAG, "Purging duplicate legacy user doc in Firestore: ${doc.id} (active: $uid)")
                                        doc.reference.delete()
                                    }
                                }
                            }
                    }

                    // Also cleanup duplicate docs with identical fullName & jerseyNumber (e.g., test logins)
                    val cleanName = profile.fullName.trim()
                    if (cleanName.isNotBlank() && profile.jerseyNumber > 0) {
                        firestore.collection("users")
                            .whereEqualTo("fullName", cleanName)
                            .get()
                            .addOnSuccessListener { snapshot ->
                                for (doc in snapshot.documents) {
                                    if (doc.id != uid) {
                                        val num = doc.getLong("jerseyNumber")?.toInt() ?: 0
                                        if (num == profile.jerseyNumber) {
                                            Log.i(TAG, "Purging duplicate user record by name/jersey: ${doc.id}")
                                            doc.reference.delete()
                                        }
                                    }
                                }
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to synchronize profile in Firestore: ${e.message}")
                }
        } catch (e: Throwable) {
            Log.w(TAG, "Error registering user profile: ${e.message}")
        }
    }

    /**
     * Authenticates and restores a user's profile using their unique @username and 4-6 digit PIN.
     * Allows seamless session recovery for guest and logged-out users without phone or email.
     */
    suspend fun verifyAndRestoreByUsernameAndPin(username: String, enteredPin: String): Result<CricHeroesProfile> {
        val cleanUser = username.trim().removePrefix("@").lowercase()
        val cleanPin = enteredPin.trim()

        if (cleanUser.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter your @username."))
        }
        if (cleanPin.length < 4 || cleanPin.length > 6) {
            return Result.failure(IllegalArgumentException("PIN must be 4 to 6 digits."))
        }

        return try {
            val snapshot = firestore.collection("users")
                .whereEqualTo("username", cleanUser)
                .get()
                .await()

            if (snapshot.isEmpty) {
                return Result.failure(Exception("Username '@$cleanUser' not found in player records."))
            }

            val userDoc = snapshot.documents.first()
            val savedPin = userDoc.getString("pin") ?: ""

            if (savedPin.isBlank()) {
                return Result.failure(Exception("No backup PIN was set for @$cleanUser. Please sign in with linked Google/Email."))
            }

            if (savedPin != cleanPin) {
                return Result.failure(Exception("Incorrect PIN for @$cleanUser. Please try again."))
            }

            // Ensure Firebase Auth session is active (anonymous fallback if not signed in)
            if (auth.currentUser == null) {
                try {
                    auth.signInAnonymously().await()
                } catch (e: Throwable) {
                    Log.w(TAG, "Anonymous sign in fallback: ${e.message}")
                }
            }

            val uid = userDoc.getString("uid") ?: userDoc.id
            val restoredProfile = CricHeroesProfile(
                id = uid,
                uid = uid,
                username = cleanUser,
                pin = savedPin,
                mobileNumber = userDoc.getString("mobile") ?: userDoc.getString("mobileNumber") ?: "",
                fullName = userDoc.getString("fullName") ?: userDoc.getString("name") ?: cleanUser.replaceFirstChar { it.uppercase() },
                jerseyName = userDoc.getString("jerseyName") ?: cleanUser.take(8).uppercase(),
                jerseyNumber = (userDoc.getLong("jerseyNumber") ?: 7L).toInt(),
                primaryRole = userDoc.getString("primaryRole") ?: "All-Rounder",
                battingStyle = userDoc.getString("battingStyle") ?: "Right-hand Bat",
                bowlingStyle = userDoc.getString("bowlingStyle") ?: "Right-arm Medium",
                teamName = userDoc.getString("teamName") ?: "Local XI",
                city = userDoc.getString("city") ?: "India",
                avatarEmoji = userDoc.getString("avatarEmoji") ?: "🏏",
                badgeTitle = userDoc.getString("badgeTitle") ?: "AYUUCRIC PLAYER",
                matchesPlayed = (userDoc.getLong("matchesPlayed") ?: 0L).toInt(),
                runs = (userDoc.getLong("runs") ?: 0L).toInt(),
                wickets = (userDoc.getLong("wickets") ?: 0L).toInt(),
                strikeRate = userDoc.getDouble("strikeRate") ?: 0.0,
                isVerified = userDoc.getBoolean("isVerified") ?: true,
                isOnline = true,
                isGuest = false
            )

            Result.success(restoredProfile)
        } catch (e: Throwable) {
            Log.e(TAG, "Error looking up username/PIN: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches saved CricHeroesProfile by Firebase UID from Firestore `users/{uid}`.
     */
    suspend fun fetchUserProfile(uid: String): CricHeroesProfile? {
        if (uid.isBlank()) return null
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            if (!doc.exists()) return null
            val username = doc.getString("username") ?: ""
            if (username.isBlank()) return null
            CricHeroesProfile(
                id = uid,
                uid = uid,
                username = username,
                pin = doc.getString("pin") ?: "",
                mobileNumber = doc.getString("mobile") ?: doc.getString("mobileNumber") ?: "",
                fullName = doc.getString("fullName") ?: doc.getString("name") ?: username,
                jerseyName = doc.getString("jerseyName") ?: username.take(8).uppercase(),
                jerseyNumber = (doc.getLong("jerseyNumber") ?: 7L).toInt(),
                primaryRole = doc.getString("primaryRole") ?: "All-Rounder",
                battingStyle = doc.getString("battingStyle") ?: "Right-hand Bat",
                bowlingStyle = doc.getString("bowlingStyle") ?: "Right-arm Medium",
                teamName = doc.getString("teamName") ?: "Local XI",
                city = doc.getString("city") ?: "India",
                avatarEmoji = doc.getString("avatarEmoji") ?: "🏏",
                badgeTitle = doc.getString("badgeTitle") ?: "AYUUCRIC PLAYER",
                matchesPlayed = (doc.getLong("matchesPlayed") ?: 0L).toInt(),
                runs = (doc.getLong("runs") ?: 0L).toInt(),
                wickets = (doc.getLong("wickets") ?: 0L).toInt(),
                strikeRate = doc.getDouble("strikeRate") ?: 0.0,
                isVerified = doc.getBoolean("isVerified") ?: true,
                isOnline = true,
                isGuest = false
            )
        } catch (e: Throwable) {
            Log.w(TAG, "fetchUserProfile error: ${e.message}")
            null
        }
    }

    /**
     * Fetches saved CricHeroesProfile by username from Firestore `users` collection.
     */
    suspend fun fetchUserProfileByUsername(username: String): CricHeroesProfile? {
        val clean = username.trim().removePrefix("@").lowercase()
        if (clean.isBlank()) return null
        return try {
            val snapshot = firestore.collection("users").whereEqualTo("username", clean).limit(1).get().await()
            val doc = snapshot.documents.firstOrNull() ?: return null
            val uid = doc.getString("uid") ?: doc.id
            CricHeroesProfile(
                id = uid,
                uid = uid,
                username = clean,
                pin = doc.getString("pin") ?: "",
                mobileNumber = doc.getString("mobile") ?: doc.getString("mobileNumber") ?: "",
                fullName = doc.getString("fullName") ?: doc.getString("name") ?: clean,
                jerseyName = doc.getString("jerseyName") ?: clean.take(8).uppercase(),
                jerseyNumber = (doc.getLong("jerseyNumber") ?: 7L).toInt(),
                primaryRole = doc.getString("primaryRole") ?: "All-Rounder",
                battingStyle = doc.getString("battingStyle") ?: "Right-hand Bat",
                bowlingStyle = doc.getString("bowlingStyle") ?: "Right-arm Medium",
                teamName = doc.getString("teamName") ?: "Local XI",
                city = doc.getString("city") ?: "India",
                avatarEmoji = doc.getString("avatarEmoji") ?: "🏏",
                badgeTitle = doc.getString("badgeTitle") ?: "AYUUCRIC PLAYER",
                matchesPlayed = (doc.getLong("matchesPlayed") ?: 0L).toInt(),
                runs = (doc.getLong("runs") ?: 0L).toInt(),
                wickets = (doc.getLong("wickets") ?: 0L).toInt(),
                strikeRate = doc.getDouble("strikeRate") ?: 0.0,
                isVerified = doc.getBoolean("isVerified") ?: true,
                isOnline = true,
                isGuest = false
            )
        } catch (e: Throwable) {
            Log.w(TAG, "fetchUserProfileByUsername error: ${e.message}")
            null
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
                                pin = doc.getString("pin") ?: "",
                                mobileNumber = doc.getString("mobile") ?: "",
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

                    val deduplicated = users.groupBy { u ->
                        val clean = u.username.trim().removePrefix("@").lowercase()
                        if (clean.isNotBlank()) clean else "${u.fullName.trim().lowercase()}_${u.jerseyNumber}"
                    }.map { (_, group) ->
                        // If one matches the active current user UID, prioritize it; otherwise pick highest stats
                        val currentAuthUid = auth.currentUser?.uid
                        group.firstOrNull { it.uid == currentAuthUid }
                            ?: group.maxByOrNull { it.runs + it.wickets * 20 + it.matchesPlayed * 5 }
                            ?: group.first()
                    }

                    trySend(deduplicated)
                }
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to start observeCommunityUsers listener: ${e.message}")
            null
        }

        awaitClose { listener?.remove() }
    }
}
