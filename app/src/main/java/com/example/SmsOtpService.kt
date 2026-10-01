package com.example

import android.content.Context

/**
 * External SMS services and local OTP generation are disabled.
 * Real authentication must use Firebase Phone Auth only.
 */
@Deprecated("Use Firebase Phone Auth for SMS verification only")
object SmsOtpService {
    suspend fun sendOtp(mobileNumber: String, otpCode: String): Boolean {
        throw UnsupportedOperationException(
            "External SMS is disabled. Use Firebase Phone Auth instead."
        )
    }
}
