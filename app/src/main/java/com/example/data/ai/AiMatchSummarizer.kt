package com.example.data.ai

import com.example.BuildConfig
import com.example.data.audio.SidhuCommentaryGenerator
import com.example.data.audio.SidhuVoiceStyle
import com.example.data.model.BallEventEntity
import com.example.data.model.MatchEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class MatchSummaryResult(
    val title: String,
    val headline: String,
    val detailedSummary: String,
    val turningPoint: String,
    val topPerformers: String,
    val sidhuShayari: String,
    val isAiGenerated: Boolean,
    val spokenScript: String
)

object AiMatchSummarizer {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun generateSummary(
        match: MatchEntity,
        balls: List<BallEventEntity>,
        style: SidhuVoiceStyle = SidhuVoiceStyle.ENERGETIC_JOSH,
        customApiKey: String? = null,
        forceOffline: Boolean = false
    ): MatchSummaryResult = withContext(Dispatchers.IO) {
        if (forceOffline) {
            return@withContext generateSmartLocalSummary(match, balls, style)
        }

        val apiKey = if (!customApiKey.isNullOrBlank()) {
            customApiKey.trim()
        } else {
            try {
                BuildConfig.GEMINI_API_KEY
            } catch (_: Exception) {
                ""
            }
        }

        val isValidKey = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"

        if (isValidKey) {
            try {
                val aiResult = callGeminiForSummary(apiKey, match, balls, style)
                if (aiResult != null) {
                    return@withContext aiResult
                }
            } catch (_: Exception) {
                // Seamlessly fall through to smart offline local generator
            }
        }

        // Local Smart Sidhu Paaji Summary Engine
        return@withContext generateSmartLocalSummary(match, balls, style)
    }

    suspend fun testApiKey(apiKey: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext Pair(false, "API key khali hai!")
        try {
            val jsonRequest = JSONObject().apply {
                val contentsArray = JSONArray()
                val contentObj = JSONObject()
                val partsArray = JSONArray()
                partsArray.put(JSONObject().put("text", "Say: Sidhu Paaji Ready!"))
                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
                put("contents", contentsArray)
            }
            val requestBody = jsonRequest.toString().toRequestBody("application/json".toMediaType())
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=${apiKey.trim()}"
            val request = Request.Builder().url(url).post(requestBody).build()
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                Pair(true, "Key bilkul sahi hai! Gemini 3.5 Flash connect ho gaya! ⚡")
            } else {
                Pair(false, "Google Error: ${response.code} (Kripya key check karein)")
            }
        } catch (e: Exception) {
            Pair(false, "Network error: ${e.localizedMessage ?: "Connection failed"}")
        }
    }

    private fun callGeminiForSummary(
        apiKey: String,
        match: MatchEntity,
        balls: List<BallEventEntity>,
        style: SidhuVoiceStyle
    ): MatchSummaryResult? {
        val oversStr = "${match.legalBalls / 6}.${match.legalBalls % 6}"
        val recentHighlights = balls.take(15).joinToString(", ") {
            val event = if (it.isWicket) "OUT (${it.wicketType})" else if (it.runs == 6) "SIX!" else if (it.runs == 4) "FOUR" else "${it.runs}r"
            "${it.overNumber}.${it.ballInOver}: $event by ${it.batsman}"
        }

        val prompt = """
            You are Navjot Singh Sidhu ("Sidhu Paaji"), the legendary energetic Indian cricket commentator known for rhyming shayaris, metaphors ("Thoko Taali", "Guru Khataak", "Dandi Ud Gayi"), and witty punchlines.
            Summarize this cricket match for viewers in Hindi/Hinglish in your signature Sidhu Paaji style:
            
            Match: ${match.teamA} vs ${match.teamB}
            Tournament: ${match.tournamentName}
            Current Score: ${match.score}/${match.wickets} in $oversStr overs (Target: ${match.target})
            Match Status: ${match.status} (${match.statusDetail})
            Top Batter: ${match.strikerName} (${match.strikerRuns} runs in ${match.strikerBalls} balls, ${match.strikerFours}x4, ${match.strikerSixes}x6)
            Key Bowler: ${match.bowlerName} (${match.bowlerWickets} wkts, ${match.bowlerRuns} runs)
            Recent Deliveries: $recentHighlights
            
            Return a JSON object with these exact keys:
            {
              "headline": "A short, punchy Sidhu style headline like Oye Guru! Match Mein Lag Gayi Aag!",
              "detailedSummary": "A 3-4 sentence thrilling recap of the match flow in energetic Hinglish.",
              "turningPoint": "What was the biggest turning point (a big over, crucial wicket, or explosive hitting).",
              "topPerformers": "Mention the star players who carried their team on their shoulders.",
              "sidhuShayari": "A classic Sidhu Paaji rhyming couplet (sher) suited for this match climax.",
              "spokenScript": "Write a continuous 4-5 line monologue completely in fluent Devanagari Hindi (हिंदी लिपि) so that Android Text-to-Speech speaks it naturally with perfect pronunciation (e.g. ओए गुरु! ठोको ताली गुरु! मैच का पूरा हाल सुनिए। मैदान पर आज ऐसा गदर मचा कि दिल गार्डन गार्डन हो गया!)."
            }
            Only return valid JSON, no markdown formatting.
        """.trimIndent()

        val jsonRequest = JSONObject().apply {
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            partsArray.put(JSONObject().put("text", prompt))
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            put("contents", contentsArray)

            val genConfig = JSONObject()
            genConfig.put("temperature", 0.7)
            genConfig.put("topP", 0.95)
            put("generationConfig", genConfig)
        }

        val requestBody = jsonRequest.toString().toRequestBody("application/json".toMediaType())
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) return null

        val responseBody = response.body?.string() ?: return null
        val responseJson = JSONObject(responseBody)
        val text = responseJson.optJSONArray("candidates")
            ?.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts")
            ?.optJSONObject(0)
            ?.optString("text") ?: return null

        val cleanedJson = text.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val parsed = JSONObject(cleanedJson)
        return MatchSummaryResult(
            title = "🤖 Gemini AI Sidhu Paaji Match Wrap-up",
            headline = parsed.optString("headline", "Oye Guru! Match Mein Macha Ghadmasan!"),
            detailedSummary = parsed.optString("detailedSummary", "Dono teamon ne aisi takkar di ki darshakon ke ronge khade ho gaye!"),
            turningPoint = parsed.optString("turningPoint", "Jab match critical mod par tha tab boundaries ne rukh palat diya!"),
            topPerformers = parsed.optString("topPerformers", "${match.strikerName} ki dhuandhaar ballebaazi aur ${match.bowlerName} ki kassi hui gendbaazi!"),
            sidhuShayari = parsed.optString("sidhuShayari", "Loha garm tha hathoda maar diya, ballebaaz ne boundary par shor macha diya! Thoko Taali!"),
            isAiGenerated = true,
            spokenScript = parsed.optString("spokenScript", "Guru suniye match ka lekhajokha! Aisi takkar roz nahi milti! Thoko taali!")
        )
    }

    private fun generateSmartLocalSummary(
        match: MatchEntity,
        balls: List<BallEventEntity>,
        style: SidhuVoiceStyle
    ): MatchSummaryResult {
        val overs = "${match.legalBalls / 6}.${match.legalBalls % 6}"
        val boundariesCount = balls.count { it.runs >= 4 }
        val wicketsCount = balls.count { it.isWicket }

        val headline = if (match.wickets >= 7) {
            "Oye Guru! Bowlers Ka Qahar Aur Ballebaazon Ka Sangharsh!"
        } else if (match.score >= 150) {
            "Taram-tar Run Barsaat! Ballebaazon Ne Uda Di Dhajjiyan!"
        } else {
            "Kaante Ki Takkar Guru! Saans Rok Dene Wala Maha Muqabla!"
        }

        val detailedSummary = buildString {
            append("AyuuCric Live par ${match.teamA} aur ${match.teamB} ke beech maha muqabla chala! ")
            append("Scoreboard par ${match.score}/${match.wickets} runs lage hain $overs overs mein. ")
            if (match.status == "FINISHED") {
                append("Match ka faisla ho chuka hai: ${match.statusDetail}! ")
            } else {
                append("Filhal match aisi sthiti mein hai: ${match.statusDetail}! ")
            }
            append("Dono teamon ne sher ki tarah dahaad maari hai, koi bhi haar manne ko taiyar nahi!")
        }

        val turningPoint = if (wicketsCount > 0) {
            "Match ka sabse bada turning point raha wicketon ka girna aur ${match.bowlerName} ke teekhe teer jinhone match ka rukh badla!"
        } else {
            "${match.strikerName} ke vo gagan-chumbi chhakke jinhone bowleron ki line aur length bigad di!"
        }

        val topPerformers = "🌟 Ballebaazi: ${match.strikerName} (${match.strikerRuns} runs, ${match.strikerSixes} Sixes) • 🎯 Gendbaazi: ${match.bowlerName} (${match.bowlerWickets} wkts, eco: ${(match.bowlerRuns / (match.bowlerBalls.coerceAtLeast(1) / 6f)).toInt()})"

        val shayari = when (style) {
            SidhuVoiceStyle.SHAYARI_PUNCH -> "कोशिश करने वालों की कभी हार नहीं होती, और मैदान में शेर कभी घास नहीं खाता! ठोको ताली गुरु!"
            SidhuVoiceStyle.ENERGETIC_JOSH -> "लोहा गर्म था और हथौड़ा मार दिया! मैच में आग लगा दी गुरु, तरम-तर! चक दे फट्टे!"
            SidhuVoiceStyle.TV_BROADCAST -> "क्रिकेट अनिश्चितताओं का खेल है गुरु! जो आख़िरी दम तक लड़ा वही सिकंदर बना! वाह भाई वाह!"
        }

        val spokenScript = buildString {
            append("ओए गुरु! ठोको ताली गुरु! मैच का पूरा हाल सुनिए। ")
            append("${match.teamA} और ${match.teamB} के बीच मैदान पर ज़बरदस्त जंग छिड़ी है! ")
            append("स्कोरबोर्ड पर ${match.score} रन टंग चुके हैं ${match.wickets} विकेट के नुक़सान पर। ")
            if (match.status == "FINISHED") {
                append("फैसला हो चुका है: ${match.statusDetail}। ")
            } else {
                append("मैच बेहद रोमांचक मोड़ पर चल रहा है। ")
            }
            append("${match.strikerName} ने बल्ले से गदर मचाया, और गेंदबाज़ों ने भी पसीने छुड़ा दिए! ")
            append("और सिद्धू पाजी का शेर सुनिए: $shayari ")
            append("चक दे फट्टे, नप दे किल्ली! ठोको ताली!")
        }

        return MatchSummaryResult(
            title = "⚡ AI Sidhu Paaji Match Wrap-up",
            headline = headline,
            detailedSummary = detailedSummary,
            turningPoint = turningPoint,
            topPerformers = topPerformers,
            sidhuShayari = shayari,
            isAiGenerated = false,
            spokenScript = spokenScript
        )
    }
}
