package com.example.data.chat

import com.example.data.model.ChatMessage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class RealChatRepository {

    private val firestore by lazy { FirebaseFirestore.getInstance() }

    // Consistent room ID based on usernames
    private fun getRoomId(user1: String, user2: String): String {
        val clean1 = user1.trim().lowercase().removePrefix("@").replace(" ", "_")
        val clean2 = user2.trim().lowercase().removePrefix("@").replace(" ", "_")
        return if (clean1 < clean2) "${clean1}_${clean2}" else "${clean2}_${clean1}"
    }

    // 1. Send direct message
    fun sendDirectMessage(
        senderUsername: String,
        senderName: String,
        senderRole: String,
        avatarEmoji: String,
        receiverUsername: String,
        text: String
    ) {
        if (text.isBlank()) return
        val roomId = getRoomId(senderUsername, receiverUsername)

        val messageData = hashMapOf(
            "senderUsername" to senderUsername.trim().removePrefix("@").lowercase(),
            "senderName" to senderName,
            "senderRole" to senderRole,
            "avatarEmoji" to avatarEmoji,
            "receiverUsername" to receiverUsername.trim().removePrefix("@").lowercase(),
            "message" to text.trim(),
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection("direct_chats")
            .document(roomId)
            .collection("messages")
            .add(messageData)
    }

    // 2. Observe direct messages flow
    fun observeDirectMessages(myUsername: String, otherUsername: String): Flow<List<ChatMessage>> = callbackFlow {
        val roomId = getRoomId(myUsername, otherUsername)
        val cleanMy = myUsername.trim().lowercase().removePrefix("@")

        val listener = firestore.collection("direct_chats")
            .document(roomId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    val sender = (doc.getString("senderUsername") ?: "").trim().lowercase().removePrefix("@")
                    ChatMessage(
                        id = doc.id,
                        senderName = doc.getString("senderName") ?: "User",
                        senderRole = doc.getString("senderRole") ?: "PLAYER",
                        avatarEmoji = doc.getString("avatarEmoji") ?: "🏏",
                        message = doc.getString("message") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        isFromMe = sender == cleanMy
                    )
                } ?: emptyList()

                trySend(messages)
            }

        awaitClose { listener.remove() }
    }

    // 3. Send match chat message
    fun sendMatchChatMessage(
        matchId: String,
        senderUsername: String,
        senderName: String,
        senderRole: String,
        avatarEmoji: String,
        text: String
    ) {
        if (text.isBlank()) return

        val messageData = hashMapOf(
            "senderUsername" to senderUsername.trim().removePrefix("@").lowercase(),
            "senderName" to senderName,
            "senderRole" to senderRole,
            "avatarEmoji" to avatarEmoji,
            "message" to text.trim(),
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection("matches")
            .document(matchId)
            .collection("live_chat")
            .add(messageData)
    }

    // 4. Observe match messages flow
    fun observeMatchMessages(matchId: String, myUsername: String): Flow<List<ChatMessage>> = callbackFlow {
        val cleanMy = myUsername.trim().lowercase().removePrefix("@")

        val listener = firestore.collection("matches")
            .document(matchId)
            .collection("live_chat")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    val sender = (doc.getString("senderUsername") ?: "").trim().lowercase().removePrefix("@")
                    ChatMessage(
                        id = doc.id,
                        senderName = doc.getString("senderName") ?: "User",
                        senderRole = doc.getString("senderRole") ?: "PLAYER",
                        avatarEmoji = doc.getString("avatarEmoji") ?: "🏏",
                        message = doc.getString("message") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        isFromMe = sender == cleanMy
                    )
                } ?: emptyList()

                trySend(messages)
            }

        awaitClose { listener.remove() }
    }
}
