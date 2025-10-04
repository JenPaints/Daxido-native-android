package com.daxido.core.services

import com.daxido.core.models.DriverRating
import com.daxido.core.models.RatingTags
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rating and review system (Ola/Uber/Rapido style)
 */
@Singleton
class RatingService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions
) {

    /**
     * Submit driver rating and review
     */
    suspend fun rateDriver(
        rideId: String,
        driverId: String,
        rating: Float,
        review: String? = null,
        tags: List<String> = emptyList(),
        vehicleCleanliness: Int = 0,
        drivingSkill: Int = 0,
        behavior: Int = 0
    ): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))

            val driverRating = DriverRating(
                rideId = rideId,
                driverId = driverId,
                userId = userId,
                rating = rating,
                review = review,
                tags = tags,
                vehicleCleanliness = vehicleCleanliness,
                drivingSkill = drivingSkill,
                behavior = behavior,
                timestamp = com.google.firebase.Timestamp.now()
            )

            // Save rating
            firestore.collection("ratings")
                .document(rideId)
                .set(driverRating)
                .await()

            // Update driver's average rating via Firebase Function
            functions.getHttpsCallable("updateDriverRating")
                .call(mapOf(
                    "driverId" to driverId,
                    "rideId" to rideId,
                    "rating" to rating
                ))
                .await()

            // Update ride with rating
            firestore.collection("rides")
                .document(rideId)
                .update(
                    mapOf(
                        "rating" to rating,
                        "review" to review,
                        "isRated" to true
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Skip rating (track for later)
     */
    suspend fun skipRating(rideId: String): Result<Unit> {
        return try {
            firestore.collection("rides")
                .document(rideId)
                .update("ratingSkipped", true)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get pending ratings (rides that need to be rated)
     */
    suspend fun getPendingRatings(): Result<List<String>> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))

            val snapshot = firestore.collection("rides")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "COMPLETED")
                .whereEqualTo("isRated", false)
                .whereEqualTo("ratingSkipped", false)
                .limit(5)
                .get()
                .await()

            val rideIds = snapshot.documents.map { it.id }
            Result.success(rideIds)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get rating for a ride
     */
    suspend fun getRating(rideId: String): DriverRating? {
        return try {
            val doc = firestore.collection("ratings")
                .document(rideId)
                .get()
                .await()

            doc.toObject(DriverRating::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Report driver (safety issue)
     */
    suspend fun reportDriver(
        rideId: String,
        driverId: String,
        reason: String,
        description: String
    ): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))

            val report = mapOf(
                "rideId" to rideId,
                "driverId" to driverId,
                "userId" to userId,
                "reason" to reason,
                "description" to description,
                "timestamp" to com.google.firebase.Timestamp.now(),
                "status" to "PENDING"
            )

            // Save report
            firestore.collection("driverReports")
                .add(report)
                .await()

            // Notify admin via Firebase Function
            functions.getHttpsCallable("reportDriver")
                .call(report)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get rating tags based on rating
     */
    fun getSuggestedTags(rating: Float): List<String> {
        return if (rating >= 4.0f) {
            RatingTags.POSITIVE
        } else {
            RatingTags.NEGATIVE
        }
    }

    /**
     * Get driver's average rating
     */
    suspend fun getDriverAverageRating(driverId: String): Double? {
        return try {
            val doc = firestore.collection("drivers")
                .document(driverId)
                .get()
                .await()

            doc.getDouble("averageRating")
        } catch (e: Exception) {
            null
        }
    }
}
