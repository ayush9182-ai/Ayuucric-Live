package com.example.data.cloud

import android.util.Log
import com.example.data.model.BallEventEntity
import com.example.data.model.MatchEntity
import com.example.data.model.ChatMessage
import com.example.data.model.CricHeroesProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class CloudMessageDto(
    val id: String,
    val roomId: String,
    val senderUsername: String,
    val recipientUsername: String,
    val senderName: String,
    val senderRole: String,
    val avatarEmoji: String,
    val message: String,
    val timestamp: Long
)

data class CloudMatchDto(
    val matchId: String,
    val score: Int,
    val wickets: Int,
    val legalBalls: Int,
    val totalOvers: Int,
    val target: Int,
    val strikerName: String,
    val strikerRuns: Int,
    val strikerBalls: Int,
    val strikerFours: Int,
    val strikerSixes: Int,
    val nonStrikerName: String,
    val nonStrikerRuns: Int,
    val nonStrikerBalls: Int,
    val bowlerName: String,
    val bowlerRuns: Int,
    val bowlerBalls: Int,
    val bowlerWickets: Int,
    val battingTeam: String,
    val bowlingTeam: String,
    val status: String,
    val lastBallRuns: Int,
    val lastBallExtra: String,
    val lastBallCommentary: String,
    val updatedAt: Long
)

object CloudSyncService {
    private const val TAG = "CloudSyncService"
    private const val BASE_URL = "https://api.restful-api.dev/objects"

    // Dedicated Cloud Storage Object IDs on restful-api.dev
    private const val USER_REGISTRY_OBJ_ID = "ff808181a09d98f701a0c346bfc55e6c"
    private const val MESSAGES_HUB_OBJ_ID = "ff808181a09d98f701a0c34703945e6e"
    private const val LIVE_MATCH_OBJ_ID = "ff808181a09d98f701a0c346f7d05e6d"

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .build()

    /**
     * 1. Register or update player profile in the global cloud directory.
     * Accessible to all other downloaded apps & phones across India.
     */
    suspend fun registerUserProfileInCloud(profile: CricHeroesProfile): Boolean = withContext(Dispatchers.IO) {
        if (profile.username.isBlank()) return@withContext false
        try {
            val existingUsers = fetchAllUsersFromCloud().toMutableList()
            // Remove previous entry of this user if exists
            val cleanUsername = profile.username.removePrefix("@").trim().lowercase()
            existingUsers.removeAll {
                it.username.removePrefix("@").trim().lowercase() == cleanUsername ||
                (it.mobileNumber.isNotBlank() && it.mobileNumber == profile.mobileNumber) ||
                it.id == profile.id
            }
            existingUsers.add(0, profile)

            val usersJsonArray = JSONArray()
            existingUsers.take(150).forEach { u ->
                val obj = JSONObject().apply {
                    put("id", u.id)
                    put("username", u.username)
                    put("mobileNumber", u.mobileNumber)
                    put("fullName", u.fullName)
                    put("jerseyName", u.jerseyName)
                    put("jerseyNumber", u.jerseyNumber)
                    put("primaryRole", u.primaryRole)
                    put("battingStyle", u.battingStyle)
                    put("bowlingStyle", u.bowlingStyle)
                    put("teamName", u.teamName)
                    put("city", u.city)
                    put("avatarEmoji", u.avatarEmoji)
                    put("badgeTitle", u.badgeTitle)
                    put("matchesPlayed", u.matchesPlayed)
                    put("runs", u.runs)
                    put("wickets", u.wickets)
                    put("strikeRate", u.strikeRate)
                    put("isVerified", u.isVerified)
                    put("isOnline", true)
                    put("lastSeen", System.currentTimeMillis())
                }
                usersJsonArray.put(obj)
            }

            val payload = JSONObject().apply {
                put("name", "AyuuCric_User_Directory_V1")
                put("data", JSONObject().apply {
                    put("users", usersJsonArray)
                    put("lastUpdated", System.currentTimeMillis())
                })
            }

            val request = Request.Builder()
                .url("$BASE_URL/$USER_REGISTRY_OBJ_ID")
                .put(payload.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val isSuccess = response.isSuccessful
            response.close()
            Log.d(TAG, "registerUserProfileInCloud: ${profile.username} -> success=$isSuccess")
            isSuccess
        } catch (e: Exception) {
            Log.w(TAG, "registerUserProfileInCloud error: ${e.message}")
            false
        }
    }

    /**
     * 2. Fetch all registered players from the cloud directory so phone 2 can find phone 1's ID.
     */
    suspend fun fetchAllUsersFromCloud(): List<CricHeroesProfile> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/$USER_REGISTRY_OBJ_ID")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                response.close()
                return@withContext emptyList()
            }

            val bodyStr = response.body?.string() ?: ""
            response.close()

            if (bodyStr.isBlank()) return@withContext emptyList()
            val root = JSONObject(bodyStr)
            val dataObj = root.optJSONObject("data") ?: return@withContext emptyList()
            val usersArray = dataObj.optJSONArray("users") ?: return@withContext emptyList()

            val result = mutableListOf<CricHeroesProfile>()
            for (i in 0 until usersArray.length()) {
                val u = usersArray.optJSONObject(i) ?: continue
                result.add(
                    CricHeroesProfile(
                        id = u.optString("id", "player_$i"),
                        username = u.optString("username", ""),
                        mobileNumber = u.optString("mobileNumber", ""),
                        fullName = u.optString("fullName", ""),
                        jerseyName = u.optString("jerseyName", ""),
                        jerseyNumber = u.optInt("jerseyNumber", 0),
                        primaryRole = u.optString("primaryRole", "All-Rounder"),
                        battingStyle = u.optString("battingStyle", "Right-hand Bat"),
                        bowlingStyle = u.optString("bowlingStyle", "Right-arm Medium"),
                        teamName = u.optString("teamName", ""),
                        city = u.optString("city", ""),
                        avatarEmoji = u.optString("avatarEmoji", "🏏"),
                        badgeTitle = u.optString("badgeTitle", "AYUUCRIC PLAYER"),
                        matchesPlayed = u.optInt("matchesPlayed", 0),
                        runs = u.optInt("runs", 0),
                        wickets = u.optInt("wickets", 0),
                        strikeRate = u.optDouble("strikeRate", 0.0),
                        isVerified = u.optBoolean("isVerified", true),
                        isOnline = u.optBoolean("isOnline", true)
                    )
                )
            }
            result
        } catch (e: Exception) {
            Log.w(TAG, "fetchAllUsersFromCloud error: ${e.message}")
            emptyList()
        }
    }

    /**
     * 3. Send message to Cloud Messages Hub (Cross-Device 1-on-1 DM & Match Chat)
     */
    suspend fun sendCloudMessage(
        roomId: String,
        senderUsername: String,
        recipientUsername: String,
        senderName: String,
        senderRole: String,
        avatarEmoji: String,
        message: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (message.isBlank()) return@withContext false
        try {
            val existing = fetchAllCloudMessages().toMutableList()
            val newMsg = CloudMessageDto(
                id = java.util.UUID.randomUUID().toString(),
                roomId = roomId,
                senderUsername = senderUsername,
                recipientUsername = recipientUsername,
                senderName = senderName,
                senderRole = senderRole,
                avatarEmoji = avatarEmoji,
                message = message.trim(),
                timestamp = System.currentTimeMillis()
            )
            existing.add(newMsg)

            val msgsJsonArray = JSONArray()
            // Keep the latest 100 messages for speed and storage limits
            existing.takeLast(100).forEach { m ->
                val obj = JSONObject().apply {
                    put("id", m.id)
                    put("roomId", m.roomId)
                    put("senderUsername", m.senderUsername)
                    put("recipientUsername", m.recipientUsername)
                    put("senderName", m.senderName)
                    put("senderRole", m.senderRole)
                    put("avatarEmoji", m.avatarEmoji)
                    put("message", m.message)
                    put("timestamp", m.timestamp)
                }
                msgsJsonArray.put(obj)
            }

            val payload = JSONObject().apply {
                put("name", "AyuuCric_Global_Messages_V1")
                put("data", JSONObject().apply {
                    put("messages", msgsJsonArray)
                    put("lastUpdated", System.currentTimeMillis())
                })
            }

            val request = Request.Builder()
                .url("$BASE_URL/$MESSAGES_HUB_OBJ_ID")
                .put(payload.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val ok = response.isSuccessful
            response.close()
            Log.d(TAG, "sendCloudMessage: roomId=$roomId -> success=$ok")
            ok
        } catch (e: Exception) {
            Log.w(TAG, "sendCloudMessage error: ${e.message}")
            false
        }
    }

    /**
     * 4. Fetch all cloud messages from the central hub.
     */
    suspend fun fetchAllCloudMessages(): List<CloudMessageDto> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/$MESSAGES_HUB_OBJ_ID")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                response.close()
                return@withContext emptyList()
            }

            val bodyStr = response.body?.string() ?: ""
            response.close()

            if (bodyStr.isBlank()) return@withContext emptyList()
            val root = JSONObject(bodyStr)
            val dataObj = root.optJSONObject("data") ?: return@withContext emptyList()
            val msgsArray = dataObj.optJSONArray("messages") ?: return@withContext emptyList()

            val result = mutableListOf<CloudMessageDto>()
            for (i in 0 until msgsArray.length()) {
                val m = msgsArray.optJSONObject(i) ?: continue
                result.add(
                    CloudMessageDto(
                        id = m.optString("id", "msg_$i"),
                        roomId = m.optString("roomId", ""),
                        senderUsername = m.optString("senderUsername", ""),
                        recipientUsername = m.optString("recipientUsername", ""),
                        senderName = m.optString("senderName", "Player"),
                        senderRole = m.optString("senderRole", "Spectator"),
                        avatarEmoji = m.optString("avatarEmoji", "🏏"),
                        message = m.optString("message", ""),
                        timestamp = m.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            result
        } catch (e: Exception) {
            Log.w(TAG, "fetchAllCloudMessages error: ${e.message}")
            emptyList()
        }
    }

    /**
     * 5. Publish live match state from official scorer phone to cloud so spectators/other phones see it live.
     */
    suspend fun publishLiveMatch(match: MatchEntity, lastDelivery: BallEventEntity? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("name", "AyuuCric_Live_Match_V1")
                put("data", JSONObject().apply {
                    put("matchId", match.id)
                    put("score", match.score)
                    put("wickets", match.wickets)
                    put("legalBalls", match.legalBalls)
                    put("totalOvers", match.totalOvers)
                    put("target", match.target)
                    put("strikerName", match.strikerName)
                    put("strikerRuns", match.strikerRuns)
                    put("strikerBalls", match.strikerBalls)
                    put("strikerFours", match.strikerFours)
                    put("strikerSixes", match.strikerSixes)
                    put("nonStrikerName", match.nonStrikerName)
                    put("nonStrikerRuns", match.nonStrikerRuns)
                    put("nonStrikerBalls", match.nonStrikerBalls)
                    put("bowlerName", match.bowlerName)
                    put("bowlerRuns", match.bowlerRuns)
                    put("bowlerBalls", match.bowlerBalls)
                    put("bowlerWickets", match.bowlerWickets)
                    put("battingTeam", match.battingTeam)
                    put("bowlingTeam", match.bowlingTeam)
                    put("status", match.status)
                    put("lastBallRuns", lastDelivery?.runs ?: 0)
                    put("lastBallExtra", lastDelivery?.extraType ?: "None")
                    put("lastBallCommentary", lastDelivery?.commentary ?: "")
                    put("updatedAt", System.currentTimeMillis())
                })
            }

            val request = Request.Builder()
                .url("$BASE_URL/$LIVE_MATCH_OBJ_ID")
                .put(payload.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val ok = response.isSuccessful
            response.close()
            Log.d(TAG, "publishLiveMatch: ${match.score}/${match.wickets} -> success=$ok")
            ok
        } catch (e: Exception) {
            Log.w(TAG, "publishLiveMatch error: ${e.message}")
            false
        }
    }

    /**
     * 6. Fetch live match state from cloud.
     */
    suspend fun fetchLiveMatchFromCloud(): CloudMatchDto? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/$LIVE_MATCH_OBJ_ID")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                response.close()
                return@withContext null
            }

            val bodyStr = response.body?.string() ?: ""
            response.close()

            if (bodyStr.isBlank()) return@withContext null
            val root = JSONObject(bodyStr)
            val dataObj = root.optJSONObject("data") ?: return@withContext null

            CloudMatchDto(
                matchId = dataObj.optString("matchId", "match_live_1"),
                score = dataObj.optInt("score", 0),
                wickets = dataObj.optInt("wickets", 0),
                legalBalls = dataObj.optInt("legalBalls", 0),
                totalOvers = dataObj.optInt("totalOvers", 10),
                target = dataObj.optInt("target", 0),
                strikerName = dataObj.optString("strikerName", "Striker"),
                strikerRuns = dataObj.optInt("strikerRuns", 0),
                strikerBalls = dataObj.optInt("strikerBalls", 0),
                strikerFours = dataObj.optInt("strikerFours", 0),
                strikerSixes = dataObj.optInt("strikerSixes", 0),
                nonStrikerName = dataObj.optString("nonStrikerName", "Non-Striker"),
                nonStrikerRuns = dataObj.optInt("nonStrikerRuns", 0),
                nonStrikerBalls = dataObj.optInt("nonStrikerBalls", 0),
                bowlerName = dataObj.optString("bowlerName", "Bowler"),
                bowlerRuns = dataObj.optInt("bowlerRuns", 0),
                bowlerBalls = dataObj.optInt("bowlerBalls", 0),
                bowlerWickets = dataObj.optInt("bowlerWickets", 0),
                battingTeam = dataObj.optString("battingTeam", "Team A"),
                bowlingTeam = dataObj.optString("bowlingTeam", "Team B"),
                status = dataObj.optString("status", "LIVE"),
                lastBallRuns = dataObj.optInt("lastBallRuns", 0),
                lastBallExtra = dataObj.optString("lastBallExtra", "None"),
                lastBallCommentary = dataObj.optString("lastBallCommentary", ""),
                updatedAt = dataObj.optLong("updatedAt", 0L)
            )
        } catch (e: Exception) {
            Log.w(TAG, "fetchLiveMatchFromCloud error: ${e.message}")
            null
        }
    }
}
