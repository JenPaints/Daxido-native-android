package com.daxido.core.services

import com.daxido.core.maps.DirectionsService
import com.daxido.core.models.*
import com.daxido.core.models.RideRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

@Singleton
class RidePoolingService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val realtimeDb: FirebaseDatabase,
    private val directionsService: DirectionsService
) {
    private val poolingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun findPoolMatch(
        rideRequest: RideRequest,
        criteria: PoolMatchingCriteria
    ): Flow<PoolMatchResult> = flow {
        // Search for existing pools that match criteria
        val existingPools = searchExistingPools(rideRequest, criteria)

        if (existingPools.isNotEmpty()) {
            val bestMatch = rankPoolMatches(existingPools, rideRequest, criteria)
            emit(PoolMatchResult.MatchFound(bestMatch))
        } else {
            // Create new pool
            val newPool = createNewPool(rideRequest)
            emit(PoolMatchResult.NewPoolCreated(newPool))
        }
    }

    private suspend fun searchExistingPools(
        request: RideRequest,
        criteria: PoolMatchingCriteria
    ): List<RidePool> {
        return try {
            val querySnapshot = firestore.collection("active_pools")
                .whereEqualTo("status", PoolStatus.ACCEPTING_RIDERS.name)
                .whereEqualTo("vehicleType", request.vehicleType.name)
                .whereLessThan("currentOccupancy", 4)
                .get()
                .result
            
            querySnapshot.documents.mapNotNull { doc ->
                val pool = doc.toObject(RidePool::class.java)
                pool?.let {
                    if (isPoolCompatible(it, request, criteria)) pool else null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun isPoolCompatible(
        pool: RidePool,
        request: RideRequest,
        criteria: PoolMatchingCriteria
    ): Boolean {
        // Calculate detour impact
        val detourResult = calculateDetour(pool, request)

        return detourResult.additionalTime <= criteria.maxDetourTime &&
               detourResult.additionalDistance <= criteria.maxDetourDistance &&
               checkGenderCompatibility(pool, request.userId, criteria.genderPreference)
    }

    private fun calculateDetour(
        pool: RidePool,
        request: RideRequest
    ): DetourCalculation {
        // Complex route optimization algorithm
        val currentRoute = pool.routePolyline
        val newPickup = Location(
            latitude = request.pickupLocation.latitude,
            longitude = request.pickupLocation.longitude,
            address = null
        )
        val newDrop = Location(
            latitude = request.dropLocation.latitude,
            longitude = request.dropLocation.longitude,
            address = null
        )

        // Calculate optimal insertion points
        val optimizer = RouteOptimizer()
        return optimizer.calculateDetour(currentRoute, newPickup, newDrop, pool.rides)
    }

    private fun checkGenderCompatibility(
        pool: RidePool,
        userId: String,
        preference: GenderPreference
    ): Boolean {
        return when (preference) {
            GenderPreference.NO_PREFERENCE -> true
            GenderPreference.WOMEN_ONLY_POOL -> {
                pool.rides.all { ride ->
                    ride.coRiders.all { it.gender == Gender.FEMALE }
                }
            }
            GenderPreference.SAME_GENDER_ONLY -> {
                // Check if all riders in pool have same gender
                val userGender = getUserGender(userId)
                pool.rides.all { ride ->
                    ride.coRiders.all { it.gender == userGender }
                }
            }
        }
    }

    private fun getUserGender(userId: String): Gender? {
        // Fetch from user profile
        return Gender.OTHER // Placeholder
    }

    private suspend fun createNewPool(request: RideRequest): RidePool {
        val poolId = generatePoolId()
        val pool = RidePool(
            poolId = poolId,
            primaryRideId = request.userId + "_" + System.currentTimeMillis(),
            rides = listOf(
                PooledRide(
                    rideId = generateRideId(),
                    userId = request.userId,
                    pickupLocation = Location(
                        latitude = request.pickupLocation.latitude,
                        longitude = request.pickupLocation.longitude,
                        address = null
                    ),
                    dropLocation = Location(
                        latitude = request.dropLocation.latitude,
                        longitude = request.dropLocation.longitude,
                        address = null
                    ),
                    pickupOrder = 1,
                    dropOrder = 1,
                    individualFare = request.estimatedFare.total * 0.7, // 30% discount
                    status = PoolRideStatus.WAITING
                )
            ),
            vehicleType = request.vehicleType,
            maxCapacity = 4,
            currentOccupancy = 1,
            status = PoolStatus.ACCEPTING_RIDERS
        )

        // Save to Firestore
        try {
            firestore.collection("active_pools")
                .document(poolId)
                .set(pool)
        } catch (e: Exception) {
            // Handle error
        }

        return pool
    }

    private fun rankPoolMatches(
        pools: List<RidePool>,
        request: RideRequest,
        criteria: PoolMatchingCriteria
    ): RidePool {
        return pools.minByOrNull { pool ->
            val detour = calculateDetour(pool, request)
            detour.additionalTime * 0.6 + detour.additionalDistance * 0.4
        } ?: pools.first()
    }

    fun optimizePoolRoute(pool: RidePool): PoolOptimizationResult {
        val optimizer = RouteOptimizer()
        val allPickups = pool.rides.map { it.pickupLocation }
        val allDrops = pool.rides.map { it.dropLocation }

        // Use TSP (Traveling Salesman Problem) variant for optimization
        val optimizedRoute = optimizer.findOptimalRoute(allPickups, allDrops)

        // Calculate environmental impact
        val standardDistance = pool.rides.sumOf { ride ->
            calculateDistance(ride.pickupLocation, ride.dropLocation)
        }
        val pooledDistance = optimizedRoute.totalDistance
        val savedDistance = standardDistance - pooledDistance

        val environmentalImpact = EnvironmentalImpact(
            co2Saved = (savedDistance * 0.12).toFloat(), // kg CO2 per km
            fuelSaved = (savedDistance * 0.08).toFloat(), // liters per km
            treesEquivalent = (savedDistance * 0.003).toFloat() // trees equivalent
        )

        return PoolOptimizationResult(
            optimalRoute = optimizedRoute.waypoints,
            pickupOrder = optimizedRoute.pickupOrder,
            dropOrder = optimizedRoute.dropOrder,
            totalDistance = optimizedRoute.totalDistance.toFloat(),
            totalDuration = optimizedRoute.totalDuration,
            savingsPerRider = calculateSavings(pool),
            environmentalImpact = environmentalImpact
        )
    }

    private fun calculateSavings(pool: RidePool): Map<String, Double> {
        return pool.rides.associate { ride ->
            val standardFare = ride.individualFare / 0.7 // Reverse the 30% discount
            val savings = standardFare - ride.individualFare
            ride.userId to savings
        }
    }

    private fun calculateDistance(from: Location, to: Location): Double {
        val earthRadius = 6371.0 // km
        val dLat = Math.toRadians(to.latitude - from.latitude)
        val dLon = Math.toRadians(to.longitude - from.longitude)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(from.latitude)) * cos(Math.toRadians(to.latitude)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    private fun generatePoolId(): String = "POOL_${System.currentTimeMillis()}"
    private fun generateRideId(): String = "RIDE_${System.currentTimeMillis()}"
}

sealed class PoolMatchResult {
    data class MatchFound(val pool: RidePool) : PoolMatchResult()
    data class NewPoolCreated(val pool: RidePool) : PoolMatchResult()
    object NoMatchAvailable : PoolMatchResult()
}

data class DetourCalculation(
    val additionalTime: Int,
    val additionalDistance: Float
)

class RouteOptimizer {
    data class OptimizedRoute(
        val waypoints: List<Location>,
        val pickupOrder: List<String>,
        val dropOrder: List<String>,
        val totalDistance: Double,
        val totalDuration: Int
    )

    fun calculateDetour(
        currentRoute: String,
        newPickup: Location,
        newDrop: Location,
        existingRides: List<PooledRide>
    ): DetourCalculation {
        // Simplified calculation - would use actual routing API
        return DetourCalculation(
            additionalTime = 5,
            additionalDistance = 2.5f
        )
    }

    fun findOptimalRoute(
        pickups: List<Location>,
        drops: List<Location>
    ): OptimizedRoute {
        // Implement TSP optimization
        return OptimizedRoute(
            waypoints = pickups + drops,
            pickupOrder = pickups.indices.map { "user_$it" },
            dropOrder = drops.indices.map { "user_$it" },
            totalDistance = 15.0,
            totalDuration = 45
        )
    }
}