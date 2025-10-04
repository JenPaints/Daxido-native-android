package com.daxido.core.models

import java.util.Date

data class RidePool(
    val poolId: String = "",
    val primaryRideId: String = "",
    val rides: List<PooledRide> = emptyList(),
    val vehicleType: VehicleType = VehicleType.CAR,
    val maxCapacity: Int = 4,
    val currentOccupancy: Int = 0,
    val routePolyline: String = "",
    val totalDistance: Float = 0f,
    val estimatedDuration: Int = 0,
    val status: PoolStatus = PoolStatus.ACCEPTING_RIDERS,
    val createdAt: Date = Date()
)

data class PooledRide(
    val rideId: String = "",
    val userId: String = "",
    val pickupLocation: Location = Location(),
    val dropLocation: Location = Location(),
    val pickupOrder: Int = 0,
    val dropOrder: Int = 0,
    val individualFare: Double = 0.0,
    val discountPercentage: Float = 30f, // Pool rides typically 30% cheaper
    val status: PoolRideStatus = PoolRideStatus.WAITING,
    val coRiders: List<CoRiderInfo> = emptyList()
)

data class CoRiderInfo(
    val userId: String = "",
    val name: String = "",
    val rating: Float = 0f,
    val profilePic: String? = null,
    val gender: Gender? = null,
    val verificationStatus: VerificationStatus = VerificationStatus.PENDING
)

enum class PoolStatus {
    ACCEPTING_RIDERS,
    POOL_FULL,
    EN_ROUTE,
    COMPLETED,
    CANCELLED
}

enum class PoolRideStatus {
    WAITING,
    PICKED_UP,
    DROPPED,
    CANCELLED
}

enum class Gender {
    MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY
}

data class PoolMatchingCriteria(
    val maxDetourTime: Int = 10, // minutes
    val maxDetourDistance: Float = 3f, // km
    val genderPreference: GenderPreference = GenderPreference.NO_PREFERENCE,
    val minRating: Float = 4.0f
)

enum class GenderPreference {
    SAME_GENDER_ONLY,
    NO_PREFERENCE,
    WOMEN_ONLY_POOL
}

data class PoolOptimizationResult(
    val optimalRoute: List<Location>,
    val pickupOrder: List<String>, // userId order
    val dropOrder: List<String>, // userId order
    val totalDistance: Float,
    val totalDuration: Int,
    val savingsPerRider: Map<String, Double>,
    val environmentalImpact: EnvironmentalImpact
)

data class EnvironmentalImpact(
    val co2Saved: Float, // in kg
    val fuelSaved: Float, // in liters
    val treesEquivalent: Float // trees planted equivalent
)