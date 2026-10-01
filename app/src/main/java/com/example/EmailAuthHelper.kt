package com.example

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

object EmailAuthHelper {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    // 1. Naya Account Banana (Sign Up)
    fun signUpWithEmail(
        email: String,
        pass: String,
        onSuccess: (FirebaseUser) -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || pass.isBlank()) {
            onError("Email and password cannot be empty.")
            return
        }
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            onError("Please enter a valid email address.")
            return
        }
        if (pass.length < 6) {
            onError("Password must be at least 6 characters.")
            return
        }

        val currentUser = auth.currentUser
        if (currentUser != null && currentUser.isAnonymous) {
            val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(cleanEmail, pass)
            currentUser.linkWithCredential(credential)
                .addOnCompleteListener { linkTask ->
                    if (linkTask.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null) onSuccess(user) else onError("User registration failed")
                    } else {
                        val ex = linkTask.exception
                        if (ex is com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                            // Email already registered to another account -> sign in instead
                            auth.signInWithEmailAndPassword(cleanEmail, pass)
                                .addOnCompleteListener { signInTask ->
                                    if (signInTask.isSuccessful) {
                                        val user = auth.currentUser
                                        if (user != null) onSuccess(user) else onError("Sign in failed")
                                    } else {
                                        onError("Email already registered. Please sign in with your password.")
                                    }
                                }
                        } else {
                            onError(ex?.localizedMessage ?: "Account linking failed")
                        }
                    }
                }
            return
        }

        auth.createUserWithEmailAndPassword(cleanEmail, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) onSuccess(user) else onError("User registration failed")
                } else {
                    val raw = task.exception?.localizedMessage ?: "Sign up failed"
                    val msg = when {
                        raw.contains("already in use", ignoreCase = true) ->
                            "Email already registered. Click 'Sign In' above to login."
                        raw.contains("badly formatted", ignoreCase = true) ->
                            "Invalid email format. Please check your email."
                        raw.contains("weak-password", ignoreCase = true) ->
                            "Password is too weak. Please use at least 6 characters."
                        raw.contains("network", ignoreCase = true) ->
                            "Network error. Please check your internet connection."
                        else -> raw
                    }
                    onError(msg)
                }
            }
    }

    // 2. Existing Account me Sign In Karna (Login)
    fun signInWithEmail(
        email: String,
        pass: String,
        onSuccess: (FirebaseUser) -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || pass.isBlank()) {
            onError("Please enter your email and password.")
            return
        }

        auth.signInWithEmailAndPassword(cleanEmail, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) onSuccess(user) else onError("Sign in failed")
                } else {
                    val raw = task.exception?.localizedMessage ?: "Invalid credentials"
                    val msg = when {
                        raw.contains("no user record", ignoreCase = true) ||
                                raw.contains("user-not-found", ignoreCase = true) ->
                            "No account found with this email. Click 'Create Account' to register."
                        raw.contains("wrong-password", ignoreCase = true) ||
                                raw.contains("invalid-credential", ignoreCase = true) ||
                                raw.contains("malformed or has expired", ignoreCase = true) ->
                            "Invalid password or email. Please check your credentials."
                        raw.contains("badly formatted", ignoreCase = true) ->
                            "Invalid email address format."
                        raw.contains("network", ignoreCase = true) ->
                            "Network error. Please check your internet connection."
                        else -> raw
                    }
                    onError(msg)
                }
            }
    }

    // 3. Password Reset Email
    fun sendPasswordReset(
        email: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            onError("Please enter a valid email address to reset password.")
            return
        }
        auth.sendPasswordResetEmail(cleanEmail)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    onError(task.exception?.localizedMessage ?: "Failed to send reset email.")
                }
            }
    }

    // 4. Sign Out
    fun signOut() {
        auth.signOut()
    }
}
