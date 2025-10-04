package com.daxido.core.models

/**
 * Emergency Live Dashcam Feature
 *
 * Data models for real-time camera streaming during emergencies
 * When rider triggers SOS, driver's camera activates and streams live to:
 * - Rider (for safety assurance)
 * - Admin (for monitoring and response)
 */

data class EmergencyStream(
    val streamId: String = "",
    val rideId: String = "",
    val riderId: String = "",
    val driverId: String = "",
    val status: StreamStatus = StreamStatus.INACTIVE,
    val startedAt: Long = 0L,
    val endedAt: Long? = null,
    val cameraType: CameraType = CameraType.REAR,
    val audioEnabled: Boolean = true,
    val recordingUrl: String? = null, // Cloud Storage URL for saved recording
    val adminViewers: List<String> = emptyList(), // Admin IDs currently viewing
    val metadata: StreamMetadata = StreamMetadata(),
    val location: StreamLocation? = null,
    val emergencyType: EmergencyType = EmergencyType.GENERAL_SOS,
    val triggeredBy: String = "", // User ID who triggered
    val autoRecording: Boolean = true // Auto-save to Cloud Storage
)

enum class StreamStatus {
    INACTIVE,       // Not streaming
    STARTING,       // Camera initializing
    ACTIVE,         // Currently streaming
    PAUSED,         // Temporarily paused
    ENDED,          // Stream ended normally
    ERROR,          // Stream failed
    SAVING          // Saving recording to storage
}

enum class CameraType {
    FRONT,  // Driver-facing camera (for driver behavior)
    REAR,   // Road-facing camera (for road conditions)
    BOTH    // Both cameras (if device supports dual camera)
}

enum class EmergencyType {
    GENERAL_SOS,        // General emergency
    ACCIDENT,           // Traffic accident
    HARASSMENT,         // Driver harassment
    SUSPICIOUS_ROUTE,   // Driver taking wrong route
    MEDICAL,            // Medical emergency
    VEHICLE_ISSUE,      // Vehicle problem
    OTHER               // Other safety concern
}

data class StreamMetadata(
    val resolution: String = "640x480",
    val frameRate: Int = 10,  // Frames per second (10 FPS for bandwidth optimization)
    val bitrate: Int = 500,   // Kilobits per second
    val compressionQuality: Int = 50, // JPEG quality 0-100
    val audioQuality: Int = 32, // kbps for audio
    val deviceModel: String = "",
    val osVersion: String = "",
    val appVersion: String = "",
    val networkType: String = "", // WiFi, 4G, 5G, etc.
    val batteryLevel: Int = 100,
    val storageAvailable: Long = 0L // Bytes available
)

data class StreamLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy: Float = 0f,
    val speed: Float = 0f,
    val heading: Float = 0f,
    val address: String = "",
    val timestamp: Long = 0L
)

/**
 * Individual frame data for streaming
 * Sent via Firebase Realtime Database for low-latency delivery
 */
data class StreamFrame(
    val frameId: String = "",
    val streamId: String = "",
    val timestamp: Long = 0L,
    val imageData: String = "", // Base64 encoded JPEG image
    val audioData: String? = null, // Base64 encoded audio chunk (optional)
    val sequenceNumber: Int = 0,
    val location: StreamLocation? = null,
    val metadata: FrameMetadata? = null
)

data class FrameMetadata(
    val fileSize: Int = 0, // Bytes
    val encodingTime: Long = 0L, // Milliseconds to encode
    val uploadTime: Long = 0L, // Milliseconds to upload
    val quality: Int = 50 // Compression quality used
)

/**
 * Admin notification for emergency stream
 */
data class EmergencyStreamNotification(
    val notificationId: String = "",
    val streamId: String = "",
    val rideId: String = "",
    val emergencyType: EmergencyType = EmergencyType.GENERAL_SOS,
    val riderName: String = "",
    val driverName: String = "",
    val location: StreamLocation? = null,
    val timestamp: Long = 0L,
    val priority: NotificationPriority = NotificationPriority.HIGH,
    val acknowledged: Boolean = false,
    val acknowledgedBy: String? = null,
    val acknowledgedAt: Long? = null
)

enum class NotificationPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Stream recording saved to Cloud Storage
 */
data class StreamRecording(
    val recordingId: String = "",
    val streamId: String = "",
    val rideId: String = "",
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val duration: Long = 0L, // Milliseconds
    val fileUrl: String = "", // Cloud Storage URL
    val fileSize: Long = 0L, // Bytes
    val thumbnailUrl: String? = null,
    val format: String = "mp4",
    val resolution: String = "",
    val frameCount: Int = 0,
    val hasAudio: Boolean = true,
    val retentionPeriod: Int = 30, // Days to keep recording
    val evidenceStatus: EvidenceStatus = EvidenceStatus.PENDING,
    val accessLog: List<RecordingAccess> = emptyList()
)

enum class EvidenceStatus {
    PENDING,        // Awaiting review
    UNDER_REVIEW,   // Being reviewed
    CLEARED,        // No issue found
    FLAGGED,        // Flagged for further investigation
    REPORTED,       // Reported to authorities
    ARCHIVED        // Archived after resolution
}

data class RecordingAccess(
    val userId: String = "",
    val userName: String = "",
    val accessedAt: Long = 0L,
    val purpose: String = "",
    val ipAddress: String = ""
)

/**
 * Stream viewer tracking
 */
data class StreamViewer(
    val viewerId: String = "",
    val viewerName: String = "",
    val viewerRole: ViewerRole = ViewerRole.ADMIN,
    val joinedAt: Long = 0L,
    val leftAt: Long? = null,
    val isActive: Boolean = true
)

enum class ViewerRole {
    RIDER,          // The rider who triggered SOS
    ADMIN,          // Platform admin
    EMERGENCY,      // Emergency services
    SUPPORT         // Customer support
}

/**
 * Stream statistics for monitoring
 */
data class StreamStatistics(
    val streamId: String = "",
    val totalFrames: Int = 0,
    val droppedFrames: Int = 0,
    val averageFps: Float = 0f,
    val averageLatency: Long = 0L, // Milliseconds
    val totalDataTransferred: Long = 0L, // Bytes
    val peakViewers: Int = 0,
    val currentViewers: Int = 0,
    val networkErrors: Int = 0,
    val cameraErrors: Int = 0
)

/**
 * Camera Control Command
 * Admin can send commands to control driver's camera in real-time
 */
data class CameraCommand(
    val commandId: String = "",
    val streamId: String = "",
    val commandType: CameraCommandType = CameraCommandType.SWITCH_CAMERA,
    val cameraType: CameraType? = null, // Target camera for SWITCH_CAMERA
    val issuedBy: String = "", // Admin ID
    val issuedAt: Long = 0L,
    val executed: Boolean = false,
    val executedAt: Long? = null,
    val result: CommandResult = CommandResult.PENDING
)

enum class CameraCommandType {
    SWITCH_CAMERA,      // Switch between front/rear camera
    TOGGLE_AUDIO,       // Enable/disable audio
    ADJUST_QUALITY,     // Change video quality
    PAUSE_STREAM,       // Temporarily pause streaming
    RESUME_STREAM,      // Resume paused stream
    TAKE_SNAPSHOT       // Capture high-quality snapshot
}

enum class CommandResult {
    PENDING,            // Command issued, waiting for execution
    SUCCESS,            // Command executed successfully
    FAILED,             // Command execution failed
    TIMEOUT,            // Command not executed within timeout
    CANCELLED           // Command cancelled by admin
}
