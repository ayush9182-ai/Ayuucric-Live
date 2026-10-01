package com.example.data.firebase

import android.util.Log
import com.example.data.model.BallEventEntity
import com.example.data.model.DrsDecisionCardData
import com.example.data.model.MatchEntity
import com.example.data.model.TeamStandingEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object RealtimeMatchSyncService {

    private const val TAG = "RealtimeMatchSync"
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    suspend fun ensureAuth() {
        try {
            if (auth.currentUser == null) {
                auth.signInAnonymously().await()
                Log.d(TAG, "ensureAuth: Anonymous authentication active for sync: ${auth.currentUser?.uid}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "ensureAuth notice: ${e.message}")
        }
    }

    suspend fun fetchRemoteMatchesOnce(): List<MatchEntity> {
        ensureAuth()
        return try {
            val snapshot = firestore.collection("live_matches").get().await()
            snapshot.documents.mapNotNull { docToMatchEntity(it) }
        } catch (e: Exception) {
            Log.w(TAG, "fetchRemoteMatchesOnce error: ${e.message}")
            emptyList()
        }
    }

    /**
     * Publishes full match entity to Firebase Firestore under collection "live_matches"
     */
    fun publishMatch(
        match: MatchEntity,
        ballEvent: BallEventEntity? = null,
        sidhuDialogue: String? = null
    ) {
        if (match.id.isBlank()) return
        try {
            // Ensure auth session before publishing
            if (auth.currentUser == null) {
                auth.signInAnonymously().addOnCompleteListener {
                    publishMatchInternal(match, ballEvent, sidhuDialogue)
                }
            } else {
                publishMatchInternal(match, ballEvent, sidhuDialogue)
            }
        } catch (e: Exception) {
            Log.w(TAG, "publishMatch exception: ${e.message}")
        }
    }

    private fun publishMatchInternal(
        match: MatchEntity,
        ballEvent: BallEventEntity?,
        sidhuDialogue: String?
    ) {
        if (match.id.isBlank()) return
        try {
            val matchData = hashMapOf<String, Any?>(
                "id" to match.id,
                "tournamentName" to match.tournamentName,
                "teamA" to match.teamA,
                "teamB" to match.teamB,
                "teamAShort" to match.teamAShort,
                "teamBShort" to match.teamBShort,
                "teamAColorHex" to match.teamAColorHex,
                "teamBColorHex" to match.teamBColorHex,
                "currentInnings" to match.currentInnings,
                "battingTeam" to match.battingTeam,
                "bowlingTeam" to match.bowlingTeam,
                "score" to match.score,
                "wickets" to match.wickets,
                "legalBalls" to match.legalBalls,
                "totalOvers" to match.totalOvers,
                "target" to match.target,
                "status" to match.status,
                "statusDetail" to match.statusDetail,
                "strikerName" to match.strikerName,
                "strikerRuns" to match.strikerRuns,
                "strikerBalls" to match.strikerBalls,
                "strikerFours" to match.strikerFours,
                "strikerSixes" to match.strikerSixes,
                "nonStrikerName" to match.nonStrikerName,
                "nonStrikerRuns" to match.nonStrikerRuns,
                "nonStrikerBalls" to match.nonStrikerBalls,
                "nonStrikerFours" to match.nonStrikerFours,
                "nonStrikerSixes" to match.nonStrikerSixes,
                "bowlerName" to match.bowlerName,
                "bowlerBalls" to match.bowlerBalls,
                "bowlerMaidens" to match.bowlerMaidens,
                "bowlerRuns" to match.bowlerRuns,
                "bowlerWickets" to match.bowlerWickets,
                "venue" to match.venue,
                "matchDate" to match.matchDate,
                "matchTime" to match.matchTime,
                "venueAddress" to match.venueAddress,
                "venueCoordinates" to match.venueCoordinates,
                "teamAFirstInningsScore" to match.teamAFirstInningsScore,
                "teamAPlayers" to match.teamAPlayers,
                "teamBPlayers" to match.teamBPlayers,
                "dismissedBatsmenJson" to match.dismissedBatsmenJson,
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "revision" to com.google.firebase.firestore.FieldValue.increment(1)
            )

            if (!sidhuDialogue.isNullOrBlank()) {
                matchData["latestSidhuDialogue"] = sidhuDialogue
                matchData["lastBallTimestamp"] = System.currentTimeMillis()
                matchData["lastBallId"] = ballEvent?.id ?: java.util.UUID.randomUUID().toString()
            }

            firestore.collection("live_matches")
                .document(match.id)
                .set(matchData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "Match published to Firebase: ${match.id} -> ${match.score}/${match.wickets}")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to publish match to Firebase: ${e.message}")
                }

            // Record the ball event idempotently under subcollections "deliveries" and "balls"
            if (ballEvent != null) {
                val deliveryId = "${match.id}_inn${match.currentInnings}_ov${ballEvent.overNumber}_b${ballEvent.ballInOver}_${ballEvent.id}"
                val ballData = hashMapOf(
                    "deliveryId" to deliveryId,
                    "matchId" to ballEvent.matchId,
                    "innings" to match.currentInnings,
                    "overNumber" to ballEvent.overNumber,
                    "ballInOver" to ballEvent.ballInOver,
                    "runs" to ballEvent.runs,
                    "isWicket" to ballEvent.isWicket,
                    "wicketType" to ballEvent.wicketType,
                    "extraType" to ballEvent.extraType,
                    "batsman" to ballEvent.batsman,
                    "bowler" to ballEvent.bowler,
                    "commentary" to ballEvent.commentary,
                    "shotAngle" to ballEvent.shotAngle,
                    "pitchZone" to ballEvent.pitchZone,
                    "isBoundary" to ballEvent.isBoundary,
                    "isSix" to ballEvent.isSix,
                    "timestamp" to System.currentTimeMillis(),
                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )

                // Write to deliveries subcollection idempotently
                firestore.collection("live_matches")
                    .document(match.id)
                    .collection("deliveries")
                    .document(deliveryId)
                    .set(ballData, com.google.firebase.firestore.SetOptions.merge())

                // Also maintain balls subcollection with the same deterministic document ID
                firestore.collection("live_matches")
                    .document(match.id)
                    .collection("balls")
                    .document(deliveryId)
                    .set(ballData, com.google.firebase.firestore.SetOptions.merge())
            }
        } catch (e: Exception) {
            Log.w(TAG, "publishMatch exception: ${e.message}")
        }
    }

    /**
     * Real-time listener for ALL live matches on Firebase.
     * Any phone connected to the internet gets instant updates.
     */
    fun observeAllMatches(): Flow<List<MatchEntity>> = callbackFlow {
        val listener = firestore.collection("live_matches")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "observeAllMatches error: ${error.message}")
                    return@addSnapshotListener
                }

                val matches = snapshot?.documents?.mapNotNull { doc ->
                    docToMatchEntity(doc)
                } ?: emptyList()

                trySend(matches)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Real-time listener for a single match on Firebase
     */
    fun observeSingleMatch(matchId: String): Flow<MatchEntity?> = callbackFlow {
        if (matchId.isBlank()) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val listener = firestore.collection("live_matches")
            .document(matchId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "observeSingleMatch error: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val match = docToMatchEntity(snapshot)
                    trySend(match)
                }
            }

        awaitClose { listener.remove() }
    }

    /**
     * Converts a Firestore document snapshot into MatchEntity
     */
    fun docToMatchEntity(doc: DocumentSnapshot): MatchEntity? {
        val id = doc.getString("id") ?: doc.id
        if (id.isBlank()) return null
        return MatchEntity(
            id = id,
            tournamentName = doc.getString("tournamentName") ?: "Live Match",
            teamA = doc.getString("teamA") ?: "Team A",
            teamB = doc.getString("teamB") ?: "Team B",
            teamAShort = doc.getString("teamAShort") ?: "TMA",
            teamBShort = doc.getString("teamBShort") ?: "TMB",
            teamAColorHex = doc.getLong("teamAColorHex") ?: 0xFF2563EB,
            teamBColorHex = doc.getLong("teamBColorHex") ?: 0xFFDC2626,
            currentInnings = (doc.getLong("currentInnings") ?: 1L).toInt(),
            battingTeam = doc.getString("battingTeam") ?: "Team A",
            bowlingTeam = doc.getString("bowlingTeam") ?: "Team B",
            score = (doc.getLong("score") ?: 0L).toInt(),
            wickets = (doc.getLong("wickets") ?: 0L).toInt(),
            legalBalls = (doc.getLong("legalBalls") ?: 0L).toInt(),
            totalOvers = (doc.getLong("totalOvers") ?: 10L).toInt(),
            target = (doc.getLong("target") ?: 0L).toInt(),
            status = doc.getString("status") ?: "LIVE",
            statusDetail = doc.getString("statusDetail") ?: "",
            strikerName = doc.getString("strikerName") ?: "Striker",
            strikerRuns = (doc.getLong("strikerRuns") ?: 0L).toInt(),
            strikerBalls = (doc.getLong("strikerBalls") ?: 0L).toInt(),
            strikerFours = (doc.getLong("strikerFours") ?: 0L).toInt(),
            strikerSixes = (doc.getLong("strikerSixes") ?: 0L).toInt(),
            nonStrikerName = doc.getString("nonStrikerName") ?: "Non-Striker",
            nonStrikerRuns = (doc.getLong("nonStrikerRuns") ?: 0L).toInt(),
            nonStrikerBalls = (doc.getLong("nonStrikerBalls") ?: 0L).toInt(),
            nonStrikerFours = (doc.getLong("nonStrikerFours") ?: 0L).toInt(),
            nonStrikerSixes = (doc.getLong("nonStrikerSixes") ?: 0L).toInt(),
            bowlerName = doc.getString("bowlerName") ?: "Bowler",
            bowlerBalls = (doc.getLong("bowlerBalls") ?: 0L).toInt(),
            bowlerMaidens = (doc.getLong("bowlerMaidens") ?: 0L).toInt(),
            bowlerRuns = (doc.getLong("bowlerRuns") ?: 0L).toInt(),
            bowlerWickets = (doc.getLong("bowlerWickets") ?: 0L).toInt(),
            venue = doc.getString("venue") ?: "",
            matchDate = doc.getString("matchDate") ?: "",
            matchTime = doc.getString("matchTime") ?: "",
            venueAddress = doc.getString("venueAddress") ?: "",
            venueCoordinates = doc.getString("venueCoordinates") ?: "",
            teamAFirstInningsScore = doc.getString("teamAFirstInningsScore") ?: "",
            teamAPlayers = doc.getString("teamAPlayers") ?: "",
            teamBPlayers = doc.getString("teamBPlayers") ?: "",
            dismissedBatsmenJson = doc.getString("dismissedBatsmenJson") ?: ""
        )
    }

    /**
     * Broadcasts a DRS Decision Card to all connected devices in real time via Firestore.
     */
    fun publishDrsDecision(decisionCard: DrsDecisionCardData) {
        if (decisionCard.matchId.isBlank()) return
        try {
            val drsData = hashMapOf(
                "decisionId" to decisionCard.id,
                "appealType" to decisionCard.appealType,
                "batsman" to decisionCard.batsman,
                "bowler" to decisionCard.bowler,
                "onFieldDecision" to decisionCard.onFieldDecision,
                "thirdUmpireDecision" to decisionCard.thirdUmpireDecision,
                "pitching" to decisionCard.pitching,
                "impact" to decisionCard.impact,
                "wickets" to decisionCard.wickets,
                "timestamp" to decisionCard.timestamp
            )
            firestore.collection("live_matches")
                .document(decisionCard.matchId)
                .set(mapOf("lastDrsDecision" to drsData), com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "DRS Decision card broadcasted to Firebase for match ${decisionCard.matchId}")
                }
        } catch (e: Exception) {
            Log.w(TAG, "publishDrsDecision error: ${e.message}")
        }
    }

    /**
     * Observes real-time DRS Decision events for all phones.
     */
    fun observeDrsDecisions(matchId: String): Flow<com.example.data.model.DrsDecisionCardData?> = callbackFlow {
        if (matchId.isBlank()) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val listener = firestore.collection("live_matches")
            .document(matchId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    val rawMap = snapshot.get("lastDrsDecision") as? Map<String, Any?>
                    if (rawMap != null) {
                        val ts = (rawMap["timestamp"] as? Number)?.toLong() ?: 0L
                        // Only show recent reviews within 60 seconds
                        if (System.currentTimeMillis() - ts < 60_000L) {
                            val decisionId = rawMap["decisionId"] as? String ?: "${matchId}_$ts"
                            val card = com.example.data.model.DrsDecisionCardData(
                                id = decisionId,
                                matchId = matchId,
                                appealType = rawMap["appealType"] as? String ?: "LBW",
                                batsman = rawMap["batsman"] as? String ?: "Batter",
                                bowler = rawMap["bowler"] as? String ?: "Bowler",
                                onFieldDecision = rawMap["onFieldDecision"] as? String ?: "NOT OUT",
                                thirdUmpireDecision = rawMap["thirdUmpireDecision"] as? String ?: "OUT",
                                pitching = rawMap["pitching"] as? String ?: "IN_LINE",
                                impact = rawMap["impact"] as? String ?: "IN_LINE",
                                wickets = rawMap["wickets"] as? String ?: "HITTING",
                                timestamp = ts
                            )
                            trySend(card)
                        } else {
                            trySend(null)
                        }
                    } else {
                        trySend(null)
                    }
                }
            }

        awaitClose { listener.remove() }
    }

    /**
     * Broadcasts an active DRS Appeal so Sidhu Paaji announces it across all connected phones
     */
    fun publishDrsAppeal(matchId: String, appealType: String, initiatedBy: String = "Player") {
        if (matchId.isBlank()) return
        try {
            val appealData = hashMapOf(
                "appealType" to appealType,
                "initiatedBy" to initiatedBy,
                "timestamp" to System.currentTimeMillis()
            )
            firestore.collection("live_matches")
                .document(matchId)
                .set(mapOf("activeDrsAppeal" to appealData), com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "DRS Appeal broadcasted to Firebase for match $matchId")
                }
        } catch (e: Exception) {
            Log.w(TAG, "publishDrsAppeal error: ${e.message}")
        }
    }

    /**
     * Clears active DRS appeal from Firestore once review is resolved or dismissed
     */
    fun clearDrsAppeal(matchId: String) {
        if (matchId.isBlank()) return
        try {
            firestore.collection("live_matches")
                .document(matchId)
                .update("activeDrsAppeal", com.google.firebase.firestore.FieldValue.delete())
                .addOnSuccessListener {
                    Log.d(TAG, "DRS Appeal cleared from Firebase for match $matchId")
                }
        } catch (e: Exception) {
            Log.w(TAG, "clearDrsAppeal error: ${e.message}")
        }
    }

    /**
     * Clears last DRS decision card from Firestore once dismissed
     */
    fun clearDrsDecision(matchId: String) {
        if (matchId.isBlank()) return
        try {
            firestore.collection("live_matches")
                .document(matchId)
                .update("lastDrsDecision", com.google.firebase.firestore.FieldValue.delete())
                .addOnSuccessListener {
                    Log.d(TAG, "DRS Decision cleared from Firebase for match $matchId")
                }
        } catch (e: Exception) {
            Log.w(TAG, "clearDrsDecision error: ${e.message}")
        }
    }

    /**
     * Observes real-time DRS Appeal trigger to announce via Sidhu Paaji voice
     */
    fun observeDrsAppeal(matchId: String): Flow<Pair<String, Long>?> = callbackFlow {
        if (matchId.isBlank()) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val listener = firestore.collection("live_matches")
            .document(matchId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    val rawMap = snapshot.get("activeDrsAppeal") as? Map<String, Any?>
                    if (rawMap != null) {
                        val ts = (rawMap["timestamp"] as? Number)?.toLong() ?: 0L
                        val appeal = rawMap["appealType"] as? String ?: "DRS"
                        // Only trigger if happened in last 30 seconds
                        if (System.currentTimeMillis() - ts < 30_000L) {
                            trySend(Pair(appeal, ts))
                        } else {
                            trySend(null)
                        }
                    } else {
                        trySend(null)
                    }
                }
            }
        awaitClose { listener.remove() }
    }

    /**
     * Observes real-time Sidhu Paaji commentary audio broadcast from the active scorer phone.
     * All spectators and umpires will hear the voice commentary in real-time.
     */
    fun observeLiveBallAudio(matchId: String): Flow<Pair<String, Long>?> = callbackFlow {
        if (matchId.isBlank()) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val listener = firestore.collection("live_matches")
            .document(matchId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val dialogue = snapshot.getString("latestSidhuDialogue")
                    val ts = (snapshot.get("lastBallTimestamp") as? Number)?.toLong() ?: 0L
                    if (!dialogue.isNullOrBlank() && (System.currentTimeMillis() - ts < 25_000L)) {
                        trySend(Pair(dialogue, ts))
                    } else {
                        trySend(null)
                    }
                }
            }
        awaitClose { listener.remove() }
    }

    /**
     * Deletes a match from Firestore collection "live_matches"
     */
    fun deleteRemoteMatch(matchId: String) {
        if (matchId.isBlank()) return
        try {
            firestore.collection("live_matches").document(matchId).delete()
                .addOnSuccessListener {
                    Log.d(TAG, "Deleted match $matchId from Firestore")
                }
        } catch (e: Exception) {
            Log.w(TAG, "deleteRemoteMatch error: ${e.message}")
        }
    }

    /**
     * Marks a match as COMPLETED in Firestore
     */
    fun markMatchCompleted(matchId: String, summaryText: String = "Match Completed") {
        if (matchId.isBlank()) return
        try {
            firestore.collection("live_matches").document(matchId)
                .update(
                    mapOf(
                        "status" to "COMPLETED",
                        "statusDetail" to summaryText
                    )
                )
        } catch (e: Exception) {
            Log.w(TAG, "markMatchCompleted error: ${e.message}")
        }
    }

    const val COLLECTION_STANDINGS = "tournament_standings"

    /**
     * Publishes computed or updated standings to Firestore collection "tournament_standings"
     */
    fun publishTournamentStandings(standings: List<TeamStandingEntity>) {
        if (standings.isEmpty()) return
        try {
            val batch = firestore.batch()
            for (standing in standings) {
                val docRef = firestore.collection(COLLECTION_STANDINGS).document(standing.teamId)
                val data = hashMapOf(
                    "teamId" to standing.teamId,
                    "name" to standing.name,
                    "shortName" to standing.shortName,
                    "matchesPlayed" to standing.matchesPlayed,
                    "won" to standing.won,
                    "lost" to standing.lost,
                    "tied" to standing.tied,
                    "points" to standing.points,
                    "netRunRate" to standing.netRunRate,
                    "formGuide" to standing.formGuide,
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
                batch.set(docRef, data, com.google.firebase.firestore.SetOptions.merge())
            }
            batch.commit()
                .addOnSuccessListener {
                    Log.d(TAG, "Tournament standings synced to Firestore (${standings.size} teams)")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to sync standings to Firestore: ${e.message}")
                }
        } catch (e: Exception) {
            Log.w(TAG, "publishTournamentStandings error: ${e.message}")
        }
    }

    /**
     * Fetches tournament standings once from Firestore
     */
    suspend fun fetchTournamentStandingsOnce(): List<TeamStandingEntity> {
        ensureAuth()
        return try {
            val snapshot = firestore.collection(COLLECTION_STANDINGS).get().await()
            snapshot.documents.mapNotNull { docToTeamStanding(it) }
                .sortedWith(
                    compareByDescending<TeamStandingEntity> { it.points }
                        .thenByDescending { it.netRunRate }
                        .thenByDescending { it.won }
                )
        } catch (e: Exception) {
            Log.w(TAG, "fetchTournamentStandingsOnce error: ${e.message}")
            emptyList()
        }
    }

    /**
     * Listens to real-time updates for tournament standings from Firestore collection "tournament_standings"
     */
    fun listenToTournamentStandings(): Flow<List<TeamStandingEntity>> = callbackFlow {
        val listener = firestore.collection(COLLECTION_STANDINGS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Standings snapshot listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    val list = snapshot.documents.mapNotNull { docToTeamStanding(it) }
                        .sortedWith(
                            compareByDescending<TeamStandingEntity> { it.points }
                                .thenByDescending { it.netRunRate }
                                .thenByDescending { it.won }
                        )
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    /**
     * Converts a Firestore document to TeamStandingEntity
     */
    fun docToTeamStanding(doc: DocumentSnapshot): TeamStandingEntity? {
        val teamId = doc.getString("teamId") ?: doc.id
        if (teamId.isBlank()) return null
        return TeamStandingEntity(
            teamId = teamId,
            name = doc.getString("name") ?: "Team",
            shortName = doc.getString("shortName") ?: doc.getString("name")?.take(3)?.uppercase() ?: "TM",
            matchesPlayed = (doc.getLong("matchesPlayed") ?: 0L).toInt(),
            won = (doc.getLong("won") ?: 0L).toInt(),
            lost = (doc.getLong("lost") ?: 0L).toInt(),
            tied = (doc.getLong("tied") ?: 0L).toInt(),
            points = (doc.getLong("points") ?: 0L).toInt(),
            netRunRate = doc.getDouble("netRunRate") ?: 0.0,
            formGuide = doc.getString("formGuide") ?: "–"
        )
    }
}
