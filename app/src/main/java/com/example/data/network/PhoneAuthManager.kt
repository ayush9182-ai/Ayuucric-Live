package com.example.data.network

import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class PhoneAuthManager(private val activity: Activity) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private var verificationId: String? = null

    // Real SMS OTP trigger karta hai
    fun sendOtp(
        phoneNumber: String, // Format: "+919876543210"
        onCodeSent: () -> Unit,
        onError: (String) -> Unit
    ) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onCodeSent(
                    vId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    verificationId = vId
                    onCodeSent()
                }

                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // Agar phone ne SMS auto-read kar liya
                    signIn(credential, onSuccess = {}, onError = {})
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    onError(e.localizedMessage ?: "OTP send failed. SHA-1 verify karein.")
                }
            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    // User ka dala hua OTP verify karta hai
    fun verifyOtp(
        otpCode: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val vId = verificationId
        if (vId == null) {
            onError("Pehle OTP request karein")
            return
        }

        val credential = PhoneAuthProvider.getCredential(vId, otpCode)
        signIn(credential, onSuccess, onError)
    }

    private fun signIn(
        credential: PhoneAuthCredential,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(activity) { task ->
                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    onError(task.exception?.localizedMessage ?: "Galat OTP code")
                }
            }
    }
}
