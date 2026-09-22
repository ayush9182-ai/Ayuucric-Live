package com.example.data.firebase

import android.util.Log
import com.example.data.model.BallEventEntity
import com.example.data.model.MatchEntity
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

object RealtimeMatchSyncService {

    private const val TAG = "RealtimeMatchSync"
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    /**
     * Publishes full match entity to Firebase Firestore under collection "live_matches"
     */
    fun publishMatch(match: MatchEntity, ballEvent: BallEventEntity? = null) {
        try {
            val matchData = hashMapOf(
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
                "teamAFirstInningsScore" to match.teamAFirstInningsScore,
                "teamAPlayers" to match.teamAPlayers,
                "teamBPlayers" to match.teamBPlayers,
                "dismissedBatsmenJson" to match.dismissedBatsmenJson,
                "updatedAt" to System.currentTimeMillis()
            )

            firestore.collection("live_matches")
                .document(match.id)
                .set(matchData)
                .addOnSuccessListener {
                    Log.d(TAG, "Match published to Firebase: ${match.id} -> ${match.score}/${match.wickets}")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to publish match to Firebase: ${e.message}")
                }

            // Also record the ball event under subcollection "balls" if provided
            if (ballEvent != null) {
                val ballData = hashMapOf(
                    "matchId" to ballEvent.matchId,
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
                    "timestamp" to System.currentTimeMillis()
                )

                firestore.collection("live_matches")
                    .document(match.id)
                    .collection("balls")
                    .add(ballData)
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
            teamAFirstInningsScore = doc.getString("teamAFirstInningsScore") ?: "",
            teamAPlayers = doc.getString("teamAPlayers") ?: "",
            teamBPlayers = doc.getString("teamBPlayers") ?: "",
            dismissedBatsmenJson = doc.getString("dismissedBatsmenJson") ?: ""
        )
    }
}
