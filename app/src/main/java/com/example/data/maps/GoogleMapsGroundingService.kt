package com.example.data.maps

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class GroundVenueInfo(
    val name: String,
    val cleanAddress: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val mapsUrl: String = "",
    val pitchType: String = "Grass Turf Pitch",
    val description: String = "",
    val isVerifiedOnMaps: Boolean = true
)

object GoogleMapsGroundingService {

    private const val TAG = "GoogleMapsGrounding"
    private const val GEMINI_MODEL = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$GEMINI_MODEL:generateContent"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    /**
     * Curated local & popular cricket venues used for instant offline suggestions or fallbacks.
     */
    val POPULAR_GROUNDS = listOf(
        GroundVenueInfo(
            name = "Ground Turf Arena",
            cleanAddress = "Sector 7 Sports Complex, Main Turf Ground",
            latitude = 28.5355,
            longitude = 77.3910,
            pitchType = "AstroTurf Box Pitch",
            description = "High-quality turf pitch with floodlights for day-night matches.",
            isVerifiedOnMaps = true
        ),
        GroundVenueInfo(
            name = "Shivaji Park Cricket Ground",
            cleanAddress = "Dadar West, Mumbai, Maharashtra 400028",
            latitude = 19.0270,
            longitude = 72.8397,
            pitchType = "Natural Red Clay Turf",
            description = "Iconic cradle of Indian cricket with 8 standard practice pitches.",
            isVerifiedOnMaps = true
        ),
        GroundVenueInfo(
            name = "Arun Jaitley Stadium Ground",
            cleanAddress = "Bahadur Shah Zafar Marg, New Delhi, Delhi 110002",
            latitude = 28.6378,
            longitude = 77.2424,
            pitchType = "BCCI Certified Grass Turf",
            description = "International cricket stadium with pavilion stands & LED floodlights.",
            isVerifiedOnMaps = true
        ),
        GroundVenueInfo(
            name = "Sector 62 Sports Complex Turf",
            cleanAddress = "Block B, Industrial Area, Sector 62, Noida, UP 201309",
            latitude = 28.6258,
            longitude = 77.3653,
            pitchType = "Professional Box Cricket Turf",
            description = "Enclosed synthetic turf with bounce matting & scoreboard.",
            isVerifiedOnMaps = true
        ),
        GroundVenueInfo(
            name = "M. Chinnaswamy Stadium",
            cleanAddress = "MG Road, Cubbon Park, Bengaluru, Karnataka 560001",
            latitude = 12.9788,
            longitude = 77.5996,
            pitchType = "Sub-Air Fast Drainage Turf",
            description = "World-class stadium famous for electrifying high-scoring encounters.",
            isVerifiedOnMaps = true
        ),
        GroundVenueInfo(
            name = "Wankhede Cricket Stadium",
            cleanAddress = "Vinoo Mankad Road, Churchgate, Mumbai, Maharashtra 400020",
            latitude = 18.9389,
            longitude = 72.8258,
            pitchType = "True Bounce Red Soil Turf",
            description = "Historic seaside ground hosting premier world tournament finals.",
            isVerifiedOnMaps = true
        )
    )

    /**
     * Checks if a ground name appears weird, messy, coordinates, or excessively technical.
     */
    fun isWeirdVenueName(name: String): Boolean {
        val clean = name.trim().lowercase()
        if (clean.isBlank()) return true
        if (clean.length > 45) return true
        if (clean.matches(Regex(".*\\d+\\.\\d{3,}.*"))) return true // Lat/Long numbers
        if (clean.contains("unnamed") || clean.contains("plus code") || clean.contains("unknown")) return true
        if (clean.contains("null") || clean.contains("undefined") || clean.contains("near pole")) return true
        if (clean.contains("plot no") || clean.contains("khasra") || clean.contains("behind shop")) return true
        return false
    }

    /**
     * Automatically suggests a clean, concise, punchy venue name from an address or messy name.
     */
    fun suggestCleanFriendlyName(rawName: String, address: String = ""): String {
        var candidate = rawName.trim()
        if (candidate.isBlank() && address.isNotBlank()) {
            candidate = address.split(",").firstOrNull()?.trim() ?: "Cricket Ground"
        }
        // Remove technical coordinate tags or excess details
        candidate = candidate.replace(Regex("\\b\\d+\\.\\d+,\\s*\\d+\\.\\d+\\b"), "")
        candidate = candidate.replace(Regex("(?i)unnamed road"), "Turf Ground")
        candidate = candidate.replace(Regex("(?i)plot\\s*\\d+"), "Cricket Turf")
        candidate = candidate.trim().trim(',', '-', '•')
        if (candidate.length > 32) {
            candidate = candidate.take(30).trimEnd() + "..."
        }
        return candidate.ifBlank { "Local Cricket Ground" }
    }

    /**
     * Searches cricket grounds, stadiums and turfs using Google Maps Grounding via Gemini 3.5 Flash
     * with tool: {"googleMaps": {}}
     */
    suspend fun searchGroundsWithMapsGrounding(
        query: String,
        customApiKey: String? = null
    ): List<GroundVenueInfo> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) {
            return@withContext POPULAR_GROUNDS
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
                val results = callGeminiWithGoogleMapsTool(cleanQuery, apiKey)
                if (results.isNotEmpty()) {
                    return@withContext results
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gemini Google Maps Grounding error: ${e.message}")
            }
        }

        // Filter local catalog or return customized match
        val filtered = POPULAR_GROUNDS.filter {
            it.name.contains(cleanQuery, ignoreCase = true) ||
            it.cleanAddress.contains(cleanQuery, ignoreCase = true)
        }

        if (filtered.isNotEmpty()) {
            return@withContext filtered
        }

        // Return a clean synthesized ground entry with the user query
        return@withContext listOf(
            GroundVenueInfo(
                name = cleanQuery.replaceFirstChar { it.uppercaseChar() },
                cleanAddress = "$cleanQuery, Local Area",
                pitchType = "Standard Cricket Turf",
                description = "Custom cricket ground for this match.",
                isVerifiedOnMaps = false
            )
        ) + POPULAR_GROUNDS.take(3)
    }

    /**
     * Calls Gemini 3.5 Flash using direct REST API with the official "googleMaps" tool
     * to perform Grounding with Google Maps.
     */
    private fun callGeminiWithGoogleMapsTool(query: String, apiKey: String): List<GroundVenueInfo> {
        val jsonRequest = JSONObject().apply {
            // System instructions
            val systemInstruction = JSONObject().apply {
                val sysParts = JSONArray()
                sysParts.put(
                    JSONObject().put(
                        "text",
                        "You are Google Maps Grounding Engine for AyuuCric Live Cricket Studio. " +
                        "Use the Google Maps tool to ground location data. " +
                        "When user searches for a cricket ground, stadium, sports complex, or turf, " +
                        "find accurate real-world places from Google Maps. " +
                        "Return a structured JSON array with up to 4 grounds: name, address, latitude, longitude, pitchType, description, mapsUrl."
                    )
                )
                put("parts", sysParts)
            }
            put("systemInstruction", systemInstruction)

            // Prompt content
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            partsArray.put(
                JSONObject().put(
                    "text",
                    "Find real cricket grounds, stadiums, or sports turfs near or matching: '$query'. " +
                    "Use Google Maps grounding to fetch exact place name, clean postal address, coordinates, and pitch type. " +
                    "Respond with a JSON array format:\n" +
                    "[\n" +
                    "  {\n" +
                    "    \"name\": \"Clean Ground Name\",\n" +
                    "    \"address\": \"Full Street Address\",\n" +
                    "    \"latitude\": 28.6139,\n" +
                    "    \"longitude\": 77.2090,\n" +
                    "    \"pitchType\": \"Natural Turf / AstroTurf / Matting\",\n" +
                    "    \"description\": \"Short 1-line description of pitch and facilities\",\n" +
                    "    \"mapsUrl\": \"https://maps.google.com/?q=...\"\n" +
                    "  }\n" +
                    "]"
                )
            )
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            put("contents", contentsArray)

            // MANDATORY Google Maps Grounding Tool
            val toolsArray = JSONArray()
            val mapsTool = JSONObject()
            mapsTool.put("googleMaps", JSONObject())
            toolsArray.put(mapsTool)
            put("tools", toolsArray)
        }

        val requestBody = jsonRequest.toString().toRequestBody("application/json".toMediaType())
        val url = "$BASE_URL?key=$apiKey"
        val request = Request.Builder().url(url).post(requestBody).build()

        val response = okHttpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: return emptyList()

        if (!response.isSuccessful) {
            Log.w(TAG, "Gemini Maps Grounding failed with code ${response.code}: $responseBody")
            return emptyList()
        }

        return parseGroundsFromResponse(responseBody, query)
    }

    /**
     * Parses candidate parts text and groundingMetadata from Gemini API response.
     */
    private fun parseGroundsFromResponse(responseJsonStr: String, fallbackQuery: String): List<GroundVenueInfo> {
        val results = mutableListOf<GroundVenueInfo>()
        try {
            val root = JSONObject(responseJsonStr)
            val candidates = root.optJSONArray("candidates") ?: return emptyList()
            if (candidates.length() == 0) return emptyList()

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            var rawText = ""
            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val p = parts.getJSONObject(i)
                    rawText += p.optString("text", "") + "\n"
                }
            }

            // Extract Google Maps URLs or Grounding Chunks if present in groundingMetadata
            val groundingMetadata = firstCandidate.optJSONObject("groundingMetadata")
            val groundingChunks = groundingMetadata?.optJSONArray("groundingChunks")
            val extractedMapsUrls = mutableListOf<String>()
            if (groundingChunks != null) {
                for (i in 0 until groundingChunks.length()) {
                    val chunk = groundingChunks.optJSONObject(i)
                    val web = chunk?.optJSONObject("web")
                    val uri = web?.optString("uri", "") ?: ""
                    if (uri.isNotBlank()) extractedMapsUrls.add(uri)
                    val maps = chunk?.optJSONObject("maps")
                    val mapUri = maps?.optString("uri", "") ?: ""
                    if (mapUri.isNotBlank()) extractedMapsUrls.add(mapUri)
                }
            }

            // Look for JSON Array in response
            val startIdx = rawText.indexOf('[')
            val endIdx = rawText.lastIndexOf(']')
            if (startIdx != -1 && endIdx > startIdx) {
                val jsonArrayStr = rawText.substring(startIdx, endIdx + 1)
                val jsonArr = JSONArray(jsonArrayStr)
                for (i in 0 until jsonArr.length()) {
                    val item = jsonArr.optJSONObject(i) ?: continue
                    val name = item.optString("name", "").trim()
                    if (name.isBlank()) continue
                    val address = item.optString("address", "").trim()
                    val lat = item.optDouble("latitude", Double.NaN).let { if (it.isNaN()) null else it }
                    val lng = item.optDouble("longitude", Double.NaN).let { if (it.isNaN()) null else it }
                    val pitch = item.optString("pitchType", "Cricket Turf Pitch").trim()
                    val desc = item.optString("description", "").trim()
                    val mapUrl = item.optString("mapsUrl", "").ifBlank {
                        extractedMapsUrls.getOrNull(i) ?: "https://www.google.com/maps/search/?api=1&query=${URLEncoder.encode("$name $address", "UTF-8")}"
                    }

                    results.add(
                        GroundVenueInfo(
                            name = name,
                            cleanAddress = address.ifBlank { "$name, Cricket Ground" },
                            latitude = lat,
                            longitude = lng,
                            mapsUrl = mapUrl,
                            pitchType = pitch,
                            description = desc,
                            isVerifiedOnMaps = true
                        )
                    )
                }
            }

            // If JSON array parsing didn't find items, attempt line-based parsing
            if (results.isEmpty() && rawText.isNotBlank()) {
                val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
                var currentName = ""
                for (line in lines) {
                    val cleanLine = line.removePrefix("*").removePrefix("-").removePrefix("•").trim()
                    if (cleanLine.contains("ground", ignoreCase = true) ||
                        cleanLine.contains("stadium", ignoreCase = true) ||
                        cleanLine.contains("turf", ignoreCase = true) ||
                        cleanLine.contains("complex", ignoreCase = true)
                    ) {
                        currentName = cleanLine.split(":", "-", "(").firstOrNull()?.trim() ?: cleanLine
                        if (currentName.length in 4..45) {
                            results.add(
                                GroundVenueInfo(
                                    name = currentName,
                                    cleanAddress = "$currentName, Sports Area",
                                    pitchType = "Grass Turf Pitch",
                                    description = "Google Maps Grounding location",
                                    isVerifiedOnMaps = true,
                                    mapsUrl = "https://www.google.com/maps/search/?api=1&query=${URLEncoder.encode(currentName, "UTF-8")}"
                                )
                            )
                        }
                    }
                    if (results.size >= 4) break
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "parseGroundsFromResponse error: ${e.message}")
        }
        return results
    }

    /**
     * Opens Google Maps for the specified venue and address using Android Intent.
     */
    fun openGoogleMapsForVenue(
        context: Context,
        venue: String,
        address: String = "",
        coordinates: String = ""
    ) {
        try {
            val queryText = when {
                coordinates.isNotBlank() && coordinates.contains(",") -> coordinates.trim()
                address.isNotBlank() -> "$venue, $address"
                else -> "$venue cricket ground"
            }
            val encodedQuery = URLEncoder.encode(queryText, "UTF-8")
            val mapsUri = if (coordinates.isNotBlank() && coordinates.contains(",")) {
                Uri.parse("geo:${coordinates.trim()}?q=${encodedQuery}")
            } else {
                Uri.parse("https://www.google.com/maps/search/?api=1&query=${encodedQuery}")
            }

            val mapIntent = Intent(Intent.ACTION_VIEW, mapsUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(mapIntent)
        } catch (e: Exception) {
            Log.w(TAG, "Could not open Google Maps app: ${e.message}")
            try {
                val browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${URLEncoder.encode("$venue $address", "UTF-8")}")
                val browserIntent = Intent(Intent.ACTION_VIEW, browserUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(browserIntent)
            } catch (_: Exception) {}
        }
    }
}
