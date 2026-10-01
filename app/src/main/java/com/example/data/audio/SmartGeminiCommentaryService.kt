package com.example.data.audio

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.BallEventEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object SmartGeminiCommentaryService {

    private const val TAG = "SmartGeminiCommentary"
    private const val PREFS_NAME = "ayuu_sidhu_voice_prefs"
    private const val KEY_CUSTOM_KEY = "custom_gemini_api_key"
    private const val KEY_HYBRID_MODE = "hybrid_commentary_mode"

    private const val MODEL_NAME = "gemini-2.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    // Fast 2.5-second timeout for instant, zero-delay cricket commentary
    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(2500, TimeUnit.MILLISECONDS)
            .readTimeout(2500, TimeUnit.MILLISECONDS)
            .writeTimeout(2500, TimeUnit.MILLISECONDS)
            .build()
    }

    /**
     * Parses raw input into a list of cleaned API keys.
     * Supports comma-separated, newline-separated, semicolon-separated, or space-separated keys.
     */
    fun parseApiKeys(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        return raw.split(",", "\n", ";", " ")
            .map { it.trim() }
            .filter { it.length >= 10 && !it.equals("MY_GEMINI_API_KEY", ignoreCase = true) }
            .distinct()
    }

    /**
     * Retrieves all available Gemini API keys in priority order:
     * 1. All custom keys entered by the Scorer
     * 2. BuildConfig.GEMINI_API_KEY (if configured and valid)
     */
    fun getAllApiKeys(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rawCustom = prefs.getString(KEY_CUSTOM_KEY, "").orEmpty()
        val customKeys = parseApiKeys(rawCustom)

        val buildKey = try {
            BuildConfig.GEMINI_API_KEY.trim()
        } catch (_: Throwable) {
            ""
        }
        val validBuildKey = if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY" && buildKey != "MY_NEW_API_KEY_DEFAULT_VALUE") {
            buildKey
        } else {
            null
        }

        val allKeys = mutableListOf<String>()
        allKeys.addAll(customKeys)
        if (validBuildKey != null && !allKeys.contains(validBuildKey)) {
            allKeys.add(validBuildKey)
        }
        return allKeys
    }

    fun hasAvailableKeys(context: Context): Boolean {
        return getAllApiKeys(context).isNotEmpty()
    }

    fun getEffectiveApiKey(context: Context): String {
        return getAllApiKeys(context).firstOrNull().orEmpty()
    }

    fun getRawCustomApiKeys(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CUSTOM_KEY, "").orEmpty()
    }

    fun setCustomApiKey(context: Context, key: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CUSTOM_KEY, key.trim()).apply()
    }

    fun isHybridModeEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_HYBRID_MODE, true)
    }

    fun setHybridModeEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_HYBRID_MODE, enabled).apply()
    }

    /**
     * Generates a witty, authentic Navjot Singh Sidhu commentary line via Gemini API.
     * Iterates through all available API keys with automatic failover if any key
     * runs into quota limits (HTTP 429), errors, or timeout.
     * Returns null if all keys fail (triggers instant in-built fallback).
     */
    suspend fun generateSidhuAiCommentary(
        context: Context,
        ball: BallEventEntity,
        strikerName: String,
        bowlerName: String,
        currentScore: String,
        style: SidhuVoiceStyle = SidhuVoiceStyle.ENERGETIC_JOSH,
        isHatTrick: Boolean = false,
        isTurningPoint: Boolean = false,
        specialSituation: String = ""
    ): String? = withContext(Dispatchers.IO) {
        val apiKeys = getAllApiKeys(context)
        if (apiKeys.isEmpty()) {
            return@withContext null
        }

        val outcome = when {
            isHatTrick -> "HISTORIC HAT-TRICK! (3 wickets in 3 balls by $bowlerName!)"
            ball.isWicket -> "OUT! (${ball.wicketType})"
            ball.runs == 6 -> "CHHAKKA! Gagan-chumbi 6 Runs!"
            ball.runs == 4 -> "CHAUKA! Bullet shot 4 Runs!"
            ball.runs == 0 -> "DOT BALL! Tight bowling!"
            else -> "${ball.runs} Run(s)"
        }

        val situationNote = if (isHatTrick) {
            "⚡ MOMENT: THIS IS A HISTORIC HAT-TRICK! SHOUT at the top of your voice: 'ओए गुरु!', 'हैट्रिक!', 'चक दे फट्टे!', 'ठोको ताली!', full stadium screaming excitement!"
        } else if (isTurningPoint) {
            "⚡ MOMENT: CRUCIAL MATCH TURNING POINT! ($specialSituation) Scream with dramatic Sidhu excitement and suspense!"
        } else ""

        val prompt = """
You are the legendary Indian cricket commentator Navjot Singh Sidhu ("Sidhu Paaji").
Give an authentic, hilarious, high-energy live commentary punchline in Hindi/Hinglish for this ball!

Ball Context:
- Batsman on strike: $strikerName
- Bowler: $bowlerName
- Result: $outcome
- Current Match Score: $currentScore
- Commentary Style: ${style.label}
$situationNote

Strict Rules:
1. Speak in Sidhu Paaji's real style with classic Sidhu-isms: "ओए गुरु!", "ठोको ताली!", "चक दे फट्टे!", funny metaphors, rhyming punchlines.
2. If turning point or hat-trick or wicket, shout with intense excitement!
3. Keep it strictly between 1 to 2 short sentences (maximum 20 words) for instant spoken commentary.
4. Do NOT use markdown, quotes, asterisks or bullet points. Output only the pure spoken dialogue text in Hindi/Hinglish.
        """.trimIndent()

        val requestJson = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val partsArray = JSONArray().apply {
                        val partObj = JSONObject().apply {
                            put("text", prompt)
                        }
                        put(partObj)
                    }
                    put("parts", partsArray)
                }
                put(contentObj)
            }
            put("contents", contentsArray)

            val genConfig = JSONObject().apply {
                put("temperature", 0.85)
                put("maxOutputTokens", 60)
            }
            put("generationConfig", genConfig)
        }

        val requestBody = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        // Iterate through all configured keys for seamless multi-key failover
        for ((index, key) in apiKeys.withIndex()) {
            try {
                val url = "$BASE_URL?key=$key"
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.w(TAG, "Gemini key #${index + 1} failed with HTTP ${response.code}. Failing over to next key...")
                        return@use // continue to next key
                    }

                    val responseStr = response.body?.string() ?: return@use
                    val root = JSONObject(responseStr)
                    val candidates = root.optJSONArray("candidates") ?: return@use
                    if (candidates.length() == 0) return@use

                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content") ?: return@use
                    val parts = content.optJSONArray("parts") ?: return@use
                    if (parts.length() == 0) return@use

                    val rawText = parts.getJSONObject(0).optString("text", "").trim()
                    if (rawText.isNotBlank()) {
                        val cleaned = rawText
                            .replace("*", "")
                            .replace("\"", "")
                            .replace("\n", " ")
                            .trim()
                        Log.d(TAG, "Gemini Sidhu commentary generated via key #${index + 1}: $cleaned")
                        return@withContext cleaned
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gemini key #${index + 1} call failed (${e.message}). Failing over...")
            }
        }

        Log.w(TAG, "All configured Gemini keys failed or timed out. Falling back to built-in Sidhu Paaji engine.")
        return@withContext null
    }

    /**
     * Tests all entered API keys and returns a clear status report.
     */
    suspend fun testAllApiKeys(rawKeys: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val keys = parseApiKeys(rawKeys)
        if (keys.isEmpty()) {
            return@withContext Pair(false, "Koi valid API key nahi mili! Kripya Gemini key paste karein.")
        }

        var successCount = 0
        val details = mutableListOf<String>()

        for ((i, key) in keys.withIndex()) {
            try {
                val testJson = JSONObject().apply {
                    val contentsArray = JSONArray().apply {
                        val contentObj = JSONObject().apply {
                            val partsArray = JSONArray().apply {
                                put(JSONObject().put("text", "Confirm readiness for AyuuCric live commentary in one word: Ready!"))
                            }
                            put("parts", partsArray)
                        }
                        put(contentObj)
                    }
                    put("contents", contentsArray)
                }
                val body = testJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val url = "$BASE_URL?key=$key"
                val request = Request.Builder().url(url).post(body).build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        successCount++
                        details.add("Key #${i + 1}: Active ✅")
                    } else {
                        details.add("Key #${i + 1}: Error ${response.code} ❌")
                    }
                }
            } catch (e: Exception) {
                details.add("Key #${i + 1}: Timeout ❌")
            }
        }

        val isAnySuccess = successCount > 0
        val summary = if (successCount == keys.size) {
            "✅ Sabhi ${keys.size} Gemini Keys active hain! Auto-Failover poori tarah ready hai! ⚡"
        } else if (successCount > 0) {
            "⚠️ $successCount/${keys.size} Keys active hain (${details.joinToString(", ")}). Failover kaam karega!"
        } else {
            "❌ Sabhi ${keys.size} Keys test fail hui (${details.joinToString(", ")}). Kripya check karein."
        }

        Pair(isAnySuccess, summary)
    }
}
