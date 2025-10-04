package com.daxido.core.algorithms

import com.daxido.core.location.LocationData
import com.daxido.core.maps.DirectionsApiService
import com.daxido.core.maps.ETAResult
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real ride allocation engine with actual driver matching
 */
@Singleton
class RealRideAllocationEngine @Inject constructor(
    private val directionsApiService: DirectionsApiService
) {
    
    /**
     * Find the best driver for a ride request
     */
    suspend fun findBestDriver(
        pickupLocation: LatLng,
        dropoffLocation: LatLng,
        vehicleType: String,
        availableDrivers: List<DriverLocation>
    ): DriverAllocationResult? = coroutineScope {
        
        if (availableDrivers.isEmpty()) {
            return@coroutineScope null
        }
        
        // Filter drivers by vehicle type
        val filteredDrivers = availableDrivers.filter { driver ->
            driver.vehicleType == vehicleType && driver.isAvailable
        }
        
        if (filteredDrivers.isEmpty()) {
            return@coroutineScope null
        }
        
        // Calculate scores for each driver
        val driverScores = mutableListOf<DriverScore>()
        
        for (driver in filteredDrivers) {
            val score = calculateDriverScore(
                driver = driver,
                pickupLocation = pickupLocation,
                dropoffLocation = dropoffLocation
            )
            driverScores.add(score)
        }
        
        // Sort by score (highest first)
        driverScores.sortByDescending { it.totalScore }
        
        // Return the best driver
        val bestDriver = driverScores.first()
        
        DriverAllocationResult(
            driverId = bestDriver.driverId,
            driverName = bestDriver.driverName,
            vehicleType = bestDriver.vehicleType,
            vehicleNumber = bestDriver.vehicleNumber,
            estimatedArrivalTime = bestDriver.estimatedArrivalTime,
            estimatedFare = bestDriver.estimatedFare,
            distanceToPickup = bestDriver.distanceToPickup,
            driverRating = bestDriver.driverRating,
            allocationScore = bestDriver.totalScore,
            allocationTime = System.currentTimeMillis()
        )
    }
    
    /**
     * Calculate comprehensive driver score
     */
    private suspend fun calculateDriverScore(
        driver: DriverLocation,
        pickupLocation: LatLng,
        dropoffLocation: LatLng
    ): DriverScore {
        
        // Calculate distance to pickup
        val distanceToPickup = calculateDistance(
            driver.currentLocation.toLatLng(),
            pickupLocation
        )
        
        // Calculate ETA to pickup
        val etaToPickup = directionsApiService.getETA(
            origin = driver.currentLocation.toLatLng(),
            destination = pickupLocation
        )
        
        // Calculate trip distance and duration
        val tripETA = directionsApiService.getETA(
            origin = pickupLocation,
            destination = dropoffLocation
        )
        
        // Calculate fare estimate
        val estimatedFare = calculateFare(
            distanceToPickup = distanceToPickup,
            tripDistance = tripETA?.distance ?: 0,
            tripDuration = tripETA?.duration ?: 0,
            vehicleType = driver.vehicleType,
            baseFare = getBaseFare(driver.vehicleType),
            perKmRate = getPerKmRate(driver.vehicleType),
            perMinuteRate = getPerMinuteRate(driver.vehicleType)
        )
        
        // Calculate various score components
        val distanceScore = calculateDistanceScore(distanceToPickup)
        val etaScore = calculateETAScore(etaToPickup?.duration ?: 0)
        val ratingScore = calculateRatingScore(driver.rating)
        val availabilityScore = calculateAvailabilityScore(driver.lastSeen)
        val fareScore = calculateFareScore(estimatedFare, driver.vehicleType)
        
        // Weighted total score
        val totalScore = (
            distanceScore * 0.3f +
            etaScore * 0.25f +
            ratingScore * 0.2f +
            availabilityScore * 0.15f +
            fareScore * 0.1f
        )
        
        return DriverScore(
            driverId = driver.driverId,
            driverName = driver.driverName,
            vehicleType = driver.vehicleType,
            vehicleNumber = driver.vehicleNumber,
            driverRating = driver.rating,
            distanceToPickup = distanceToPickup,
            estimatedArrivalTime = etaToPickup?.duration ?: 0,
            estimatedFare = estimatedFare,
            totalScore = totalScore
        )
    }
    
    /**
     * Calculate distance score (closer is better)
     */
    private fun calculateDistanceScore(distance: Float): Float {
        return when {
            distance <= 500f -> 1.0f      // Within 500m
            distance <= 1000f -> 0.8f     // Within 1km
            distance <= 2000f -> 0.6f     // Within 2km
            distance <= 5000f -> 0.4f     // Within 5km
            else -> 0.2f                  // Beyond 5km
        }
    }
    
    /**
     * Calculate ETA score (faster is better)
     */
    private fun calculateETAScore(etaSeconds: Int): Float {
        val etaMinutes = etaSeconds / 60f
        return when {
            etaMinutes <= 3f -> 1.0f      // Within 3 minutes
            etaMinutes <= 5f -> 0.8f      // Within 5 minutes
            etaMinutes <= 10f -> 0.6f     // Within 10 minutes
            etaMinutes <= 15f -> 0.4f     // Within 15 minutes
            else -> 0.2f                  // Beyond 15 minutes
        }
    }
    
    /**
     * Calculate rating score
     */
    private fun calculateRatingScore(rating: Float): Float {
        return when {
            rating >= 4.8f -> 1.0f       // Excellent
            rating >= 4.5f -> 0.8f       // Very good
            rating >= 4.0f -> 0.6f       // Good
            rating >= 3.5f -> 0.4f       // Fair
            else -> 0.2f                 // Poor
        }
    }
    
    /**
     * Calculate availability score (recently active is better)
     */
    private fun calculateAvailabilityScore(lastSeen: Long): Float {
        val timeSinceLastSeen = System.currentTimeMillis() - lastSeen
        val minutesSinceLastSeen = timeSinceLastSeen / (1000 * 60)
        
        return when {
            minutesSinceLastSeen <= 2 -> 1.0f    // Within 2 minutes
            minutesSinceLastSeen <= 5 -> 0.8f    // Within 5 minutes
            minutesSinceLastSeen <= 10 -> 0.6f   // Within 10 minutes
            minutesSinceLastSeen <= 30 -> 0.4f    // Within 30 minutes
            else -> 0.2f                         // Beyond 30 minutes
        }
    }
    
    /**
     * Calculate fare score (reasonable fare is better)
     */
    private fun calculateFareScore(fare: Double, vehicleType: String): Float {
        val expectedFareRange = getExpectedFareRange(vehicleType)
        return when {
            fare <= expectedFareRange.first -> 1.0f
            fare <= expectedFareRange.second -> 0.8f
            fare <= expectedFareRange.second * 1.2 -> 0.6f
            fare <= expectedFareRange.second * 1.5 -> 0.4f
            else -> 0.2f
        }
    }
    
    /**
     * Calculate fare based on distance and time
     */
    private fun calculateFare(
        distanceToPickup: Float,
        tripDistance: Int,
        tripDuration: Int,
        vehicleType: String,
        baseFare: Double,
        perKmRate: Double,
        perMinuteRate: Double
    ): Double {
        val pickupDistanceKm = distanceToPickup / 1000f
        val tripDistanceKm = tripDistance / 1000f
        val tripDurationMinutes = tripDuration / 60f
        
        return baseFare + (pickupDistanceKm * perKmRate) + (tripDistanceKm * perKmRate) + (tripDurationMinutes * perMinuteRate)
    }
    
    /**
     * Get base fare for vehicle type
     */
    private fun getBaseFare(vehicleType: String): Double {
        return when (vehicleType.lowercase()) {
            "economy" -> 2.0
            "comfort" -> 3.0
            "premium" -> 5.0
            "luxury" -> 8.0
            "suv" -> 4.0
            "bike" -> 1.0
            else -> 2.5
        }
    }
    
    /**
     * Get per km rate for vehicle type
     */
    private fun getPerKmRate(vehicleType: String): Double {
        return when (vehicleType.lowercase()) {
            "economy" -> 1.5
            "comfort" -> 2.0
            "premium" -> 3.0
            "luxury" -> 5.0
            "suv" -> 2.5
            "bike" -> 0.8
            else -> 1.8
        }
    }
    
    /**
     * Get per minute rate for vehicle type
     */
    private fun getPerMinuteRate(vehicleType: String): Double {
        return when (vehicleType.lowercase()) {
            "economy" -> 0.3
            "comfort" -> 0.4
            "premium" -> 0.6
            "luxury" -> 1.0
            "suv" -> 0.5
            "bike" -> 0.2
            else -> 0.35
        }
    }
    
    /**
     * Get expected fare range for vehicle type
     */
    private fun getExpectedFareRange(vehicleType: String): Pair<Double, Double> {
        return when (vehicleType.lowercase()) {
            "economy" -> Pair(5.0, 15.0)
            "comfort" -> Pair(8.0, 25.0)
            "premium" -> Pair(15.0, 40.0)
            "luxury" -> Pair(25.0, 60.0)
            "suv" -> Pair(12.0, 35.0)
            "bike" -> Pair(3.0, 10.0)
            else -> Pair(6.0, 20.0)
        }
    }
    
    /**
     * Calculate distance between two points
     */
    private fun calculateDistance(point1: LatLng, point2: LatLng): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            point1.latitude, point1.longitude,
            point2.latitude, point2.longitude,
            results
        )
        return results[0]
    }
    
    /**
     * Find alternative drivers if primary allocation fails
     */
    suspend fun findAlternativeDrivers(
        pickupLocation: LatLng,
        dropoffLocation: LatLng,
        vehicleType: String,
        availableDrivers: List<DriverLocation>,
        excludeDriverIds: List<String> = emptyList()
    ): List<DriverAllocationResult> {
        
        val filteredDrivers = availableDrivers.filter { driver ->
            driver.vehicleType == vehicleType && 
            driver.isAvailable && 
            !excludeDriverIds.contains(driver.driverId)
        }
        
        val allocations = mutableListOf<DriverAllocationResult>()
        
        for (driver in filteredDrivers.take(5)) { // Limit to top 5 alternatives
            val allocation = findBestDriver(pickupLocation, dropoffLocation, vehicleType, listOf(driver))
            allocation?.let { allocations.add(it) }
        }
        
        return allocations.sortedByDescending { it.allocationScore }
    }
    
    /**
     * Update driver location and availability
     */
    suspend fun updateDriverLocation(
        driverId: String,
        location: LocationData,
        isAvailable: Boolean
    ): Boolean {
        // This would typically update Firebase Realtime Database
        // For now, we'll simulate the update
        return true
    }
}

/**
 * Data classes for ride allocation
 */
data class DriverLocation(
    val driverId: String,
    val driverName: String,
    val vehicleType: String,
    val vehicleNumber: String,
    val currentLocation: LocationData,
    val rating: Float,
    val isAvailable: Boolean,
    val lastSeen: Long
)

data class DriverScore(
    val driverId: String,
    val driverName: String,
    val vehicleType: String,
    val vehicleNumber: String,
    val driverRating: Float,
    val distanceToPickup: Float,
    val estimatedArrivalTime: Int,
    val estimatedFare: Double,
    val totalScore: Float
)

data class DriverAllocationResult(
    val driverId: String,
    val driverName: String,
    val vehicleType: String,
    val vehicleNumber: String,
    val estimatedArrivalTime: Int,
    val estimatedFare: Double,
    val distanceToPickup: Float,
    val driverRating: Float,
    val allocationScore: Float,
    val allocationTime: Long
)
