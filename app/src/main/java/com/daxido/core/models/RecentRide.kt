package com.daxido.core.models

data class RecentRide(
    val id: String,
    val pickupLocation: String,
    val dropoffLocation: String,
    val fare: Double,
    val date: Long,
    val status: RideStatus,
    val vehicleType: VehicleType,
    val rating: Double? = null
)
