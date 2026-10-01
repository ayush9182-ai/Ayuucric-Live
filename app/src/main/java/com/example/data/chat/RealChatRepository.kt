package com.example.data.chat

import android.util.Log
import com.example.data.model.ChatMessage
import com.example.data.model.DirectPersonalMessage
import com.example.ui.viewmodel.ChatViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Authoritative Firebase Firestore repository for all match live banter and direct messaging (DM).
 * Enforces immutable UIDs, server timestamps, 30-day retention policies, and structured error reporting.
 */
class RealChatRepository {

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    companion object {
        private const val TAG = "RealChatRepo"
        const val MESSAGE_RETENTION_MILLIS = 30L * 24 * 60 * 60 * 1000L // 30 days

        /**
         * Generates a canonical, order-independent Room ID for 1-on-1 direct messages.
         */
        fun getDmRoomId(id1: String, id2: String): String {
            val clean1 = id1.trim().lowercase().removePrefix("@").replace(" ", "_")
            val clean2 = id2.trim().lowercase().removePrefix("@").replace(" ", "_")
            return if (clean1 < clean2) "${clean1}_${clean2}" else "${clean2}_${clean1}"
        }

        /**
         * Generates the canonical Thread ID for 1-on-1 direct chats,
         * prioritizing non-blank UIDs with a clean username fallback.
         */
        fun getDmThreadId(
            uid1: String,
            username1: String,
            uid2: String,
            username2: String
        ): String {
            val cleanUid1 = uid1.trim()
            val cleanUid2 = uid2.trim()
            return if (cleanUid1.isNotBlank() && cleanUid2.isNotBlank()) {
                getDmRoomId(cleanUid1, cleanUid2)
            } else {
                getDmRoomId(username1, username2)
            }
        }
    }

    /**
     * Sends a 1-on-1 Direct Message with UID tracking and error handling.
     */
    suspend fun sendDirectMessage(
        senderUid: String,
        senderUsername: String,
        senderName: String,
        senderRole: String,
        avatarEmoji: String,
        recipientUid: String,
        recipientUsername: String,
        text: String,
        mediaUrl: String = "",
        mediaType: String = "",
        isSnap: Boolean = false
    ): Result<String> {
        if (text.isBlank() && mediaUrl.isBlank()) return Result.failure(IllegalArgumentException("Message cannot be empty"))

        val roomId = getDmThreadId(
            uid1 = senderUid,
            username1 = senderUsername,
            uid2 = recipientUid,
            username2 = recipientUsername
        )

        val effectiveSenderUid = senderUid.ifBlank {
            try { auth.currentUser?.uid ?: "" } catch (_: Throwable) { "" }
        }

        val now = System.currentTimeMillis()
        val expiresAt = now + MESSAGE_RETENTION_MILLIS

        val messageData = hashMapOf(
            "senderUid" to effectiveSenderUid,
            "senderUsername" to senderUsername.trim().removePrefix("@").lowercase(),
            "senderName" to senderName,
            "senderRole" to senderRole,
            "avatarEmoji" to avatarEmoji,
            "recipientUid" to recipientUid,
            "receiverUsername" to recipientUsername.trim().removePrefix("@").lowercase(),
            "message" to text.trim(),
            "mediaUrl" to mediaUrl,
            "mediaType" to mediaType,
            "isSnap" to isSnap,
            "isSnapOpened" to false,
            "timestamp" to now,
            "createdAt" to FieldValue.serverTimestamp(),
            "expiresAt" to expiresAt,
            "status" to "SENT"
        )

        return try {
            // CRITICAL: Ensure parent direct_chats/{roomId} exists with participants before writing message
            val cleanSenderUser = senderUsername.trim().removePrefix("@").lowercase()
            val cleanRecipientUser = recipientUsername.trim().removePrefix("@").lowercase()
            val threadSummary = hashMapOf(
                "threadId" to roomId,
                "participants" to listOf(effectiveSenderUid, recipientUid).filter { it.isNotBlank() },
                "participantUsernames" to listOf(cleanSenderUser, cleanRecipientUser).filter { it.isNotBlank() },
                "lastMessage" to text.trim(),
                "lastMessageAt" to now,
                "lastSenderUsername" to cleanSenderUser,
                "lastSenderName" to senderName.ifBlank { cleanSenderUser },
                "lastIsSnap" to isSnap,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("direct_chats")
                .document(roomId)
                .set(threadSummary, SetOptions.merge())
                .await()

            val docRef = firestore.collection("direct_chats")
                .document(roomId)
                .collection("messages")
                .add(messageData)
                .await()

            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.w(TAG, "sendDirectMessage failed: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Observes real-time direct messages between two users with 30-day retention filtering
     * and safe deserialization without ClassCastException.
     */
    fun observeDirectMessages(
        threadId: String,
        currentUserId: String = "",
        currentUsername: String = ""
    ): Flow<List<ChatMessage>> = callbackFlow {
        if (threadId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val effectiveUid = currentUserId.ifBlank {
            try { auth.currentUser?.uid ?: "" } catch (_: Throwable) { "" }
        }
        val cleanMy = currentUsername.trim().lowercase().removePrefix("@")
        val retentionCutoff = System.currentTimeMillis() - MESSAGE_RETENTION_MILLIS

        val listener = firestore.collection("direct_chats")
            .document(threadId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observeDirectMessages Firestore error: ${error.message}", error)
                    close(error)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    val ts = when (val rawTs = doc.get("timestamp")) {
                        is Number -> rawTs.toLong()
                        is com.google.firebase.Timestamp -> rawTs.toDate().time
                        else -> 0L
                    }
                    if (ts > 0 && ts < retentionCutoff) {
                        // Expired message (> 30 days)
                        try { doc.reference.delete() } catch (_: Throwable) {}
                        return@mapNotNull null
                    }

                    ChatViewModel.parseDocSafely(doc, effectiveUid)?.let { parsed ->
                        val isMe = parsed.isFromMe || (cleanMy.isNotBlank() && parsed.senderUsername.trim().lowercase().removePrefix("@") == cleanMy)
                        parsed.copy(isFromMe = isMe)
                    }
                } ?: emptyList()

                trySend(messages)
            }

        awaitClose { listener.remove() }
    }

    fun observeDirectMessages(
        myIdentifier: String,
        otherIdentifier: String
    ): Flow<List<ChatMessage>> {
        val roomId = getDmRoomId(myIdentifier, otherIdentifier)
        return observeDirectMessages(threadId = roomId, currentUsername = myIdentifier)
    }

    /**
     * Sends a Live Match banter chat message.
     */
    suspend fun sendMatchChatMessage(
        matchId: String,
        senderUid: String,
        senderUsername: String,
        senderName: String,
        senderRole: String,
        avatarEmoji: String,
        text: String
    ): Result<String> {
        if (matchId.isBlank()) return Result.failure(IllegalArgumentException("Match ID cannot be empty"))
        if (text.isBlank()) return Result.failure(IllegalArgumentException("Message cannot be empty"))

        val effectiveSenderUid = senderUid.ifBlank {
            try { auth.currentUser?.uid ?: "" } catch (_: Throwable) { "" }
        }

        val now = System.currentTimeMillis()
        val expiresAt = now + MESSAGE_RETENTION_MILLIS

        val messageData = hashMapOf(
            "senderUid" to effectiveSenderUid,
            "senderUsername" to senderUsername.trim().removePrefix("@").lowercase(),
            "senderName" to senderName,
            "senderRole" to senderRole,
            "avatarEmoji" to avatarEmoji,
            "message" to text.trim(),
            "timestamp" to now,
            "createdAt" to FieldValue.serverTimestamp(),
            "expiresAt" to expiresAt,
            "status" to "SENT"
        )

        return try {
            val docRef = firestore.collection("matches")
                .document(matchId)
                .collection("live_chat")
                .add(messageData)
                .await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.w(TAG, "sendMatchChatMessage failed: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Observes real-time match banter messages with 30-day retention enforcement.
     */
    fun observeMatchMessages(
        matchId: String,
        myUsername: String,
        myUid: String = ""
    ): Flow<List<ChatMessage>> = callbackFlow {
        if (matchId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val cleanMy = myUsername.trim().lowercase().removePrefix("@")
        val currentUid = myUid.ifBlank {
            try { auth.currentUser?.uid ?: "" } catch (_: Throwable) { "" }
        }
        val retentionCutoff = System.currentTimeMillis() - MESSAGE_RETENTION_MILLIS

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
                    val ts = doc.getLong("timestamp") ?: 0L
                    if (ts > 0 && ts < retentionCutoff) {
                        try { doc.reference.delete() } catch (_: Throwable) {}
                        return@mapNotNull null
                    }

                    val senderUid = doc.getString("senderUid") ?: ""
                    val sender = (doc.getString("senderUsername") ?: "").trim().lowercase().removePrefix("@")
                    val isFromMe = (senderUid.isNotBlank() && currentUid.isNotBlank() && senderUid == currentUid) ||
                                   sender == cleanMy

                    ChatMessage(
                        id = doc.id,
                        senderUid = senderUid,
                        senderUsername = sender,
                        senderName = doc.getString("senderName") ?: "User",
                        senderRole = doc.getString("senderRole") ?: "PLAYER",
                        avatarEmoji = doc.getString("avatarEmoji") ?: "🏏",
                        message = doc.getString("message") ?: "",
                        timestamp = ts,
                        isFromMe = isFromMe,
                        status = doc.getString("status") ?: "SENT"
                    )
                } ?: emptyList()

                trySend(messages)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Deletes a match chat message after validating that the requester owns the message or is admin.
     */
    suspend fun deleteMatchChatMessage(
        matchId: String,
        messageId: String,
        requesterUid: String = "",
        isAdmin: Boolean = false
    ): Result<Unit> {
        if (matchId.isBlank() || messageId.isBlank()) {
            return Result.failure(IllegalArgumentException("Match ID and Message ID cannot be empty"))
        }
        return try {
            val docRef = firestore.collection("matches")
                .document(matchId)
                .collection("live_chat")
                .document(messageId)

            val doc = docRef.get().await()
            val senderUid = doc.getString("senderUid") ?: ""
            if (!isAdmin && senderUid.isNotBlank() && requesterUid.isNotBlank() && senderUid != requesterUid) {
                return Result.failure(SecurityException("Unauthorized: Cannot delete messages sent by another user"))
            }

            docRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "deleteMatchChatMessage failed: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Deletes a direct message from a direct chat thread by threadId.
     */
    suspend fun deleteDirectMessageByThread(
        threadId: String,
        messageId: String,
        requesterUid: String = ""
    ): Result<Unit> {
        if (threadId.isBlank() || messageId.isBlank()) {
            return Result.failure(IllegalArgumentException("Thread ID and Message ID cannot be empty"))
        }
        return try {
            val docRef = firestore.collection("direct_chats")
                .document(threadId)
                .collection("messages")
                .document(messageId)

            val doc = docRef.get().await()
            val senderUid = doc.getString("senderUid") ?: ""
            if (requesterUid.isNotBlank() && senderUid.isNotBlank() && senderUid != requesterUid) {
                return Result.failure(SecurityException("Unauthorized: Cannot delete messages sent by another user"))
            }

            docRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "deleteDirectMessage failed: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Deletes a direct message from a direct chat thread.
     */
    suspend fun deleteDirectMessage(
        user1: String,
        user2: String,
        messageId: String,
        requesterUid: String = ""
    ): Result<Unit> {
        val roomId = getDmRoomId(user1, user2)
        return deleteDirectMessageByThread(roomId, messageId, requesterUid)
    }

    /**
     * Marks a disappearing Snap as opened in Firestore so both participants see it as disappeared.
     */
    suspend fun markSnapOpened(
        threadId: String,
        messageId: String
    ): Result<Unit> {
        if (threadId.isBlank() || messageId.isBlank()) return Result.failure(IllegalArgumentException("Thread ID and Message ID cannot be empty"))
        return try {
            firestore.collection("direct_chats")
                .document(threadId)
                .collection("messages")
                .document(messageId)
                .update("isSnapOpened", true)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "markSnapOpened failed: ${e.message}")
            Result.failure(e)
        }
    }
}
