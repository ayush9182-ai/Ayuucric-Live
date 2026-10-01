package com.example

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object SmsOtpService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // Fast2SMS API Key
    private const val API_KEY = "jx7g0vJTBGNfCnwoWFy9ZOlpc4MA2esUKH1bmt65SLkhPuY8aIcodGw7jWhvDNayK8PFsBlenbSM2T4J"

    suspend fun sendOtp(mobileNumber: String, otpCode: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Number se spaces aur +91 trim karo
            val cleanPhone = mobileNumber.replace("+91", "").replace(" ", "").trim()

            val jsonBody = JSONObject().apply {
                put("route", "otp")
                put("variables_values", otpCode)
                put("numbers", cleanPhone)
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://www.fast2sms.com/dev/bulkV2")
                .addHeader("authorization", API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
