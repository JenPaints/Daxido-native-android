package com.daxido.core.ridepooling

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
 * Ride pooling/sharing service for shared rides
 */
@Singleton
class RidePoolingService @Inject constructor(
    private val context: Context
) {
    
    private val activePoolRides = mutableMapOf<String, PoolRide>()
    private val waitingPassengers = mutableListOf<PoolPassenger>()
    
    /**
     * Create a new pool ride
     */
    suspend fun createPoolRide(
        rideId: String,
        driverId: String,
        vehicleType: String,
        maxPassengers: Int = 4,
        pickupLocation: LatLng,
        dropoffLocation: LatLng,
        baseFare: Double
    ): PoolRideResult = withContext(Dispatchers.IO) {
        try {
            val poolRide = PoolRide(
                id = rideId,
                driverId = driverId,
                vehicleType = vehicleType,
                maxPassengers = maxPassengers,
                currentPassengers = 0,
                pickupLocation = pickupLocation,
                dropoffLocation = dropoffLocation,
                baseFare = baseFare,
                sharedFare = calculateSharedFare(baseFare, maxPassengers),
                status = PoolRideStatus.WAITING_FOR_PASSENGERS,
                passengers = emptyList(),
                createdAt = System.currentTimeMillis()
            )
            
            activePoolRides[rideId] = poolRide
            
            Log.d("RidePoolingService", "Pool ride created: $rideId")
            PoolRideResult.Success(poolRide)
        } catch (e: Exception) {
            Log.e("RidePoolingService", "Error creating pool ride: ${e.message}", e)
            PoolRideResult.Failure("Failed to create pool ride: ${e.message}")
        }
    }
    
    /**
     * Add passenger to pool ride
     */
    suspend fun addPassengerToPool(
        rideId: String,
        passengerId: String,
        pickupLocation: LatLng,
        dropoffLocation: LatLng,
        passengerName: String
    ): PoolPassengerResult = withContext(Dispatchers.IO) {
        try {
            val poolRide = activePoolRides[rideId]
                ?: return@withContext PoolPassengerResult.Failure("Pool ride not found")
            
            if (poolRide.currentPassengers >= poolRide.maxPassengers) {
                return@withContext PoolPassengerResult.Failure("Pool ride is full")
            }
            
            val passenger = PoolPassenger(
                id = passengerId,
                name = passengerName,
                pickupLocation = pickupLocation,
                dropoffLocation = dropoffLocation,
                fare = poolRide.sharedFare,
                status = PoolPassengerStatus.WAITING,
                joinedAt = System.currentTimeMillis()
            )
            
            val updatedPassengers = poolRide.passengers + passenger
            val updatedPoolRide = poolRide.copy(
                passengers = updatedPassengers,
                currentPassengers = updatedPassengers.size,
                status = if (updatedPassengers.size >= poolRide.maxPassengers) {
                    PoolRideStatus.READY_TO_START
                } else {
                    PoolRideStatus.WAITING_FOR_PASSENGERS
                }
            )
            
            activePoolRides[rideId] = updatedPoolRide
            
            Log.d("RidePoolingService", "Passenger added to pool: $passengerId")
            PoolPassengerResult.Success(passenger)
        } catch (e: Exception) {
            Log.e("RidePoolingService", "Error adding passenger to pool: ${e.message}", e)
            PoolPassengerResult.Failure("Failed to add passenger: ${e.message}")
        }
    }
    
    /**
     * Remove passenger from pool ride
     */
    suspend fun removePassengerFromPool(
        rideId: String,
        passengerId: String
    ): PoolPassengerResult = withContext(Dispatchers.IO) {
        try {
            val poolRide = activePoolRides[rideId]
                ?: return@withContext PoolPassengerResult.Failure("Pool ride not found")
            
            val updatedPassengers = poolRide.passengers.filter { it.id != passengerId }
            val updatedPoolRide = poolRide.copy(
                passengers = updatedPassengers,
                currentPassengers = updatedPassengers.size,
                status = if (updatedPassengers.isEmpty()) {
                    PoolRideStatus.CANCELLED
                } else {
                    PoolRideStatus.WAITING_FOR_PASSENGERS
                }
            )
            
            activePoolRides[rideId] = updatedPoolRide
            
            Log.d("RidePoolingService", "Passenger removed from pool: $passengerId")
            PoolPassengerResult.Success(poolRide.passengers.find { it.id == passengerId }!!)
        } catch (e: Exception) {
            Log.e("RidePoolingService", "Error removing passenger from pool: ${e.message}", e)
            PoolPassengerResult.Failure("Failed to remove passenger: ${e.message}")
        }
    }
    
    /**
     * Start pool ride
     */
    suspend fun startPoolRide(rideId: String): PoolRideResult = withContext(Dispatchers.IO) {
        try {
            val poolRide = activePoolRides[rideId]
                ?: return@withContext PoolRideResult.Failure("Pool ride not found")
            
            if (poolRide.currentPassengers == 0) {
                return@withContext PoolRideResult.Failure("No passengers in pool ride")
            }
            
            val updatedPoolRide = poolRide.copy(
                status = PoolRideStatus.IN_PROGRESS,
                startedAt = System.currentTimeMillis()
            )
            
            activePoolRides[rideId] = updatedPoolRide
            
            Log.d("RidePoolingService", "Pool ride started: $rideId")
            PoolRideResult.Success(updatedPoolRide)
        } catch (e: Exception) {
            Log.e("RidePoolingService", "Error starting pool ride: ${e.message}", e)
            PoolRideResult.Failure("Failed to start pool ride: ${e.message}")
        }
    }
    
    /**
     * Complete pool ride
     */
    suspend fun completePoolRide(rideId: String): PoolRideResult = withContext(Dispatchers.IO) {
        try {
            val poolRide = activePoolRides[rideId]
                ?: return@withContext PoolRideResult.Failure("Pool ride not found")
            
            val updatedPoolRide = poolRide.copy(
                status = PoolRideStatus.COMPLETED,
                completedAt = System.currentTimeMillis()
            )
            
            activePoolRides[rideId] = updatedPoolRide
            
            Log.d("RidePoolingService", "Pool ride completed: $rideId")
            PoolRideResult.Success(updatedPoolRide)
        } catch (e: Exception) {
            Log.e("RidePoolingService", "Error completing pool ride: ${e.message}", e)
            PoolRideResult.Failure("Failed to complete pool ride: ${e.message}")
        }
    }
    
    /**
     * Get pool ride details
     */
    suspend fun getPoolRide(rideId: String): PoolRide? = withContext(Dispatchers.IO) {
        return@withContext activePoolRides[rideId]
    }
    
    /**
     * Get available pool rides near location
     */
    suspend fun getAvailablePoolRides(
        location: LatLng,
        radius: Double = 5.0 // km
    ): Flow<List<PoolRide>> = flow {
        val availableRides = activePoolRides.values.filter { poolRide ->
            poolRide.status == PoolRideStatus.WAITING_FOR_PASSENGERS &&
            poolRide.currentPassengers < poolRide.maxPassengers &&
            isWithinRadius(poolRide.pickupLocation, location, radius)
        }
        emit(availableRides)
    }
    
    /**
     * Calculate shared fare based on base fare and passenger count
     */
    private fun calculateSharedFare(baseFare: Double, maxPassengers: Int): Double {
        // Shared fare is typically 60-80% of base fare
        val discountFactor = 0.7
        return baseFare * discountFactor
    }
    
    /**
     * Check if location is within radius
     */
    private fun isWithinRadius(location1: LatLng, location2: LatLng, radiusKm: Double): Boolean {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            location1.latitude, location1.longitude,
            location2.latitude, location2.longitude,
            results
        )
        val distanceKm = results[0] / 1000.0
        return distanceKm <= radiusKm
    }
}

data class PoolRide(
    val id: String,
    val driverId: String,
    val vehicleType: String,
    val maxPassengers: Int,
    val currentPassengers: Int,
    val pickupLocation: LatLng,
    val dropoffLocation: LatLng,
    val baseFare: Double,
    val sharedFare: Double,
    val status: PoolRideStatus,
    val passengers: List<PoolPassenger>,
    val createdAt: Long,
    val startedAt: Long? = null,
    val completedAt: Long? = null
)

data class PoolPassenger(
    val id: String,
    val name: String,
    val pickupLocation: LatLng,
    val dropoffLocation: LatLng,
    val fare: Double,
    val status: PoolPassengerStatus,
    val joinedAt: Long
)

enum class PoolRideStatus {
    WAITING_FOR_PASSENGERS,
    READY_TO_START,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

enum class PoolPassengerStatus {
    WAITING,
    PICKED_UP,
    DROPPED_OFF,
    CANCELLED
}

sealed class PoolRideResult {
    data class Success(val poolRide: PoolRide) : PoolRideResult()
    data class Failure(val message: String) : PoolRideResult()
}

sealed class PoolPassengerResult {
    data class Success(val passenger: PoolPassenger) : PoolPassengerResult()
    data class Failure(val message: String) : PoolPassengerResult()
}
