package com.daxido.core.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.daxido.BuildConfig
import com.daxido.MainActivity
import com.daxido.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class DaxidoMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "DaxidoMessagingService"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        try {
            // SECURITY FIX: Only log in debug mode, don't log the actual token
            if (BuildConfig.DEBUG) {
                android.util.Log.d(TAG, "New FCM token received")
            }
            // Send token to server
            sendTokenToServer(token)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error handling new token", e)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        try {
            // SECURITY FIX: Only log in debug mode, don't log message details in production
            if (BuildConfig.DEBUG) {
                android.util.Log.d(TAG, "Message received")
            }

            message.notification?.let {
                try {
                    showNotification(it.title ?: "", it.body ?: "")
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Error showing notification", e)
                }
            }

            if (message.data.isNotEmpty()) {
                try {
                    handleDataPayload(message.data)
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Error handling data payload", e)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error in onMessageReceived", e)
        }
    }

    private fun showNotification(title: String, body: String) {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (notificationManager == null) {
                android.util.Log.e(TAG, "NotificationManager not available")
                return
            }

            val channelId = "daxido_notifications"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    val channel = NotificationChannel(
                        channelId,
                        "Daxido Notifications",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Ride updates and important notifications"
                        enableLights(true)
                        enableVibration(true)
                    }
                    notificationManager.createNotificationChannel(channel)
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Error creating notification channel", e)
                }
            }

            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
            android.util.Log.d(TAG, "Notification shown successfully")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error in showNotification", e)
        }
    }

    private fun handleDataPayload(data: Map<String, String>) {
        when (data["type"]) {
            "ride_request" -> handleRideRequest(data)
            "ride_accepted" -> handleRideAccepted(data)
            "driver_arrived" -> handleDriverArrived(data)
            "ride_started" -> handleRideStarted(data)
            "ride_completed" -> handleRideCompleted(data)
            "payment_received" -> handlePaymentReceived(data)
        }
    }

    private fun handleRideRequest(data: Map<String, String>) {
        // Handle new ride request for drivers
        val rideId = data["ride_id"]
        val pickup = data["pickup_location"]
        val drop = data["drop_location"]
        val fare = data["estimated_fare"]

        showNotification(
            "New Ride Request",
            "Pickup: $pickup | Fare: ₹$fare"
        )
    }

    private fun handleRideAccepted(data: Map<String, String>) {
        // Handle ride acceptance notification for users
        val driverName = data["driver_name"]
        val vehicleNumber = data["vehicle_number"]
        val eta = data["eta"]

        showNotification(
            "Driver Assigned",
            "$driverName is arriving in $eta minutes"
        )
    }

    private fun handleDriverArrived(data: Map<String, String>) {
        // SECURITY FIX: Don't show OTP in notification, only in the app for security
        showNotification(
            "Driver Arrived",
            "Your driver has arrived. Open app to view OTP."
        )
    }

    private fun handleRideStarted(data: Map<String, String>) {
        showNotification(
            "Ride Started",
            "Your journey has begun. Have a safe trip!"
        )
    }

    private fun handleRideCompleted(data: Map<String, String>) {
        val fare = data["fare"]
        showNotification(
            "Ride Completed",
            "Total fare: ₹$fare. Thank you for riding with Daxido!"
        )
    }

    private fun handlePaymentReceived(data: Map<String, String>) {
        val amount = data["amount"]
        showNotification(
            "Payment Successful",
            "₹$amount has been received"
        )
    }

    private fun sendTokenToServer(token: String) {
        // FEATURE COMPLETE: Send FCM token to backend server
        try {
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val userId = auth.currentUser?.uid

            if (userId != null) {
                // Store token in Firestore for this user
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val tokenData = hashMapOf(
                    "token" to token,
                    "userId" to userId,
                    "platform" to "android",
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    "deviceModel" to android.os.Build.MODEL,
                    "deviceManufacturer" to android.os.Build.MANUFACTURER,
                    "osVersion" to android.os.Build.VERSION.SDK_INT
                )

                db.collection("fcm_tokens")
                    .document(userId)
                    .set(tokenData)
                    .addOnSuccessListener {
                        if (BuildConfig.DEBUG) {
                            android.util.Log.d(TAG, "FCM token successfully uploaded")
                        }
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e(TAG, "Failed to upload FCM token", e)
                    }

                // Also update user document with latest token
                db.collection("users")
                    .document(userId)
                    .update("fcmToken", token, "tokenUpdatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp())
            } else {
                // User not logged in yet, save token locally to upload after login
                getSharedPreferences("daxido_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putString("pending_fcm_token", token)
                    .apply()
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error in sendTokenToServer", e)
        }
    }
}