package com.example.notification

import android.content.Context
import android.util.Log
import com.example.data.firebase.RealtimeMatchSyncService
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Background synchronization and notification manager that runs at Application level.
 * Guarantees that matches going LIVE and incoming direct messages/snaps trigger
 * immediate notifications even when the app is in the background, minimized,
 * or on any other screen.
 */
object AyuuBackgroundSyncManager {

    private const val TAG = "AyuuBackgroundSync"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isStarted = false
    private var lastSeenDmTimestamp = System.currentTimeMillis() - 10_000L

    fun start(context: Context) {
        if (isStarted) return
        isStarted = true

        val appContext = context.applicationContext

        // 1. Background Live Match Listener
        scope.launch {
            try {
                Log.d(TAG, "Starting background live match notification observer...")
                RealtimeMatchSyncService.observeAllMatches().collect { matches ->
                    val liveMatches = matches.filter {
                        it.status == "LIVE" &&
                                it.id != "match_live_1" &&
                                !it.id.startsWith("dummy") &&
                                !it.id.startsWith("sample")
                    }

                    liveMatches.forEach { liveMatch ->
                        try {
                            MatchNotificationHelper.notifyMatchLive(appContext, liveMatch, forceNotify = false)
                        } catch (e: Exception) {
                            Log.w(TAG, "Error notifying live match: ${e.message}")
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Background live match observer error: ${e.message}")
            }
        }

        // 2. Background Direct Messages & Snaps Listener
        scope.launch {
            try {
                Log.d(TAG, "Starting background Direct Message / Snap observer...")
                val firestore = FirebaseFirestore.getInstance()

                firestore.collection("direct_chats")
                    .addSnapshotListener { snapshots, error ->
                        if (error != null) {
                            Log.w(TAG, "direct_chats snapshot error: ${error.message}")
                            return@addSnapshotListener
                        }
                        if (snapshots == null) return@addSnapshotListener

                        val prefs = appContext.getSharedPreferences("ayuu_cricheroes_profile", Context.MODE_PRIVATE)
                        val myUsername = prefs.getString("username", "")?.trim()?.removePrefix("@")?.lowercase().orEmpty()
                        val myUid = prefs.getString("id", "").orEmpty()

                        for (dc in snapshots.documentChanges) {
                            if (dc.type == DocumentChange.Type.ADDED || dc.type == DocumentChange.Type.MODIFIED) {
                                val doc = dc.document
                                @Suppress("UNCHECKED_CAST")
                                val participants = doc.get("participantUsernames") as? List<String>
                                @Suppress("UNCHECKED_CAST")
                                val uids = doc.get("participants") as? List<String>

                                val isMyThread = (myUsername.isNotBlank() && participants?.any { it.equals(myUsername, ignoreCase = true) } == true) ||
                                        (myUid.isNotBlank() && uids?.contains(myUid) == true)

                                if (isMyThread) {
                                    val lastSender = doc.getString("lastSenderUsername")?.trim()?.removePrefix("@")?.lowercase().orEmpty()
                                    val lastMsgAt = (doc.get("lastMessageAt") as? Number)?.toLong() ?: 0L
                                    val lastMsg = doc.getString("lastMessage").orEmpty()
                                    val lastSenderName = doc.getString("lastSenderName") ?: lastSender
                                    val isSnap = doc.getBoolean("lastIsSnap") ?: false

                                    // If message was sent by someone else and is fresh
                                    if (lastSender.isNotBlank() &&
                                        !lastSender.equals(myUsername, ignoreCase = true) &&
                                        lastMsgAt > lastSeenDmTimestamp &&
                                        (System.currentTimeMillis() - lastMsgAt < 120_000L)
                                    ) {
                                        lastSeenDmTimestamp = lastMsgAt
                                        MatchNotificationHelper.notifyDirectMessage(
                                            context = appContext,
                                            senderName = lastSenderName,
                                            senderUsername = lastSender,
                                            messageText = lastMsg.ifBlank { if (isSnap) "⚡ Sent you a Snap!" else "New message" },
                                            isSnap = isSnap
                                        )
                                    }
                                }
                            }
                        }
                    }
            } catch (e: Throwable) {
                Log.w(TAG, "Background DM listener error: ${e.message}")
            }
        }

        // 3. Background DRS Appeal & Match Result Listener across live matches
        scope.launch {
            try {
                Log.d(TAG, "Starting background DRS & Match Result observer...")
                var lastSeenDrsTs = System.currentTimeMillis() - 5_000L
                val notifiedFinishedMatches = mutableSetOf<String>()

                RealtimeMatchSyncService.observeAllMatches().collect { matches ->
                    matches.forEach { match ->
                        if (match.status == "FINISHED" && !notifiedFinishedMatches.contains(match.id)) {
                            notifiedFinishedMatches.add(match.id)
                            MatchNotificationHelper.notifyGenericPushAlert(
                                context = appContext,
                                title = "🏆 Match Finished: ${match.teamA} vs ${match.teamB}",
                                message = match.statusDetail.ifBlank { "Match result is available now." },
                                matchId = match.id
                            )
                        }
                    }

                    val activeLive = matches.firstOrNull { it.status == "LIVE" && !it.id.startsWith("dummy") }
                    if (activeLive != null) {
                        try {
                            FirebaseFirestore.getInstance().collection("live_matches")
                                .document(activeLive.id)
                                .get()
                                .addOnSuccessListener { snap ->
                                    @Suppress("UNCHECKED_CAST")
                                    val drsMap = snap?.get("activeDrsAppeal") as? Map<String, Any?>
                                    if (drsMap != null) {
                                        val ts = (drsMap["timestamp"] as? Number)?.toLong() ?: 0L
                                        val appeal = drsMap["appealType"] as? String ?: "LBW"
                                        if (ts > lastSeenDrsTs && (System.currentTimeMillis() - ts < 15_000L)) {
                                            lastSeenDrsTs = ts
                                            MatchNotificationHelper.notifyDrsTaken(
                                                context = appContext,
                                                appealType = appeal,
                                                batsman = activeLive.strikerName,
                                                bowler = activeLive.bowlerName
                                            )
                                        }
                                    }
                                }
                        } catch (_: Throwable) {}
                    }
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Background DRS & Match Result observer error: ${e.message}")
            }
        }
    }
}
