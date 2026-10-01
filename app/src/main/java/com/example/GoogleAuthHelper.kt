package com.example

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

private const val TAG = "GoogleAuthHelper"

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

suspend fun signInWithGoogle(
    context: Context,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val activity = context.findActivity()
    val targetContext = activity ?: context
    val credentialManager = CredentialManager.create(targetContext)
    val webClientId = "40080303796-lsnks2d39nshmc6klv7vqcamdvaqq2u0.apps.googleusercontent.com"

    // Use GetGoogleIdOption with setFilterByAuthorizedAccounts(false)
    // so any Google account on the user's phone can be selected,
    // avoiding Error 16 (Cannot find a matching credential) on first login.
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(webClientId)
        .setAutoSelectEnabled(false)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    try {
        val result = credentialManager.getCredential(context = targetContext, request = request)
        val credential = result.credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser
            if (currentUser != null && currentUser.isAnonymous) {
                try {
                    currentUser.linkWithCredential(authCredential).await()
                    Log.d(TAG, "Guest account successfully linked with Google! UID: ${currentUser.uid}")
                    onSuccess()
                } catch (collision: com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                    Log.i(TAG, "Google account already exists, signing in directly: ${collision.message}")
                    auth.signInWithCredential(authCredential).await()
                    onSuccess()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed linking Google credential, falling back to sign-in: ${e.message}")
                    auth.signInWithCredential(authCredential).await()
                    onSuccess()
                }
            } else {
                auth.signInWithCredential(authCredential).await()
                onSuccess()
            }
        } else {
            onError("Unsupported credential type received.")
        }
    } catch (e: GetCredentialCancellationException) {
        Log.d(TAG, "User canceled Google sign in prompt")
        onError("Sign-in canceled")
    } catch (e: NoCredentialException) {
        Log.w(TAG, "No credential found: ${e.message}")
        onError("Device par koi Google account nahi mila. Kripya phone me Google account add karein ya SMS OTP use karein.")
    } catch (e: GetCredentialException) {
        Log.w(TAG, "GetCredentialException: ${e.message}", e)
        val msg = e.message ?: ""
        if (msg.contains("16") || msg.contains("Cannot find a matching credential", ignoreCase = true)) {
            onError("Google account select nahi hua ya account device par login nahi hai. Phone settings me Google account check karein.")
        } else {
            onError("Google Sign-In error: ${e.localizedMessage ?: "Failed to authenticate"}")
        }
    } catch (e: Exception) {
        Log.e(TAG, "Firebase authentication failed: ${e.message}", e)
        val raw = e.localizedMessage ?: "Failed to authenticate"
        val friendly = when {
            raw.contains("malformed or has expired", ignoreCase = true) || raw.contains("expired", ignoreCase = true) ->
                "Google sign-in session expired. Please tap 'Continue with Google' again."
            raw.contains("network", ignoreCase = true) ->
                "Network error during Google sign-in. Please check your connection."
            else -> raw
        }
        onError(friendly)
    }
}

