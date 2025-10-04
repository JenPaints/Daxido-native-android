package com.daxido.core.rating

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rating system for drivers and riders
 */
@Singleton
class RatingService @Inject constructor(
    private val context: Context
) {
    
    private val ratings = mutableMapOf<String, MutableList<Rating>>()
    
    /**
     * Submit a rating for a ride
     */
    suspend fun submitRating(
        rideId: String,
        fromUserId: String,
        toUserId: String,
        userType: UserType,
        rating: Float,
        feedback: String? = null,
        tags: List<RatingTag> = emptyList()
    ): RatingResult = withContext(Dispatchers.IO) {
        try {
            if (rating < 1.0f || rating > 5.0f) {
                return@withContext RatingResult.Failure("Rating must be between 1.0 and 5.0")
            }
            
            val ratingData = Rating(
                id = generateRatingId(),
                rideId = rideId,
                fromUserId = fromUserId,
                toUserId = toUserId,
                userType = userType,
                rating = rating,
                feedback = feedback,
                tags = tags,
                timestamp = System.currentTimeMillis()
            )
            
            // Store rating
            val userRatings = ratings.getOrPut(toUserId) { mutableListOf() }
            userRatings.add(ratingData)
            
            Log.d("RatingService", "Rating submitted: $rating for user $toUserId")
            
            RatingResult.Success(ratingData)
        } catch (e: Exception) {
            Log.e("RatingService", "Error submitting rating: ${e.message}", e)
            RatingResult.Failure("Failed to submit rating: ${e.message}")
        }
    }
    
    /**
     * Get average rating for a user
     */
    suspend fun getAverageRating(userId: String): Float = withContext(Dispatchers.IO) {
        val userRatings = ratings[userId] ?: return@withContext 0.0f
        
        if (userRatings.isEmpty()) return@withContext 0.0f
        
        val sum = userRatings.sumOf { it.rating.toDouble() }
        return@withContext (sum / userRatings.size).toFloat()
    }
    
    /**
     * Get rating count for a user
     */
    suspend fun getRatingCount(userId: String): Int = withContext(Dispatchers.IO) {
        return@withContext ratings[userId]?.size ?: 0
    }
    
    /**
     * Get detailed rating breakdown for a user
     */
    suspend fun getRatingBreakdown(userId: String): RatingBreakdown = withContext(Dispatchers.IO) {
        val userRatings = ratings[userId] ?: emptyList()
        
        val breakdown = RatingBreakdown(
            averageRating = if (userRatings.isNotEmpty()) {
                userRatings.sumOf { it.rating.toDouble() } / userRatings.size
            } else 0.0,
            totalRatings = userRatings.size,
            ratingDistribution = mapOf(
                5 to userRatings.count { it.rating >= 4.5f },
                4 to userRatings.count { it.rating >= 3.5f && it.rating < 4.5f },
                3 to userRatings.count { it.rating >= 2.5f && it.rating < 3.5f },
                2 to userRatings.count { it.rating >= 1.5f && it.rating < 2.5f },
                1 to userRatings.count { it.rating < 1.5f }
            ),
            recentRatings = userRatings.takeLast(10),
            commonTags = getCommonTags(userRatings)
        )
        
        return@withContext breakdown
    }
    
    /**
     * Get ratings for a specific ride
     */
    suspend fun getRideRatings(rideId: String): List<Rating> = withContext(Dispatchers.IO) {
        return@withContext ratings.values.flatten().filter { it.rideId == rideId }
    }
    
    /**
     * Check if user has rated for a specific ride
     */
    suspend fun hasRated(rideId: String, userId: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext ratings.values.flatten().any { 
            it.rideId == rideId && it.fromUserId == userId 
        }
    }
    
    /**
     * Get rating history for a user
     */
    suspend fun getRatingHistory(userId: String): Flow<List<Rating>> = flow {
        val userRatings = ratings[userId] ?: emptyList()
        emit(userRatings.sortedByDescending { it.timestamp })
    }
    
    private fun getCommonTags(ratings: List<Rating>): Map<RatingTag, Int> {
        val tagCounts = mutableMapOf<RatingTag, Int>()
        
        ratings.forEach { rating ->
            rating.tags.forEach { tag ->
                tagCounts[tag] = tagCounts.getOrDefault(tag, 0) + 1
            }
        }
        
        return tagCounts.toMap()
    }
    
    private fun generateRatingId(): String {
        return "rating_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
}

data class Rating(
    val id: String,
    val rideId: String,
    val fromUserId: String,
    val toUserId: String,
    val userType: UserType,
    val rating: Float,
    val feedback: String?,
    val tags: List<RatingTag>,
    val timestamp: Long
)

data class RatingBreakdown(
    val averageRating: Double,
    val totalRatings: Int,
    val ratingDistribution: Map<Int, Int>,
    val recentRatings: List<Rating>,
    val commonTags: Map<RatingTag, Int>
)

enum class UserType {
    RIDER, DRIVER
}

enum class RatingTag(val displayName: String) {
    // Driver tags
    SAFE_DRIVING("Safe Driving"),
    PUNCTUAL("Punctual"),
    FRIENDLY("Friendly"),
    CLEAN_VEHICLE("Clean Vehicle"),
    GOOD_ROUTE("Good Route"),
    HELPFUL("Helpful"),
    
    // Rider tags
    POLITE("Polite"),
    ON_TIME("On Time"),
    RESPECTFUL("Respectful"),
    CLEAN("Clean"),
    GOOD_COMMUNICATION("Good Communication"),
    FOLLOWS_RULES("Follows Rules")
}

sealed class RatingResult {
    data class Success(val rating: Rating) : RatingResult()
    data class Failure(val message: String) : RatingResult()
}
