package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ChatMessage
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChatViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var snapshotListener: ListenerRegistration? = null
    private var currentThreadId: String = ""

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    fun listenToMessages(threadId: String) {
        if (threadId.isBlank()) return
        if (threadId == currentThreadId && snapshotListener != null) return

        snapshotListener?.remove()
        currentThreadId = threadId

        val currentUid = try { auth.currentUser?.uid.orEmpty() } catch (_: Throwable) { "" }

        firestore.collection("direct_chats")
            .document(threadId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatDebug", "Firestore listen error: ${error.message}", error)
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val messageList = snapshot.documents.mapNotNull { doc ->
                        parseDocSafely(doc, currentUid)
                    }
                    _messages.value = messageList
                } else {
                    _messages.value = emptyList()
                }
            }
    }

    fun sendMessage(
        threadId: String,
        text: String,
        recipientUid: String,
        recipientUsername: String = "",
        senderUsername: String = "",
        senderName: String = "",
        senderRole: String = "Fan",
        avatarEmoji: String = "🏏"
    ) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || threadId.isBlank()) return

        val myUid = auth.currentUser?.uid.orEmpty()
        val now = System.currentTimeMillis()
        val expiresAt = now + (30L * 24 * 60 * 60 * 1000L) // 30-day retention

        viewModelScope.launch {
            _isSending.value = true
            try {
                // Ensure parent direct_chats document exists for security rules threadMember check
                val threadSummary = hashMapOf(
                    "threadId" to threadId,
                    "participants" to listOf(myUid, recipientUid).filter { it.isNotBlank() },
                    "participantUsernames" to listOf(senderUsername, recipientUsername).filter { it.isNotBlank() },
                    "lastMessage" to trimmed,
                    "lastMessageAt" to now,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                firestore.collection("direct_chats")
                    .document(threadId)
                    .set(threadSummary, SetOptions.merge())
                    .await()

                val messageData = hashMapOf(
                    "message" to trimmed,
                    "senderUid" to myUid,
                    "recipientUid" to recipientUid,
                    "senderUsername" to senderUsername.trim().removePrefix("@").lowercase(),
                    "receiverUsername" to recipientUsername.trim().removePrefix("@").lowercase(),
                    "senderName" to senderName.ifBlank { "User" },
                    "senderRole" to senderRole.ifBlank { "PLAYER" },
                    "avatarEmoji" to avatarEmoji.ifBlank { "🏏" },
                    "status" to "SENT",
                    "timestamp" to now,
                    "expiresAt" to expiresAt,
                    "createdAt" to FieldValue.serverTimestamp()
                )

                firestore.collection("direct_chats")
                    .document(threadId)
                    .collection("messages")
                    .add(messageData)
                    .await()
            } catch (e: Exception) {
                Log.e("ChatDebug", "Failed to send message: ${e.message}", e)
            } finally {
                _isSending.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        snapshotListener?.remove()
        snapshotListener = null
    }

    companion object {
        /**
         * Safely parse a DocumentSnapshot to ChatMessage without throwing ClassCastException
         * even if fields were stored as Timestamp, Long, Double, or alternate names.
         */
        fun parseDocSafely(doc: DocumentSnapshot, currentUid: String = ""): ChatMessage? {
            return try {
                val parsed = doc.toObject(ChatMessage::class.java)
                if (parsed != null) {
                    val ts = if (parsed.timestamp > 0L) {
                        parsed.timestamp
                    } else {
                        parsed.createdAt?.toDate()?.time ?: 0L
                    }
                    val senderUid = parsed.senderUid
                    val isFromMe = (senderUid.isNotBlank() && currentUid.isNotBlank() && senderUid == currentUid)
                    parsed.copy(
                        id = doc.id,
                        timestamp = if (ts > 0L) ts else System.currentTimeMillis(),
                        isFromMe = isFromMe
                    )
                } else null
            } catch (e: Exception) {
                Log.w("ChatDebug", "toObject threw ClassCastException / parsing error for doc ${doc.id}: ${e.message}, falling back to safe manual parser")
                try {
                    val ts = when (val rawTs = doc.get("timestamp")) {
                        is Number -> rawTs.toLong()
                        is Timestamp -> rawTs.toDate().time
                        else -> {
                            when (val rawCreated = doc.get("createdAt")) {
                                is Timestamp -> rawCreated.toDate().time
                                is Number -> rawCreated.toLong()
                                else -> System.currentTimeMillis()
                            }
                        }
                    }
                    val expiresAt = when (val rawExp = doc.get("expiresAt")) {
                        is Number -> rawExp.toLong()
                        is Timestamp -> rawExp.toDate().time
                        else -> 0L
                    }
                    val senderUid = doc.getString("senderUid").orEmpty()
                    val isFromMe = (senderUid.isNotBlank() && currentUid.isNotBlank() && senderUid == currentUid)

                    ChatMessage(
                        id = doc.id,
                        message = doc.getString("message") ?: doc.getString("text").orEmpty(),
                        senderUid = senderUid,
                        recipientUid = doc.getString("recipientUid").orEmpty(),
                        senderUsername = doc.getString("senderUsername").orEmpty(),
                        receiverUsername = doc.getString("receiverUsername").orEmpty(),
                        senderName = doc.getString("senderName") ?: "User",
                        senderRole = doc.getString("senderRole") ?: "PLAYER",
                        avatarEmoji = doc.getString("avatarEmoji") ?: "🏏",
                        status = doc.getString("status") ?: "SENT",
                        timestamp = ts,
                        expiresAt = expiresAt,
                        createdAt = doc.getTimestamp("createdAt"),
                        mediaUrl = doc.getString("mediaUrl").orEmpty(),
                        mediaType = doc.getString("mediaType").orEmpty(),
                        isSnap = doc.getBoolean("isSnap") ?: false,
                        isSnapOpened = doc.getBoolean("isSnapOpened") ?: false,
                        isFromMe = isFromMe
                    )
                } catch (ex: Exception) {
                    Log.e("ChatDebug", "Safe fallback parser also failed for doc ${doc.id}", ex)
                    null
                }
            }
        }
    }
}
