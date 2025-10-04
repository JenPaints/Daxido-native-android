package com.daxido.core.multistop

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import com.google.android.gms.maps.model.LatLng
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Multi-stop rides service for rides with multiple destinations
 */
@Singleton
class MultiStopRidesService @Inject constructor(
    private val context: Context
) {
    
    private val multiStopRides = mutableMapOf<String, MultiStopRide>()
    
    /**
     * Create a multi-stop ride
     */
    suspend fun createMultiStopRide(
        rideId: String,
        userId: String,
        driverId: String,
        stops: List<RideStop>,
        vehicleType: String,
        baseFare: Double
    ): MultiStopRideResult = withContext(Dispatchers.IO) {
        try {
            if (stops.size < 2) {
                return@withContext MultiStopRideResult.Failure("Multi-stop ride must have at least 2 stops")
            }
            
            val optimizedStops = optimizeStopOrder(stops)
            val totalFare = calculateMultiStopFare(baseFare, optimizedStops.size)
            
            val multiStopRide = MultiStopRide(
                id = rideId,
                userId = userId,
                driverId = driverId,
                stops = optimizedStops,
                vehicleType = vehicleType,
                baseFare = baseFare,
                totalFare = totalFare,
                status = MultiStopRideStatus.CREATED,
                currentStopIndex = 0,
                createdAt = System.currentTimeMillis()
            )
            
            multiStopRides[rideId] = multiStopRide
            
            Log.d("MultiStopRidesService", "Multi-stop ride created: $rideId with ${stops.size} stops")
            MultiStopRideResult.Success(multiStopRide)
        } catch (e: Exception) {
            Log.e("MultiStopRidesService", "Error creating multi-stop ride: ${e.message}", e)
            MultiStopRideResult.Failure("Failed to create multi-stop ride: ${e.message}")
        }
    }
    
    /**
     * Add a stop to existing multi-stop ride
     */
    suspend fun addStopToRide(
        rideId: String,
        stop: RideStop
    ): MultiStopRideResult = withContext(Dispatchers.IO) {
        try {
            val multiStopRide = multiStopRides[rideId]
                ?: return@withContext MultiStopRideResult.Failure("Multi-stop ride not found")
            
            if (multiStopRide.status != MultiStopRideStatus.CREATED) {
                return@withContext MultiStopRideResult.Failure("Cannot add stop to ride in progress")
            }
            
            val updatedStops = multiStopRide.stops + stop
            val optimizedStops = optimizeStopOrder(updatedStops)
            val totalFare = calculateMultiStopFare(multiStopRide.baseFare, optimizedStops.size)
            
            val updatedRide = multiStopRide.copy(
                stops = optimizedStops,
                totalFare = totalFare
            )
            
            multiStopRides[rideId] = updatedRide
            
            Log.d("MultiStopRidesService", "Stop added to ride: $rideId")
            MultiStopRideResult.Success(updatedRide)
        } catch (e: Exception) {
            Log.e("MultiStopRidesService", "Error adding stop to ride: ${e.message}", e)
            MultiStopRideResult.Failure("Failed to add stop: ${e.message}")
        }
    }
    
    /**
     * Remove a stop from multi-stop ride
     */
    suspend fun removeStopFromRide(
        rideId: String,
        stopId: String
    ): MultiStopRideResult = withContext(Dispatchers.IO) {
        try {
            val multiStopRide = multiStopRides[rideId]
                ?: return@withContext MultiStopRideResult.Failure("Multi-stop ride not found")
            
            if (multiStopRide.status != MultiStopRideStatus.CREATED) {
                return@withContext MultiStopRideResult.Failure("Cannot remove stop from ride in progress")
            }
            
            val updatedStops = multiStopRide.stops.filter { it.id != stopId }
            if (updatedStops.size < 2) {
                return@withContext MultiStopRideResult.Failure("Multi-stop ride must have at least 2 stops")
            }
            
            val optimizedStops = optimizeStopOrder(updatedStops)
            val totalFare = calculateMultiStopFare(multiStopRide.baseFare, optimizedStops.size)
            
            val updatedRide = multiStopRide.copy(
                stops = optimizedStops,
                totalFare = totalFare
            )
            
            multiStopRides[rideId] = updatedRide
            
            Log.d("MultiStopRidesService", "Stop removed from ride: $rideId")
            MultiStopRideResult.Success(updatedRide)
        } catch (e: Exception) {
            Log.e("MultiStopRidesService", "Error removing stop from ride: ${e.message}", e)
            MultiStopRideResult.Failure("Failed to remove stop: ${e.message}")
        }
    }
    
    /**
     * Start multi-stop ride
     */
    suspend fun startMultiStopRide(rideId: String): MultiStopRideResult = withContext(Dispatchers.IO) {
        try {
            val multiStopRide = multiStopRides[rideId]
                ?: return@withContext MultiStopRideResult.Failure("Multi-stop ride not found")
            
            val updatedRide = multiStopRide.copy(
                status = MultiStopRideStatus.IN_PROGRESS,
                startedAt = System.currentTimeMillis()
            )
            
            multiStopRides[rideId] = updatedRide
            
            Log.d("MultiStopRidesService", "Multi-stop ride started: $rideId")
            MultiStopRideResult.Success(updatedRide)
        } catch (e: Exception) {
            Log.e("MultiStopRidesService", "Error starting multi-stop ride: ${e.message}", e)
            MultiStopRideResult.Failure("Failed to start multi-stop ride: ${e.message}")
        }
    }
    
    /**
     * Complete current stop and move to next
     */
    suspend fun completeCurrentStop(rideId: String): MultiStopRideResult = withContext(Dispatchers.IO) {
        try {
            val multiStopRide = multiStopRides[rideId]
                ?: return@withContext MultiStopRideResult.Failure("Multi-stop ride not found")
            
            if (multiStopRide.status != MultiStopRideStatus.IN_PROGRESS) {
                return@withContext MultiStopRideResult.Failure("Ride is not in progress")
            }
            
            val currentStopIndex = multiStopRide.currentStopIndex
            val currentStop = multiStopRide.stops[currentStopIndex]
            
            val updatedStops = multiStopRide.stops.toMutableList()
            updatedStops[currentStopIndex] = currentStop.copy(
                status = StopStatus.COMPLETED,
                completedAt = System.currentTimeMillis()
            )
            
            val nextStopIndex = currentStopIndex + 1
            val isCompleted = nextStopIndex >= multiStopRide.stops.size
            
            val updatedRide = multiStopRide.copy(
                stops = updatedStops,
                currentStopIndex = if (isCompleted) currentStopIndex else nextStopIndex,
                status = if (isCompleted) MultiStopRideStatus.COMPLETED else MultiStopRideStatus.IN_PROGRESS,
                completedAt = if (isCompleted) System.currentTimeMillis() else null
            )
            
            multiStopRides[rideId] = updatedRide
            
            Log.d("MultiStopRidesService", "Stop completed: $rideId, next stop: $nextStopIndex")
            MultiStopRideResult.Success(updatedRide)
        } catch (e: Exception) {
            Log.e("MultiStopRidesService", "Error completing stop: ${e.message}", e)
            MultiStopRideResult.Failure("Failed to complete stop: ${e.message}")
        }
    }
    
    /**
     * Get multi-stop ride details
     */
    suspend fun getMultiStopRide(rideId: String): MultiStopRide? = withContext(Dispatchers.IO) {
        return@withContext multiStopRides[rideId]
    }
    
    /**
     * Get current stop for multi-stop ride
     */
    suspend fun getCurrentStop(rideId: String): RideStop? = withContext(Dispatchers.IO) {
        val multiStopRide = multiStopRides[rideId] ?: return@withContext null
        return@withContext if (multiStopRide.currentStopIndex < multiStopRide.stops.size) {
            multiStopRide.stops[multiStopRide.currentStopIndex]
        } else null
    }
    
    /**
     * Get remaining stops for multi-stop ride
     */
    suspend fun getRemainingStops(rideId: String): List<RideStop> = withContext(Dispatchers.IO) {
        val multiStopRide = multiStopRides[rideId] ?: return@withContext emptyList()
        return@withContext multiStopRide.stops.drop(multiStopRide.currentStopIndex + 1)
    }
    
    /**
     * Optimize stop order for efficient routing
     */
    private fun optimizeStopOrder(stops: List<RideStop>): List<RideStop> {
        if (stops.size <= 2) return stops
        
        // Simple optimization: sort by distance from first stop
        val firstStop = stops.first()
        val otherStops = stops.drop(1).sortedBy { stop ->
            calculateDistance(firstStop.location, stop.location)
        }
        
        return listOf(firstStop) + otherStops
    }
    
    /**
     * Calculate fare for multi-stop ride
     */
    private fun calculateMultiStopFare(baseFare: Double, stopCount: Int): Double {
        // Multi-stop rides typically have a discount per additional stop
        val additionalStopDiscount = 0.1 // 10% discount per additional stop
        val additionalStops = stopCount - 1
        val discount = additionalStops * additionalStopDiscount
        return baseFare * (1.0 - discount)
    }
    
    /**
     * Calculate distance between two locations
     */
    private fun calculateDistance(location1: LatLng, location2: LatLng): Double {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            location1.latitude, location1.longitude,
            location2.latitude, location2.longitude,
            results
        )
        return results[0].toDouble()
    }
}

data class MultiStopRide(
    val id: String,
    val userId: String,
    val driverId: String,
    val stops: List<RideStop>,
    val vehicleType: String,
    val baseFare: Double,
    val totalFare: Double,
    val status: MultiStopRideStatus,
    val currentStopIndex: Int,
    val createdAt: Long,
    val startedAt: Long? = null,
    val completedAt: Long? = null
)

data class RideStop(
    val id: String,
    val location: LatLng,
    val address: String,
    val stopType: StopType,
    val notes: String? = null,
    val status: StopStatus = StopStatus.PENDING,
    val arrivedAt: Long? = null,
    val completedAt: Long? = null
)

enum class StopType {
    PICKUP,
    DROPOFF,
    WAYPOINT
}

enum class StopStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    SKIPPED
}

enum class MultiStopRideStatus {
    CREATED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

sealed class MultiStopRideResult {
    data class Success(val multiStopRide: MultiStopRide) : MultiStopRideResult()
    data class Failure(val message: String) : MultiStopRideResult()
}
