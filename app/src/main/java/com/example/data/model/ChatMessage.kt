package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Authoritative ChatMessage data model matching the Cloud Firestore document schema:
 * direct_chats/{threadId}/messages/{messageId}
 */
@IgnoreExtraProperties
data class ChatMessage(
    var id: String = "",
    val message: String = "",
    val senderUid: String = "",
    val recipientUid: String = "",
    val senderUsername: String = "",
    val receiverUsername: String = "",
    val senderName: String = "",
    val senderRole: String = "",
    val avatarEmoji: String = "",
    val status: String = "SENT",
    val timestamp: Long = 0L,
    val expiresAt: Long = 0L,
    val createdAt: Timestamp? = null,
    val mediaUrl: String = "",
    val mediaType: String = "",
    val isSnap: Boolean = false,
    val isSnapOpened: Boolean = false,
    @get:Exclude var isFromMe: Boolean = false,
    @get:Exclude var reaction: String? = null
) {
    @get:Exclude
    val text: String get() = message
}
