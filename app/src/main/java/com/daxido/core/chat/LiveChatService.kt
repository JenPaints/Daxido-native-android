package com.daxido.core.chat

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Live chat service for communication between riders and drivers
 */
@Singleton
class LiveChatService @Inject constructor(
    private val context: Context,
    private val firestore: FirebaseFirestore
) {

    private val chatsCollection = firestore.collection("chats")
    private val messagesCollection = "messages"

    /**
     * Create a new chat room
     */
    suspend fun createChatRoom(
        rideId: String,
        riderId: String,
        driverId: String
    ): ChatRoomResult {
        return try {
            val chatRoom = ChatRoom(
                id = "chat_$rideId",
                rideId = rideId,
                riderId = riderId,
                driverId = driverId,
                status = ChatStatus.ACTIVE,
                createdAt = System.currentTimeMillis(),
                lastMessage = null,
                lastMessageTime = null
            )

            chatsCollection.document(chatRoom.id)
                .set(chatRoom)
                .await()

            Log.d("LiveChatService", "Chat room created: ${chatRoom.id}")
            ChatRoomResult.Success(chatRoom)
        } catch (e: Exception) {
            Log.e("LiveChatService", "Error creating chat room: ${e.message}", e)
            ChatRoomResult.Failure(e.message ?: "Failed to create chat room")
        }
    }

    /**
     * Send a message
     */
    suspend fun sendMessage(
        chatRoomId: String,
        senderId: String,
        senderType: UserType,
        message: String,
        messageType: MessageType = MessageType.TEXT
    ): MessageResult {
        return try {
            val chatMessage = ChatMessage(
                id = "msg_${System.currentTimeMillis()}",
                chatRoomId = chatRoomId,
                senderId = senderId,
                senderType = senderType,
                message = message,
                messageType = messageType,
                timestamp = System.currentTimeMillis(),
                isRead = false,
                isDelivered = true
            )

            chatsCollection.document(chatRoomId)
                .collection(messagesCollection)
                .document(chatMessage.id)
                .set(chatMessage)
                .await()

            // Update last message in chat room
            chatsCollection.document(chatRoomId)
                .update(
                    mapOf(
                        "lastMessage" to message,
                        "lastMessageTime" to chatMessage.timestamp
                    )
                )
                .await()

            Log.d("LiveChatService", "Message sent: ${chatMessage.id}")
            MessageResult.Success(chatMessage)
        } catch (e: Exception) {
            Log.e("LiveChatService", "Error sending message: ${e.message}", e)
            MessageResult.Failure(e.message ?: "Failed to send message")
        }
    }

    /**
     * Get messages for a chat room
     */
    fun getMessages(chatRoomId: String): Flow<List<ChatMessage>> = callbackFlow {
        val subscription = chatsCollection.document(chatRoomId)
            .collection(messagesCollection)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("LiveChatService", "Error listening to messages: ${error.message}", error)
                    close(error)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)
                } ?: emptyList()

                trySend(messages)
            }

        awaitClose { subscription.remove() }
    }

    /**
     * Mark messages as read
     */
    suspend fun markMessagesAsRead(
        chatRoomId: String,
        userId: String
    ): Boolean {
        return try {
            val messages = chatsCollection.document(chatRoomId)
                .collection(messagesCollection)
                .whereEqualTo("isRead", false)
                .whereNotEqualTo("senderId", userId)
                .get()
                .await()

            messages.documents.forEach { doc ->
                doc.reference.update("isRead", true).await()
            }

            Log.d("LiveChatService", "Messages marked as read for user: $userId")
            true
        } catch (e: Exception) {
            Log.e("LiveChatService", "Error marking messages as read: ${e.message}", e)
            false
        }
    }

    /**
     * Send quick reply
     */
    suspend fun sendQuickReply(
        chatRoomId: String,
        senderId: String,
        senderType: UserType,
        quickReplyType: QuickReplyType
    ): MessageResult {
        val message = when (quickReplyType) {
            QuickReplyType.ON_THE_WAY -> "I'm on the way!"
            QuickReplyType.ARRIVING_SOON -> "Arriving in 2 minutes"
            QuickReplyType.ARRIVED -> "I've arrived at the pickup location"
            QuickReplyType.RUNNING_LATE -> "Sorry, running a bit late"
            QuickReplyType.THANK_YOU -> "Thank you!"
            QuickReplyType.CALL_ME -> "Please call me"
        }

        return sendMessage(chatRoomId, senderId, senderType, message, MessageType.QUICK_REPLY)
    }

    /**
     * End chat session
     */
    suspend fun endChatSession(chatRoomId: String): Boolean {
        return try {
            chatsCollection.document(chatRoomId)
                .update("status", ChatStatus.ENDED.name)
                .await()

            Log.d("LiveChatService", "Chat session ended: $chatRoomId")
            true
        } catch (e: Exception) {
            Log.e("LiveChatService", "Error ending chat session: ${e.message}", e)
            false
        }
    }

    /**
     * Get unread message count
     */
    suspend fun getUnreadCount(chatRoomId: String, userId: String): Int {
        return try {
            val messages = chatsCollection.document(chatRoomId)
                .collection(messagesCollection)
                .whereEqualTo("isRead", false)
                .whereNotEqualTo("senderId", userId)
                .get()
                .await()

            messages.size()
        } catch (e: Exception) {
            Log.e("LiveChatService", "Error getting unread count: ${e.message}", e)
            0
        }
    }
}

data class ChatRoom(
    val id: String = "",
    val rideId: String = "",
    val riderId: String = "",
    val driverId: String = "",
    val status: ChatStatus = ChatStatus.ACTIVE,
    val createdAt: Long = 0,
    val lastMessage: String? = null,
    val lastMessageTime: Long? = null
)

data class ChatMessage(
    val id: String = "",
    val chatRoomId: String = "",
    val senderId: String = "",
    val senderType: UserType = UserType.RIDER,
    val message: String = "",
    val messageType: MessageType = MessageType.TEXT,
    val timestamp: Long = 0,
    val isRead: Boolean = false,
    val isDelivered: Boolean = false
)

enum class ChatStatus {
    ACTIVE, ENDED
}

enum class UserType {
    RIDER, DRIVER
}

enum class MessageType {
    TEXT, QUICK_REPLY, LOCATION, IMAGE
}

enum class QuickReplyType {
    ON_THE_WAY,
    ARRIVING_SOON,
    ARRIVED,
    RUNNING_LATE,
    THANK_YOU,
    CALL_ME
}

sealed class ChatRoomResult {
    data class Success(val chatRoom: ChatRoom) : ChatRoomResult()
    data class Failure(val message: String) : ChatRoomResult()
}

sealed class MessageResult {
    data class Success(val message: ChatMessage) : MessageResult()
    data class Failure(val message: String) : MessageResult()
}