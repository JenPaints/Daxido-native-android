package com.daxido.core.services

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.daxido.core.models.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Safety features matching Ola/Uber/Rapido standards
 */
@Singleton
class SafetyService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions,
    private val context: Context
) {

    /**
     * Get user's safety contacts
     */
    suspend fun getSafetyContacts(): Result<List<SafetyContact>> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))

            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("safetyContacts")
                .get()
                .await()

            val contacts = snapshot.documents.mapNotNull { it.toObject(SafetyContact::class.java) }
            Result.success(contacts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Add safety contact
     */
    suspend fun addSafetyContact(contact: SafetyContact): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))

            firestore.collection("users")
                .document(userId)
                .collection("safetyContacts")
                .add(contact)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Share trip with contacts (like Uber's "Share Trip" feature)
     */
    suspend fun shareTripWithContacts(
        rideId: String,
        contacts: List<SafetyContact>
    ): Result<TripSharingInfo> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))

            // Generate shareable link
            val shareLink = "https://daxido.com/track/$rideId"

            // Send SMS/WhatsApp to contacts
            val message = buildTripShareMessage(rideId, shareLink)

            contacts.forEach { contact ->
                sendTripShareMessage(contact.phoneNumber, message)
            }

            // Save sharing info
            val sharingInfo = TripSharingInfo(
                shareLink = shareLink,
                isSharing = true,
                sharedWith = contacts.map { it.phoneNumber },
                expiresAt = System.currentTimeMillis() + (4 * 60 * 60 * 1000) // 4 hours
            )

            firestore.collection("rides")
                .document(rideId)
                .update("sharingInfo", sharingInfo)
                .await()

            Result.success(sharingInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Trigger SOS/Emergency (like Ola's Guardian feature)
     */
    suspend fun triggerSOS(
        rideId: String,
        location: Location?,
        reason: String = "Emergency SOS triggered"
    ): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))

            // 1. Alert safety contacts
            val contacts = getSafetyContacts().getOrNull() ?: emptyList()
            val emergencyContacts = contacts.filter { it.isEmergencyContact }

            val sosMessage = buildSOSMessage(rideId, location)
            emergencyContacts.forEach { contact ->
                sendSOSMessage(contact.phoneNumber, sosMessage)
            }

            // 2. Alert admin/support team via Firebase Function
            val sosData = mapOf(
                "rideId" to rideId,
                "userId" to userId,
                "location" to mapOf(
                    "latitude" to (location?.latitude ?: 0.0),
                    "longitude" to (location?.longitude ?: 0.0)
                ),
                "reason" to reason,
                "timestamp" to System.currentTimeMillis()
            )

            functions.getHttpsCallable("triggerSOS")
                .call(sosData)
                .await()

            // 3. Update ride with SOS status
            firestore.collection("rides")
                .document(rideId)
                .update(
                    mapOf(
                        "sosTriggered" to true,
                        "sosTimestamp" to com.google.firebase.Timestamp.now(),
                        "sosReason" to reason
                    )
                )
                .await()

            // 4. Call emergency number option
            showEmergencyCallDialog()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Auto-share trip when ride starts (Uber style)
     */
    suspend fun autoShareTrip(rideId: String): Result<Unit> {
        return try {
            val contacts = getSafetyContacts().getOrNull() ?: return Result.success(Unit)
            if (contacts.isEmpty()) return Result.success(Unit)

            shareTripWithContacts(rideId, contacts)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Stop sharing trip
     */
    suspend fun stopSharingTrip(rideId: String): Result<Unit> {
        return try {
            firestore.collection("rides")
                .document(rideId)
                .update(
                    mapOf(
                        "sharingInfo.isSharing" to false
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Call police (like Rapido's "Call Police" feature)
     */
    fun callPolice() {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:100") // 100 is police number in India
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Call ambulance
     */
    fun callAmbulance() {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:108") // 108 is ambulance number in India
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    private fun buildTripShareMessage(rideId: String, shareLink: String): String {
        return """
            🚗 I'm on a Daxido ride!

            Track my trip in real-time:
            $shareLink

            Ride ID: $rideId

            This link will expire in 4 hours.
        """.trimIndent()
    }

    private fun buildSOSMessage(rideId: String, location: Location?): String {
        val locationText = location?.let {
            "Location: https://maps.google.com/?q=${it.latitude},${it.longitude}"
        } ?: "Location unavailable"

        return """
            🆘 EMERGENCY ALERT 🆘

            I need help! I'm on a Daxido ride.
            Ride ID: $rideId
            $locationText

            Please contact me immediately!
        """.trimIndent()
    }

    private fun sendTripShareMessage(phoneNumber: String, message: String) {
        // In production, integrate with SMS gateway or WhatsApp Business API
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://wa.me/$phoneNumber?text=${Uri.encode(message)}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to SMS
            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$phoneNumber")
                putExtra("sms_body", message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(smsIntent)
        }
    }

    private fun sendSOSMessage(phoneNumber: String, message: String) {
        sendTripShareMessage(phoneNumber, message)
    }

    private fun showEmergencyCallDialog() {
        // This would show a dialog with options to call:
        // - Police (100)
        // - Ambulance (108)
        // - Women's Helpline (1091)
        // Implementation would be in the UI layer
    }
}
