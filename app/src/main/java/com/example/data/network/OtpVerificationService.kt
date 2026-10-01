package com.example.data.network

import android.content.Context

/**
 * Local OTP generation is intentionally disabled.
 * Real SMS verification must use Firebase Phone Auth only.
 */
object OtpVerificationService {
    @Deprecated("Use Firebase Phone Auth for SMS verification")
    fun sendRealOtp(context: Context, phoneNumber: String): String {
        throw UnsupportedOperationException(
            "Local OTP generation is disabled. Use Firebase Phone Auth only."
        )
    }

    @Deprecated("Use Firebase Phone Auth for SMS verification")
    fun openSmsApp(context: Context, phoneNumber: String, otpCode: String) {
        throw UnsupportedOperationException(
            "SMS compose flow is not authentication. Use Firebase Phone Auth only."
        )
    }
}
