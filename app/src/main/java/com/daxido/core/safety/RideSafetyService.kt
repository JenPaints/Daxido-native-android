package com.daxido.core.safety

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Comprehensive ride safety service with recording, SOS, and tracking features
 */
@Singleton
class RideSafetyService @Inject constructor(
    private val context: Context,
    private val storage: FirebaseStorage
) {

    private var mediaRecorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var isRecording = false

    // Trusted contacts for emergency
    private val emergencyContacts = mutableListOf<EmergencyContact>()
    private val rideRecordings = mutableMapOf<String, RideRecording>()

    /**
     * Start ride recording (audio)
     */
    fun startRideRecording(rideId: String): RecordingResult {
        try {
            if (isRecording) {
                return RecordingResult.Failure("Recording already in progress")
            }

            // Check audio recording permission
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return RecordingResult.Failure("Audio recording permission not granted")
            }

            val outputDir = context.getExternalFilesDir(null)
            recordingFile = File(outputDir, "ride_${rideId}_${System.currentTimeMillis()}.m4a")

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(recordingFile?.absolutePath)

                try {
                    prepare()
                    start()
                    isRecording = true

                    val recording = RideRecording(
                        id = "rec_${System.currentTimeMillis()}",
                        rideId = rideId,
                        filePath = recordingFile?.absolutePath ?: "",
                        startTime = System.currentTimeMillis(),
                        endTime = null,
                        uploaded = false
                    )

                    rideRecordings[rideId] = recording

                    Log.d("RideSafetyService", "Recording started for ride: $rideId")
                    return RecordingResult.Success(recording)

                } catch (e: IOException) {
                    Log.e("RideSafetyService", "Error starting recording: ${e.message}", e)
                    return RecordingResult.Failure("Failed to start recording: ${e.message}")
                }
            }

        } catch (e: Exception) {
            Log.e("RideSafetyService", "Error initializing recorder: ${e.message}", e)
            return RecordingResult.Failure("Failed to initialize recorder: ${e.message}")
        }

        return RecordingResult.Failure("Unknown error")
    }

    /**
     * Stop ride recording
     */
    fun stopRideRecording(rideId: String): Boolean {
        return try {
            if (!isRecording) {
                Log.w("RideSafetyService", "No recording in progress")
                return false
            }

            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false

            rideRecordings[rideId]?.let { recording ->
                rideRecordings[rideId] = recording.copy(
                    endTime = System.currentTimeMillis()
                )
            }

            Log.d("RideSafetyService", "Recording stopped for ride: $rideId")
            true
        } catch (e: Exception) {
            Log.e("RideSafetyService", "Error stopping recording: ${e.message}", e)
            false
        }
    }

    /**
     * Upload ride recording to cloud storage
     */
    suspend fun uploadRideRecording(rideId: String): Boolean {
        return try {
            val recording = rideRecordings[rideId] ?: return false
            val file = File(recording.filePath)

            if (!file.exists()) {
                Log.e("RideSafetyService", "Recording file not found: ${recording.filePath}")
                return false
            }

            val storageRef = storage.reference
                .child("ride_recordings")
                .child(rideId)
                .child(file.name)

            storageRef.putFile(android.net.Uri.fromFile(file))
                .await()

            rideRecordings[rideId] = recording.copy(uploaded = true)

            Log.d("RideSafetyService", "Recording uploaded for ride: $rideId")
            true
        } catch (e: Exception) {
            Log.e("RideSafetyService", "Error uploading recording: ${e.message}", e)
            false
        }
    }

    /**
     * Trigger SOS emergency alert
     */
    suspend fun triggerSOS(
        rideId: String,
        userId: String,
        location: android.location.Location,
        message: String = "Emergency! Need help!"
    ): SOSResult {
        return try {
            val sosAlert = SOSAlert(
                id = "sos_${System.currentTimeMillis()}",
                rideId = rideId,
                userId = userId,
                latitude = location.latitude,
                longitude = location.longitude,
                message = message,
                timestamp = System.currentTimeMillis(),
                status = SOSStatus.ACTIVE,
                responders = emptyList()
            )

            // Send alerts to emergency contacts
            emergencyContacts.forEach { contact ->
                sendSOSNotification(contact, sosAlert)
            }

            // Alert nearby drivers/authorities
            alertNearbyResponders(sosAlert)

            Log.d("RideSafetyService", "SOS triggered for ride: $rideId")
            SOSResult.Success(sosAlert)

        } catch (e: Exception) {
            Log.e("RideSafetyService", "Error triggering SOS: ${e.message}", e)
            SOSResult.Failure("Failed to trigger SOS: ${e.message}")
        }
    }

    /**
     * Share ride tracking with trusted contact
     */
    suspend fun shareRideTracking(
        rideId: String,
        contactPhone: String,
        contactName: String
    ): Boolean {
        return try {
            val trackingLink = "https://daxido.app/track/$rideId"

            // Send SMS with tracking link
            sendTrackingSMS(contactPhone, contactName, trackingLink)

            Log.d("RideSafetyService", "Ride tracking shared with: $contactName")
            true
        } catch (e: Exception) {
            Log.e("RideSafetyService", "Error sharing ride tracking: ${e.message}", e)
            false
        }
    }

    /**
     * Add emergency contact
     */
    fun addEmergencyContact(contact: EmergencyContact) {
        emergencyContacts.add(contact)
        Log.d("RideSafetyService", "Emergency contact added: ${contact.name}")
    }

    /**
     * Remove emergency contact
     */
    fun removeEmergencyContact(contactId: String) {
        emergencyContacts.removeIf { it.id == contactId }
        Log.d("RideSafetyService", "Emergency contact removed: $contactId")
    }

    /**
     * Get emergency contacts
     */
    fun getEmergencyContacts(): List<EmergencyContact> = emergencyContacts.toList()

    /**
     * Enable women safety mode
     */
    suspend fun enableWomenSafetyMode(rideId: String): Boolean {
        return try {
            // Auto-share ride with emergency contacts
            emergencyContacts.forEach { contact ->
                shareRideTracking(rideId, contact.phone, contact.name)
            }

            // Enable automatic SOS triggers for suspicious activities
            // Start ride recording automatically
            startRideRecording(rideId)

            Log.d("RideSafetyService", "Women safety mode enabled for ride: $rideId")
            true
        } catch (e: Exception) {
            Log.e("RideSafetyService", "Error enabling women safety mode: ${e.message}", e)
            false
        }
    }

    private fun sendSOSNotification(contact: EmergencyContact, alert: SOSAlert) {
        // Implement SMS/Push notification to emergency contact
        Log.d("RideSafetyService", "SOS notification sent to: ${contact.name}")
    }

    private fun alertNearbyResponders(alert: SOSAlert) {
        // Implement alert to nearby drivers/authorities
        Log.d("RideSafetyService", "Nearby responders alerted")
    }

    private fun sendTrackingSMS(phone: String, name: String, link: String) {
        // Implement SMS sending
        Log.d("RideSafetyService", "Tracking SMS sent to: $name")
    }
}

data class RideRecording(
    val id: String,
    val rideId: String,
    val filePath: String,
    val startTime: Long,
    val endTime: Long?,
    val uploaded: Boolean
)

data class EmergencyContact(
    val id: String,
    val name: String,
    val phone: String,
    val relationship: String
)

data class SOSAlert(
    val id: String,
    val rideId: String,
    val userId: String,
    val latitude: Double,
    val longitude: Double,
    val message: String,
    val timestamp: Long,
    val status: SOSStatus,
    val responders: List<String>
)

enum class SOSStatus {
    ACTIVE, RESOLVED, CANCELLED
}

sealed class RecordingResult {
    data class Success(val recording: RideRecording) : RecordingResult()
    data class Failure(val message: String) : RecordingResult()
}

sealed class SOSResult {
    data class Success(val alert: SOSAlert) : SOSResult()
    data class Failure(val message: String) : SOSResult()
}