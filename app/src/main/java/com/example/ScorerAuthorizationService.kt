package com.example

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

object ScorerAuthorizationService {
    private val auth = FirebaseAuth.getInstance()

    suspend fun isCurrentUserScorer(): Boolean {
        val user = auth.currentUser ?: return false
        return try {
            val token = user.getIdToken(false).await()
            val claims = token.claims
            val role = claims["role"] as? String
            val admin = claims["admin"] as? Boolean ?: false
            role == "scorer" || admin
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun getCurrentScorerUid(): String? {
        val user = auth.currentUser ?: return null
        return if (isCurrentUserScorer()) user.uid else null
    }
}
