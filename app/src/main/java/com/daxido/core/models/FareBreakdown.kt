package com.daxido.core.models

import com.google.firebase.Timestamp

/**
 * Industry-standard fare breakdown model (Ola/Uber/Rapido style)
 */
data class FareBreakdown(
    val baseFare: Double = 0.0,
    val distanceFare: Double = 0.0,
    val timeFare: Double = 0.0,
    val surgeMultiplier: Double = 1.0,
    val surgeAmount: Double = 0.0,
    val platformFee: Double = 0.0,
    val gst: Double = 0.0,
    val tollCharges: Double = 0.0,
    val parkingCharges: Double = 0.0,
    val waitingCharges: Double = 0.0,
    val nightCharges: Double = 0.0,
    val discount: Double = 0.0,
    val promoDiscount: Double = 0.0,
    val total: Double = 0.0,
    val currency: String = "INR",
    val distanceKm: Double = 0.0,
    val durationMinutes: Int = 0,
    val estimatedTime: String = ""
) {
    fun getSubtotal(): Double {
        return baseFare + distanceFare + timeFare + surgeAmount +
               platformFee + tollCharges + parkingCharges +
               waitingCharges + nightCharges
    }

    fun getTotalAfterDiscount(): Double {
        return (getSubtotal() + gst) - discount - promoDiscount
    }

    fun getFormattedTotal(): String {
        return "₹${String.format("%.2f", total)}"
    }
}

/**
 * Ride preferences matching Ola/Uber standards
 */
data class RidePreferences(
    val acPreferred: Boolean = false,
    val musicPreferred: Boolean = false,
    val petFriendly: Boolean = false,
    val childSeatRequired: Boolean = false,
    val accessibleVehicle: Boolean = false,
    val luggageSpace: Boolean = false,
    val preferredLanguage: String? = null,
    val notes: String? = null
)

/**
 * Cancellation policy
 */
data class CancellationPolicy(
    val freeCancellationWindow: Int = 5, // minutes
    val cancellationFee: Double = 0.0,
    val cancellationReason: String? = null,
    val policyText: String = "Free cancellation within 5 minutes of booking"
)

/**
 * Favorite location (Home, Work, etc.)
 */
data class FavoriteLocation(
    val id: String = "",
    val userId: String = "",
    val label: String = "", // "Home", "Work", "Gym", etc.
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val icon: String = "home", // home, work, star, etc.
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Recent search
 */
data class RecentSearch(
    val id: String = "",
    val userId: String = "",
    val searchText: String = "",
    val location: Location? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Referral information
 */
data class ReferralInfo(
    val referralCode: String = "",
    val referredBy: String? = null,
    val referralCount: Int = 0,
    val referralEarnings: Double = 0.0,
    val referralBonus: Double = 100.0, // Bonus for each referral
    val refereeBonus: Double = 50.0, // Bonus for person who uses code
    val isEligible: Boolean = true
)

/**
 * Safety contacts
 */
data class SafetyContact(
    val id: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val relationship: String = "",
    val isEmergencyContact: Boolean = false
)

/**
 * Trip sharing information
 */
data class TripSharingInfo(
    val shareLink: String = "",
    val isSharing: Boolean = false,
    val sharedWith: List<String> = emptyList(),
    val expiresAt: Long? = null
)

/**
 * Enhanced ride request with all Ola/Uber features
 */
data class EnhancedRideRequest(
    val id: String = "",
    val userId: String = "",
    val pickupLocation: Location,
    val dropLocation: Location,
    val vehicleType: VehicleType,
    val fareBreakdown: FareBreakdown,
    val preferences: RidePreferences = RidePreferences(),
    val paymentMethod: PaymentMethod,
    val promoCode: String? = null,
    val scheduledTime: Timestamp? = null, // For scheduled rides
    val isPooled: Boolean = false,
    val poolingPreference: PoolingPreference? = null,
    val notes: String? = null,
    val safetyContacts: List<SafetyContact> = emptyList(),
    val requestedAt: Timestamp = Timestamp.now(),
    val status: RideStatus = RideStatus.SEARCHING
)

/**
 * Pooling preference
 */
data class PoolingPreference(
    val maxWaitTime: Int = 5, // minutes
    val maxDetour: Int = 10, // minutes
    val preferredGender: String? = null, // "male", "female", "any"
    val allowStops: Boolean = true
)

/**
 * Driver rating and review
 */
data class DriverRating(
    val rideId: String = "",
    val driverId: String = "",
    val userId: String = "",
    val rating: Float = 0f, // 1-5 stars
    val review: String? = null,
    val tags: List<String> = emptyList(), // "Polite", "Safe Driver", "Clean Car", etc.
    val vehicleCleanliness: Int = 0, // 1-5
    val drivingSkill: Int = 0, // 1-5
    val behavior: Int = 0, // 1-5
    val timestamp: Timestamp = Timestamp.now()
)

/**
 * Rating tags (predefined options)
 */
object RatingTags {
    val POSITIVE = listOf(
        "Polite & Friendly",
        "Safe Driver",
        "Clean Car",
        "Good Music",
        "On Time",
        "Smooth Ride",
        "Helpful",
        "Professional"
    )

    val NEGATIVE = listOf(
        "Rash Driving",
        "Rude Behavior",
        "Dirty Car",
        "Late Arrival",
        "Wrong Route",
        "Unsafe Driving",
        "Bad Music",
        "Uncomfortable"
    )
}
