package com.daxido.core.data.repository

import com.daxido.user.presentation.notifications.Notification
import com.daxido.user.presentation.notifications.NotificationType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationsRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    suspend fun getNotifications(): List<Notification> {
        val userId = auth.currentUser?.uid ?: return emptyList()

        return try {
            val snapshot = firestore.collection("notifications")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                Notification(
                    id = doc.id,
                    type = NotificationType.valueOf(doc.getString("type") ?: "SYSTEM_UPDATE"),
                    title = doc.getString("title") ?: "",
                    message = doc.getString("message") ?: "",
                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                    isRead = doc.getBoolean("isRead") ?: false,
                    actionUrl = doc.getString("actionUrl"),
                    imageUrl = doc.getString("imageUrl"),
                    data = (doc.get("data") as? Map<String, String>) ?: emptyMap()
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun observeNotifications(): Flow<List<Notification>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("notifications")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val notifications = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        Notification(
                            id = doc.id,
                            type = NotificationType.valueOf(doc.getString("type") ?: "SYSTEM_UPDATE"),
                            title = doc.getString("title") ?: "",
                            message = doc.getString("message") ?: "",
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                            isRead = doc.getBoolean("isRead") ?: false,
                            actionUrl = doc.getString("actionUrl"),
                            imageUrl = doc.getString("imageUrl"),
                            data = (doc.get("data") as? Map<String, String>) ?: emptyMap()
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(notifications)
            }

        awaitClose { listener.remove() }
    }

    suspend fun markAsRead(notificationId: String): Boolean {
        return try {
            firestore.collection("notifications")
                .document(notificationId)
                .update("isRead", true, "readAt", System.currentTimeMillis())
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun markAllAsRead(): Boolean {
        val userId = auth.currentUser?.uid ?: return false

        return try {
            val batch = firestore.batch()
            val snapshot = firestore.collection("notifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .await()

            snapshot.documents.forEach { doc ->
                batch.update(
                    doc.reference,
                    mapOf(
                        "isRead" to true,
                        "readAt" to System.currentTimeMillis()
                    )
                )
            }

            batch.commit().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteNotification(notificationId: String): Boolean {
        return try {
            firestore.collection("notifications")
                .document(notificationId)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun clearAllNotifications(): Boolean {
        val userId = auth.currentUser?.uid ?: return false

        return try {
            val batch = firestore.batch()
            val snapshot = firestore.collection("notifications")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            snapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }

            batch.commit().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun sendNotification(
        userId: String,
        type: NotificationType,
        title: String,
        message: String,
        actionUrl: String? = null,
        imageUrl: String? = null,
        data: Map<String, String> = emptyMap()
    ): Boolean {
        return try {
            val notificationData = hashMapOf(
                "userId" to userId,
                "type" to type.name,
                "title" to title,
                "message" to message,
                "timestamp" to System.currentTimeMillis(),
                "isRead" to false,
                "actionUrl" to actionUrl,
                "imageUrl" to imageUrl,
                "data" to data,
                "platform" to "android"
            )

            firestore.collection("notifications")
                .add(notificationData)
                .await()

            // Trigger FCM push notification
            triggerPushNotification(userId, title, message)

            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun triggerPushNotification(userId: String, title: String, message: String) {
        try {
            // This would trigger a Cloud Function to send FCM notification
            val fcmData = hashMapOf(
                "userId" to userId,
                "title" to title,
                "message" to message,
                "timestamp" to System.currentTimeMillis()
            )

            firestore.collection("fcm_queue")
                .add(fcmData)
                .await()
        } catch (e: Exception) {
            // Silent fail for push notifications
        }
    }
}