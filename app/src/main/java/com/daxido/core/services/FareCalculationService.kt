package com.daxido.core.services

import com.daxido.core.models.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Industry-standard fare calculation (Ola/Uber/Rapido style)
 */
@Singleton
class FareCalculationService @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    // Base fare configuration (loaded from Firestore in production)
    private val baseFareConfig = mapOf(
        VehicleType.BIKE to 20.0,
        VehicleType.AUTO to 30.0,
        VehicleType.CAR to 50.0,
        VehicleType.PREMIUM to 100.0
    )

    private val perKmRate = mapOf(
        VehicleType.BIKE to 8.0,
        VehicleType.AUTO to 12.0,
        VehicleType.CAR to 15.0,
        VehicleType.PREMIUM to 25.0
    )

    private val perMinuteRate = mapOf(
        VehicleType.BIKE to 1.0,
        VehicleType.AUTO to 1.5,
        VehicleType.CAR to 2.0,
        VehicleType.PREMIUM to 3.0
    )

    /**
     * Calculate complete fare breakdown like Ola/Uber
     */
    suspend fun calculateFare(
        distanceKm: Double,
        durationMinutes: Int,
        vehicleType: VehicleType,
        surgeMultiplier: Double = 1.0,
        promoDiscount: Double = 0.0,
        preferences: RidePreferences = RidePreferences(),
        tollCharges: Double = 0.0
    ): FareBreakdown {
        // Base fare
        val baseFare = baseFareConfig[vehicleType] ?: 50.0

        // Distance fare
        val distanceFare = distanceKm * (perKmRate[vehicleType] ?: 15.0)

        // Time fare
        val timeFare = durationMinutes * (perMinuteRate[vehicleType] ?: 2.0)

        // Surge amount
        val surgeAmount = if (surgeMultiplier > 1.0) {
            (baseFare + distanceFare + timeFare) * (surgeMultiplier - 1.0)
        } else 0.0

        // Night charges (10 PM - 6 AM)
        val nightCharges = if (isNightTime()) {
            (baseFare + distanceFare) * 0.1 // 10% night charge
        } else 0.0

        // AC charges for Auto/Car
        val acCharges = if (preferences.acPreferred && vehicleType != VehicleType.BIKE) {
            20.0
        } else 0.0

        // Platform fee (like Uber's booking fee)
        val platformFee = when (vehicleType) {
            VehicleType.BIKE -> 5.0
            VehicleType.AUTO -> 10.0
            VehicleType.CAR -> 15.0
            VehicleType.PREMIUM -> 20.0
        }

        // Subtotal before tax
        val subtotal = baseFare + distanceFare + timeFare + surgeAmount +
                       nightCharges + acCharges + platformFee + tollCharges

        // GST (5% on ride fare in India)
        val gst = subtotal * 0.05

        // Total
        val total = (subtotal + gst - promoDiscount).coerceAtLeast(baseFare)

        return FareBreakdown(
            baseFare = baseFare,
            distanceFare = distanceFare,
            timeFare = timeFare,
            surgeMultiplier = surgeMultiplier,
            surgeAmount = surgeAmount,
            platformFee = platformFee,
            gst = gst,
            tollCharges = tollCharges,
            nightCharges = nightCharges,
            promoDiscount = promoDiscount,
            total = total,
            distanceKm = distanceKm,
            durationMinutes = durationMinutes,
            estimatedTime = formatDuration(durationMinutes)
        )
    }

    /**
     * Get surge pricing for current location and time
     */
    suspend fun getSurgeMultiplier(location: Location): Double {
        try {
            // In production, this would check:
            // 1. Time of day
            // 2. Day of week
            // 3. Weather conditions
            // 4. Demand vs supply ratio
            // 5. Special events in area

            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

            // Peak hours surge
            val surgePeak = when (hour) {
                in 8..10 -> 1.5  // Morning peak
                in 17..20 -> 1.8 // Evening peak
                in 22..23 -> 1.3 // Late night
                else -> 1.0
            }

            return surgePeak
        } catch (e: Exception) {
            return 1.0
        }
    }

    /**
     * Calculate cancellation fee
     */
    fun calculateCancellationFee(
        minutesSinceBooking: Int,
        fareBreakdown: FareBreakdown,
        rideStatus: RideStatus
    ): Double {
        return when {
            minutesSinceBooking < 5 -> 0.0 // Free cancellation within 5 minutes
            rideStatus == RideStatus.DRIVER_ASSIGNED && minutesSinceBooking < 10 -> {
                fareBreakdown.baseFare * 0.5 // 50% of base fare
            }
            rideStatus == RideStatus.DRIVER_ARRIVED -> {
                fareBreakdown.baseFare // Full base fare
            }
            else -> fareBreakdown.baseFare * 0.3 // 30% of base fare
        }
    }

    /**
     * Get cancellation policy text
     */
    fun getCancellationPolicy(minutesSinceBooking: Int): CancellationPolicy {
        return when {
            minutesSinceBooking < 5 -> CancellationPolicy(
                freeCancellationWindow = 5,
                cancellationFee = 0.0,
                policyText = "Free cancellation within 5 minutes of booking"
            )
            minutesSinceBooking < 10 -> CancellationPolicy(
                freeCancellationWindow = 5,
                cancellationFee = 25.0,
                policyText = "₹25 cancellation fee applicable"
            )
            else -> CancellationPolicy(
                freeCancellationWindow = 5,
                cancellationFee = 50.0,
                policyText = "₹50 cancellation fee applicable after driver arrival"
            )
        }
    }

    private fun isNightTime(): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return hour >= 22 || hour < 6
    }

    private fun formatDuration(minutes: Int): String {
        return when {
            minutes < 60 -> "$minutes min"
            else -> {
                val hours = minutes / 60
                val mins = minutes % 60
                if (mins > 0) "$hours hr $mins min" else "$hours hr"
            }
        }
    }
}
