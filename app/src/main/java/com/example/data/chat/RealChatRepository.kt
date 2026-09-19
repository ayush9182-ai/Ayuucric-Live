package com.example.data.chat

import com.example.data.model.ChatMessage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class RealChatRepository {

    private val firestore = FirebaseFirestore.getInstance()

    // Consistent room ID based on phone numbers
    private fun getRoomId(phone1: String, phone2: String): String {
        return if (phone1 < phone2) "${phone1}_${phone2}" else "${phone2}_${phone1}"
    }

    // 1. Real message bhejna
    fun sendDirectMessage(
        senderPhone: String,
        senderName: String,
        senderRole: String,
        avatarEmoji: String,
        receiverPhone: String,
        text: String
    ) {
        if (text.isBlank()) return
        val roomId = getRoomId(senderPhone, receiverPhone)

        val messageData = hashMapOf(
            "senderPhone" to senderPhone,
            "senderName" to senderName,
            "senderRole" to senderRole,
            "avatarEmoji" to avatarEmoji,
            "receiverPhone" to receiverPhone,
            "message" to text.trim(),
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection("direct_chats")
            .document(roomId)
            .collection("messages")
            .add(messageData)
    }

    // 2. Real-time stream listen karna
    fun observeDirectMessages(myPhone: String, otherPhone: String): Flow<List<ChatMessage>> = callbackFlow {
        val roomId = getRoomId(myPhone, otherPhone)

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
                    val senderPhone = doc.getString("senderPhone") ?: ""
                    ChatMessage(
                        id = doc.id,
                        senderName = doc.getString("senderName") ?: "User",
                        senderRole = doc.getString("senderRole") ?: "PLAYER",
                        avatarEmoji = doc.getString("avatarEmoji") ?: "🏏",
                        message = doc.getString("message") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        isFromMe = senderPhone == myPhone
                    )
                } ?: emptyList()

                trySend(messages)
            }

        awaitClose { listener.remove() }
    }
}
