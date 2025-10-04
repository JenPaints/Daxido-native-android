package com.daxido.core.models

import java.util.Date

data class Ride(
    val id: String = "",
    val userId: String = "",
    val driverId: String? = null,
    val pickupLocation: Location = Location(),
    val dropLocation: Location = Location(),
    val vehicleType: VehicleType = VehicleType.CAR,
    val status: RideStatus = RideStatus.SEARCHING,
    val fare: Fare = Fare(),
    val distance: Float = 0f,
    val duration: Int = 0,
    val createdAt: Date = Date(),
    val acceptedAt: Date? = null,
    val startedAt: Date? = null,
    val completedAt: Date? = null,
    val cancelledAt: Date? = null,
    val cancellationReason: String? = null,
    val cancelledBy: String? = null,
    val otp: String? = null,
    val paymentMethod: PaymentMethod = PaymentMethod(type = PaymentType.CASH),
    val paymentStatus: PaymentStatus = PaymentStatus.PENDING,
    val driverLocation: Location? = null,
    val routePolyline: String? = null,
    val rating: Float? = null,
    val review: String? = null,
    val tip: Double? = null,
    val surgeMultiplier: Float = 1.0f,
    val promoCodeApplied: String? = null,
    val discount: Double = 0.0
)

data class Fare(
    val baseFare: Double = 0.0,
    val distanceFare: Double = 0.0,
    val timeCharge: Double = 0.0,
    val surgeCharge: Double = 0.0,
    val taxes: Double = 0.0,
    val discount: Double = 0.0,
    val total: Double = 0.0
)

enum class RideStatus {
    SEARCHING,
    DRIVER_ASSIGNED,
    DRIVER_ARRIVED,
    TRIP_STARTED,
    TRIP_ENDED,
    COMPLETED,
    CANCELLED
}

data class PaymentMethod(
    val id: String = "",
    val type: PaymentType = PaymentType.CASH,
    val displayName: String = "",
    val details: String? = null,
    val isDefault: Boolean = false,
    val walletBalance: Double? = null
)

enum class PaymentType {
    CASH, CARD, UPI, WALLET, NET_BANKING
}

enum class PaymentStatus {
    PENDING, PROCESSING, COMPLETED, FAILED, REFUNDED
}

data class RideRequest(
    val userId: String = "",
    val pickupLocation: Location = Location(),
    val dropLocation: Location = Location(),
    val vehicleType: VehicleType = VehicleType.CAR,
    val paymentMethod: PaymentMethod = PaymentMethod(),
    val estimatedFare: Fare = Fare(),
    val requestedAt: Date = Date()
)

data class TrackingUpdate(
    val rideId: String,
    val driverId: String,
    val driverLocation: Location,
    val estimatedTimeRemaining: Int,
    val routePolyline: String? = null
)

data class NearbyDriver(
    val driverId: String,
    val location: Location,
    val vehicleType: VehicleType,
    val rating: Float,
    val distance: Float,
    val estimatedArrival: Int,
    val isAvailable: Boolean
)