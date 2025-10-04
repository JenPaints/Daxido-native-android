package com.daxido.data.repository

import com.daxido.core.models.Notification
import com.daxido.core.models.NotificationType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    fun getAllNotifications(): Flow<List<Notification>> = callbackFlow {
        val userId = auth.currentUser?.uid ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("notifications")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val notifications = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Notification::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(notifications)
            }

        awaitClose { listener.remove() }
    }

    fun getUnreadCount(): Flow<Int> = callbackFlow {
        val userId = auth.currentUser?.uid ?: run {
            trySend(0)
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("notifications")
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                trySend(snapshot?.size() ?: 0)
            }

        awaitClose { listener.remove() }
    }

    suspend fun markAsRead(notificationId: String) {
        try {
            firestore.collection("notifications")
                .document(notificationId)
                .update("isRead", true)
                .await()
        } catch (e: Exception) {
            // Handle error silently or log
        }
    }

    suspend fun markAllAsRead() {
        try {
            val userId = auth.currentUser?.uid ?: return

            val unreadNotifications = firestore.collection("notifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .await()

            val batch = firestore.batch()
            unreadNotifications.documents.forEach { doc ->
                batch.update(doc.reference, "isRead", true)
            }
            batch.commit().await()
        } catch (e: Exception) {
            // Handle error
        }
    }

    suspend fun addNotification(notification: Notification) {
        try {
            val userId = auth.currentUser?.uid ?: return

            val notificationData = notification.copy(
                userId = userId,
                timestamp = Date()
            )

            firestore.collection("notifications")
                .add(notificationData)
                .await()
        } catch (e: Exception) {
            // Handle error
        }
    }

    suspend fun deleteNotification(notificationId: String) {
        try {
            firestore.collection("notifications")
                .document(notificationId)
                .delete()
                .await()
        } catch (e: Exception) {
            // Handle error
        }
    }

    suspend fun clearAllNotifications() {
        try {
            val userId = auth.currentUser?.uid ?: return

            val notifications = firestore.collection("notifications")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val batch = firestore.batch()
            notifications.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()
        } catch (e: Exception) {
            // Handle error
        }
    }
}