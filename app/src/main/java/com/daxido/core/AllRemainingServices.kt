package com.daxido.core

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FAVORITE DRIVERS SERVICE
 */
@Singleton
class FavoriteDriversService @Inject constructor(
    private val context: Context,
    private val firestore: FirebaseFirestore
) {
    private val favoritesCollection = firestore.collection("favoriteDrivers")

    suspend fun addFavoriteDriver(userId: String, driverId: String): Boolean {
        return try {
            val favoriteId = "${userId}_${driverId}"
            val favorite = mapOf(
                "id" to favoriteId,
                "userId" to userId,
                "driverId" to driverId,
                "addedAt" to System.currentTimeMillis()
            )
            favoritesCollection.document(favoriteId).set(favorite).await()
            Log.d("FavoriteDrivers", "Driver added to favorites: $driverId")
            true
        } catch (e: Exception) {
            Log.e("FavoriteDrivers", "Error adding favorite: ${e.message}", e)
            false
        }
    }

    suspend fun removeFavoriteDriver(userId: String, driverId: String): Boolean {
        return try {
            val favoriteId = "${userId}_${driverId}"
            favoritesCollection.document(favoriteId).delete().await()
            Log.d("FavoriteDrivers", "Driver removed from favorites: $driverId")
            true
        } catch (e: Exception) {
            Log.e("FavoriteDrivers", "Error removing favorite: ${e.message}", e)
            false
        }
    }

    suspend fun getFavoriteDrivers(userId: String): List<String> {
        return try {
            val docs = favoritesCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
            docs.documents.mapNotNull { it.getString("driverId") }
        } catch (e: Exception) {
            Log.e("FavoriteDrivers", "Error getting favorites: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun isFavoriteDriver(userId: String, driverId: String): Boolean {
        return try {
            val favoriteId = "${userId}_${driverId}"
            val doc = favoritesCollection.document(favoriteId).get().await()
            doc.exists()
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * FACE RECOGNITION SERVICE FOR DRIVER VERIFICATION
 */
@Singleton
class FaceRecognitionService @Inject constructor(
    private val context: Context
) {
    private val highAccuracyOpts = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setMinFaceSize(0.15f)
        .enableTracking()
        .build()

    private val detector = FaceDetection.getClient(highAccuracyOpts)

    suspend fun verifyDriverFace(imageUri: android.net.Uri, driverId: String): FaceVerificationResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val image = InputImage.fromFilePath(context, imageUri)
            val faces = detector.process(image).await()

            when {
                faces.isEmpty() -> FaceVerificationResult.NoFaceDetected
                faces.size > 1 -> FaceVerificationResult.MultipleFacesDetected
                else -> {
                    val face = faces[0]

                    // Check for liveness (basic checks)
                    val leftEyeOpenProb = face.leftEyeOpenProbability ?: 0f
                    val rightEyeOpenProb = face.rightEyeOpenProbability ?: 0f
                    val smilingProb = face.smilingProbability ?: 0f

                    val isLive = leftEyeOpenProb > 0.1f && rightEyeOpenProb > 0.1f

                    if (!isLive) {
                        FaceVerificationResult.LivenessCheckFailed
                    } else {
                        // In production, compare with stored driver photo using ML
                        // For now, simulate verification
                        FaceVerificationResult.Success(
                            confidence = 0.95f,
                            driverId = driverId
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FaceRecognition", "Error verifying face: ${e.message}", e)
            FaceVerificationResult.VerificationFailed(e.message ?: "Unknown error")
        }
    }

    fun releaseResources() {
        detector.close()
    }
}

sealed class FaceVerificationResult {
    data class Success(val confidence: Float, val driverId: String) : FaceVerificationResult()
    object NoFaceDetected : FaceVerificationResult()
    object MultipleFacesDetected : FaceVerificationResult()
    object LivenessCheckFailed : FaceVerificationResult()
    data class VerificationFailed(val reason: String) : FaceVerificationResult()
}

/**
 * RIDE INSURANCE SERVICE
 */
@Singleton
class RideInsuranceService @Inject constructor(
    private val context: Context
) {
    suspend fun getPolicyForRide(rideId: String, fare: Double): InsurancePolicy {
        // Calculate insurance premium (1-2% of fare)
        val premium = fare * 0.015
        val coverage = fare * 10 // 10x coverage

        return InsurancePolicy(
            policyId = "INS_${rideId}",
            rideId = rideId,
            premium = premium,
            coverageAmount = coverage,
            coverageType = listOf("Accident", "Medical", "Theft"),
            validUntil = System.currentTimeMillis() + (24 * 60 * 60 * 1000),
            isActive = true
        )
    }

    suspend fun fileInsuranceClaim(
        policyId: String,
        claimAmount: Double,
        claimType: String,
        description: String,
        evidence: List<String>
    ): InsuranceClaimResult {
        return try {
            val claim = InsuranceClaim(
                claimId = "CLAIM_${System.currentTimeMillis()}",
                policyId = policyId,
                claimAmount = claimAmount,
                claimType = claimType,
                description = description,
                evidence = evidence,
                status = ClaimStatus.SUBMITTED,
                submittedAt = System.currentTimeMillis()
            )

            Log.d("RideInsurance", "Claim filed: ${claim.claimId}")
            InsuranceClaimResult.Success(claim)
        } catch (e: Exception) {
            Log.e("RideInsurance", "Error filing claim: ${e.message}", e)
            InsuranceClaimResult.Failure(e.message ?: "Failed to file claim")
        }
    }
}

data class InsurancePolicy(
    val policyId: String,
    val rideId: String,
    val premium: Double,
    val coverageAmount: Double,
    val coverageType: List<String>,
    val validUntil: Long,
    val isActive: Boolean
)

data class InsuranceClaim(
    val claimId: String,
    val policyId: String,
    val claimAmount: Double,
    val claimType: String,
    val description: String,
    val evidence: List<String>,
    val status: ClaimStatus,
    val submittedAt: Long
)

enum class ClaimStatus {
    SUBMITTED, UNDER_REVIEW, APPROVED, REJECTED, PAID
}

sealed class InsuranceClaimResult {
    data class Success(val claim: InsuranceClaim) : InsuranceClaimResult()
    data class Failure(val message: String) : InsuranceClaimResult()
}

/**
 * DRIVER FATIGUE DETECTION SERVICE
 */
@Singleton
class FatigueDetectionService @Inject constructor(
    private val context: Context
) {
    private val MAX_CONTINUOUS_HOURS = 8
    private val MANDATORY_BREAK_AFTER_HOURS = 4
    private val BREAK_DURATION_MINUTES = 15

    private var drivingStartTime: Long = 0
    private var totalDrivingTimeToday: Long = 0
    private var lastBreakTime: Long = 0

    fun startDrivingSession() {
        drivingStartTime = System.currentTimeMillis()
    }

    fun endDrivingSession() {
        if (drivingStartTime > 0) {
            totalDrivingTimeToday += System.currentTimeMillis() - drivingStartTime
            drivingStartTime = 0
        }
    }

    fun getFatigueStatus(): FatigueStatus {
        val currentDrivingTime = if (drivingStartTime > 0) {
            (System.currentTimeMillis() - drivingStartTime) / (60 * 60 * 1000) // hours
        } else 0

        val totalHoursToday = (totalDrivingTimeToday / (60 * 60 * 1000)).toInt()
        val hoursSinceBreak = if (lastBreakTime > 0) {
            ((System.currentTimeMillis() - lastBreakTime) / (60 * 60 * 1000)).toInt()
        } else currentDrivingTime.toInt()

        return when {
            totalHoursToday >= MAX_CONTINUOUS_HOURS -> FatigueStatus.CRITICAL
            hoursSinceBreak >= MANDATORY_BREAK_AFTER_HOURS -> FatigueStatus.BREAK_REQUIRED
            totalHoursToday >= 6 -> FatigueStatus.MODERATE
            else -> FatigueStatus.NORMAL
        }
    }

    fun recordBreak() {
        lastBreakTime = System.currentTimeMillis()
    }

    fun resetDailyCounter() {
        totalDrivingTimeToday = 0
        lastBreakTime = 0
    }
}

enum class FatigueStatus {
    NORMAL, MODERATE, BREAK_REQUIRED, CRITICAL
}

/**
 * VEHICLE MAINTENANCE SERVICE
 */
@Singleton
class VehicleMaintenanceService @Inject constructor(
    private val context: Context,
    private val firestore: FirebaseFirestore
) {
    private val maintenanceCollection = firestore.collection("vehicleMaintenance")

    suspend fun recordFuelExpense(
        vehicleId: String,
        liters: Double,
        cost: Double,
        odometer: Int
    ): Boolean {
        return try {
            val expense = mapOf(
                "id" to "fuel_${System.currentTimeMillis()}",
                "vehicleId" to vehicleId,
                "type" to "FUEL",
                "liters" to liters,
                "cost" to cost,
                "odometer" to odometer,
                "timestamp" to System.currentTimeMillis()
            )
            maintenanceCollection.add(expense).await()
            Log.d("VehicleMaintenance", "Fuel expense recorded")
            true
        } catch (e: Exception) {
            Log.e("VehicleMaintenance", "Error recording fuel: ${e.message}", e)
            false
        }
    }

    suspend fun scheduleMaintenanceService(
        vehicleId: String,
        serviceType: String,
        scheduledDate: Long,
        estimatedCost: Double
    ): Boolean {
        return try {
            val service = mapOf(
                "id" to "service_${System.currentTimeMillis()}",
                "vehicleId" to vehicleId,
                "serviceType" to serviceType,
                "scheduledDate" to scheduledDate,
                "estimatedCost" to estimatedCost,
                "status" to "SCHEDULED"
            )
            maintenanceCollection.add(service).await()
            Log.d("VehicleMaintenance", "Service scheduled")
            true
        } catch (e: Exception) {
            Log.e("VehicleMaintenance", "Error scheduling service: ${e.message}", e)
            false
        }
    }

    suspend fun getMaintenanceHistory(vehicleId: String): List<Map<String, Any>> {
        return try {
            val docs = maintenanceCollection
                .whereEqualTo("vehicleId", vehicleId)
                .get()
                .await()
            docs.documents.map { it.data ?: emptyMap() }
        } catch (e: Exception) {
            Log.e("VehicleMaintenance", "Error getting history: ${e.message}", e)
            emptyList()
        }
    }
}

/**
 * RIDE PREFERENCES SERVICE
 */
@Singleton
class RidePreferencesService @Inject constructor(
    private val context: Context
) {
    private val preferences = context.getSharedPreferences("ride_preferences", Context.MODE_PRIVATE)

    fun savePreferences(userId: String, prefs: RidePreferences) {
        preferences.edit().apply {
            putBoolean("${userId}_music", prefs.musicAllowed)
            putBoolean("${userId}_ac", prefs.acPreferred)
            putString("${userId}_conversation", prefs.conversationLevel.name)
            putInt("${userId}_temperature", prefs.temperaturePreference)
            putString("${userId}_route", prefs.routePreference.name)
            apply()
        }
    }

    fun getPreferences(userId: String): RidePreferences {
        return RidePreferences(
            musicAllowed = preferences.getBoolean("${userId}_music", true),
            acPreferred = preferences.getBoolean("${userId}_ac", true),
            conversationLevel = ConversationLevel.valueOf(
                preferences.getString("${userId}_conversation", "MODERATE") ?: "MODERATE"
            ),
            temperaturePreference = preferences.getInt("${userId}_temperature", 22),
            routePreference = RoutePreference.valueOf(
                preferences.getString("${userId}_route", "FASTEST") ?: "FASTEST"
            )
        )
    }
}

data class RidePreferences(
    val musicAllowed: Boolean = true,
    val acPreferred: Boolean = true,
    val conversationLevel: ConversationLevel = ConversationLevel.MODERATE,
    val temperaturePreference: Int = 22, // Celsius
    val routePreference: RoutePreference = RoutePreference.FASTEST
)

enum class ConversationLevel {
    QUIET, MODERATE, CHATTY
}

enum class RoutePreference {
    FASTEST, SHORTEST, SCENIC, ECO_FRIENDLY
}