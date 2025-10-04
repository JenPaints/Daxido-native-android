package com.daxido.core.scheduling

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import com.google.android.gms.maps.model.LatLng
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scheduled rides service for advance booking
 */
@Singleton
class ScheduledRidesService @Inject constructor(
    private val context: Context
) {
    
    private val scheduledRides = mutableMapOf<String, ScheduledRide>()
    private val recurringRides = mutableMapOf<String, RecurringRide>()
    
    /**
     * Schedule a ride for future
     */
    suspend fun scheduleRide(
        rideId: String,
        userId: String,
        pickupLocation: LatLng,
        dropoffLocation: LatLng,
        scheduledTime: Long,
        vehicleType: String,
        estimatedFare: Double,
        notes: String? = null
    ): ScheduledRideResult = withContext(Dispatchers.IO) {
        try {
            val currentTime = System.currentTimeMillis()
            if (scheduledTime <= currentTime) {
                return@withContext ScheduledRideResult.Failure("Scheduled time must be in the future")
            }
            
            val scheduledRide = ScheduledRide(
                id = rideId,
                userId = userId,
                pickupLocation = pickupLocation,
                dropoffLocation = dropoffLocation,
                scheduledTime = scheduledTime,
                vehicleType = vehicleType,
                estimatedFare = estimatedFare,
                notes = notes,
                status = ScheduledRideStatus.SCHEDULED,
                createdAt = currentTime,
                driverId = null,
                actualFare = null
            )
            
            scheduledRides[rideId] = scheduledRide
            
            Log.d("ScheduledRidesService", "Ride scheduled: $rideId for ${scheduledTime}")
            ScheduledRideResult.Success(scheduledRide)
        } catch (e: Exception) {
            Log.e("ScheduledRidesService", "Error scheduling ride: ${e.message}", e)
            ScheduledRideResult.Failure("Failed to schedule ride: ${e.message}")
        }
    }
    
    /**
     * Create a recurring ride
     */
    suspend fun createRecurringRide(
        recurringId: String,
        userId: String,
        pickupLocation: LatLng,
        dropoffLocation: LatLng,
        time: String, // HH:MM format
        days: List<DayOfWeek>,
        vehicleType: String,
        estimatedFare: Double,
        startDate: Long,
        endDate: Long? = null,
        notes: String? = null
    ): RecurringRideResult = withContext(Dispatchers.IO) {
        try {
            val recurringRide = RecurringRide(
                id = recurringId,
                userId = userId,
                pickupLocation = pickupLocation,
                dropoffLocation = dropoffLocation,
                time = time,
                days = days,
                vehicleType = vehicleType,
                estimatedFare = estimatedFare,
                startDate = startDate,
                endDate = endDate,
                notes = notes,
                status = RecurringRideStatus.ACTIVE,
                createdAt = System.currentTimeMillis(),
                lastTriggered = null
            )
            
            recurringRides[recurringId] = recurringRide
            
            Log.d("ScheduledRidesService", "Recurring ride created: $recurringId")
            RecurringRideResult.Success(recurringRide)
        } catch (e: Exception) {
            Log.e("ScheduledRidesService", "Error creating recurring ride: ${e.message}", e)
            RecurringRideResult.Failure("Failed to create recurring ride: ${e.message}")
        }
    }
    
    /**
     * Get scheduled rides for user
     */
    suspend fun getUserScheduledRides(userId: String): Flow<List<ScheduledRide>> = flow {
        val userRides = scheduledRides.values.filter { it.userId == userId }
            .sortedBy { it.scheduledTime }
        emit(userRides)
    }
    
    /**
     * Get recurring rides for user
     */
    suspend fun getUserRecurringRides(userId: String): Flow<List<RecurringRide>> = flow {
        val userRecurringRides = recurringRides.values.filter { it.userId == userId }
        emit(userRecurringRides)
    }
    
    /**
     * Cancel scheduled ride
     */
    suspend fun cancelScheduledRide(rideId: String): ScheduledRideResult = withContext(Dispatchers.IO) {
        try {
            val scheduledRide = scheduledRides[rideId]
                ?: return@withContext ScheduledRideResult.Failure("Scheduled ride not found")
            
            val updatedRide = scheduledRide.copy(
                status = ScheduledRideStatus.CANCELLED,
                cancelledAt = System.currentTimeMillis()
            )
            
            scheduledRides[rideId] = updatedRide
            
            Log.d("ScheduledRidesService", "Scheduled ride cancelled: $rideId")
            ScheduledRideResult.Success(updatedRide)
        } catch (e: Exception) {
            Log.e("ScheduledRidesService", "Error cancelling scheduled ride: ${e.message}", e)
            ScheduledRideResult.Failure("Failed to cancel scheduled ride: ${e.message}")
        }
    }
    
    /**
     * Cancel recurring ride
     */
    suspend fun cancelRecurringRide(recurringId: String): RecurringRideResult = withContext(Dispatchers.IO) {
        try {
            val recurringRide = recurringRides[recurringId]
                ?: return@withContext RecurringRideResult.Failure("Recurring ride not found")
            
            val updatedRide = recurringRide.copy(
                status = RecurringRideStatus.CANCELLED,
                cancelledAt = System.currentTimeMillis()
            )
            
            recurringRides[recurringId] = updatedRide
            
            Log.d("ScheduledRidesService", "Recurring ride cancelled: $recurringId")
            RecurringRideResult.Success(updatedRide)
        } catch (e: Exception) {
            Log.e("ScheduledRidesService", "Error cancelling recurring ride: ${e.message}", e)
            RecurringRideResult.Failure("Failed to cancel recurring ride: ${e.message}")
        }
    }
    
    /**
     * Get rides that need to be triggered (within next 30 minutes)
     */
    suspend fun getRidesToTrigger(): Flow<List<ScheduledRide>> = flow {
        val currentTime = System.currentTimeMillis()
        val triggerTime = currentTime + (30 * 60 * 1000) // 30 minutes from now
        
        val ridesToTrigger = scheduledRides.values.filter { ride ->
            ride.status == ScheduledRideStatus.SCHEDULED &&
            ride.scheduledTime <= triggerTime &&
            ride.scheduledTime > currentTime
        }.sortedBy { it.scheduledTime }
        
        emit(ridesToTrigger)
    }
    
    /**
     * Trigger scheduled ride (convert to active ride)
     */
    suspend fun triggerScheduledRide(rideId: String, driverId: String): ScheduledRideResult = withContext(Dispatchers.IO) {
        try {
            val scheduledRide = scheduledRides[rideId]
                ?: return@withContext ScheduledRideResult.Failure("Scheduled ride not found")
            
            val updatedRide = scheduledRide.copy(
                status = ScheduledRideStatus.TRIGGERED,
                driverId = driverId,
                triggeredAt = System.currentTimeMillis()
            )
            
            scheduledRides[rideId] = updatedRide
            
            Log.d("ScheduledRidesService", "Scheduled ride triggered: $rideId")
            ScheduledRideResult.Success(updatedRide)
        } catch (e: Exception) {
            Log.e("ScheduledRidesService", "Error triggering scheduled ride: ${e.message}", e)
            ScheduledRideResult.Failure("Failed to trigger scheduled ride: ${e.message}")
        }
    }
    
    /**
     * Check and trigger recurring rides
     */
    suspend fun checkRecurringRides(): Flow<List<ScheduledRide>> = flow {
        val currentTime = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        val currentDay = DayOfWeek.values()[calendar.get(Calendar.DAY_OF_WEEK) - 1]
        val currentTimeStr = String.format("%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
        
        val triggeredRides = mutableListOf<ScheduledRide>()
        
        recurringRides.values.forEach { recurringRide ->
            if (recurringRide.status == RecurringRideStatus.ACTIVE &&
                recurringRide.days.contains(currentDay) &&
                recurringRide.time == currentTimeStr &&
                (recurringRide.lastTriggered == null || 
                 currentTime - recurringRide.lastTriggered!! > 24 * 60 * 60 * 1000)) { // Not triggered today
                
                val scheduledTime = getNextScheduledTime(recurringRide.time, currentTime)
                val rideId = "${recurringRide.id}_${scheduledTime}"
                
                val scheduledRide = ScheduledRide(
                    id = rideId,
                    userId = recurringRide.userId,
                    pickupLocation = recurringRide.pickupLocation,
                    dropoffLocation = recurringRide.dropoffLocation,
                    scheduledTime = scheduledTime,
                    vehicleType = recurringRide.vehicleType,
                    estimatedFare = recurringRide.estimatedFare,
                    notes = recurringRide.notes,
                    status = ScheduledRideStatus.SCHEDULED,
                    createdAt = currentTime,
                    driverId = null,
                    actualFare = null
                )
                
                scheduledRides[rideId] = scheduledRide
                triggeredRides.add(scheduledRide)
                
                // Update last triggered time
                val updatedRecurringRide = recurringRide.copy(lastTriggered = currentTime)
                recurringRides[recurringRide.id] = updatedRecurringRide
            }
        }
        
        emit(triggeredRides)
    }
    
    private fun getNextScheduledTime(timeStr: String, currentTime: Long): Long {
        val calendar = Calendar.getInstance()
        val timeParts = timeStr.split(":")
        calendar.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
        calendar.set(Calendar.MINUTE, timeParts[1].toInt())
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        // If the time has passed today, schedule for tomorrow
        if (calendar.timeInMillis <= currentTime) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        return calendar.timeInMillis
    }
}

data class ScheduledRide(
    val id: String,
    val userId: String,
    val pickupLocation: LatLng,
    val dropoffLocation: LatLng,
    val scheduledTime: Long,
    val vehicleType: String,
    val estimatedFare: Double,
    val notes: String?,
    val status: ScheduledRideStatus,
    val createdAt: Long,
    val driverId: String?,
    val actualFare: Double?,
    val triggeredAt: Long? = null,
    val cancelledAt: Long? = null
)

data class RecurringRide(
    val id: String,
    val userId: String,
    val pickupLocation: LatLng,
    val dropoffLocation: LatLng,
    val time: String, // HH:MM format
    val days: List<DayOfWeek>,
    val vehicleType: String,
    val estimatedFare: Double,
    val startDate: Long,
    val endDate: Long?,
    val notes: String?,
    val status: RecurringRideStatus,
    val createdAt: Long,
    val lastTriggered: Long?,
    val cancelledAt: Long? = null
)

enum class ScheduledRideStatus {
    SCHEDULED,
    TRIGGERED,
    CANCELLED,
    COMPLETED
}

enum class RecurringRideStatus {
    ACTIVE,
    CANCELLED
}

enum class DayOfWeek {
    SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY
}

sealed class ScheduledRideResult {
    data class Success(val scheduledRide: ScheduledRide) : ScheduledRideResult()
    data class Failure(val message: String) : ScheduledRideResult()
}

sealed class RecurringRideResult {
    data class Success(val recurringRide: RecurringRide) : RecurringRideResult()
    data class Failure(val message: String) : RecurringRideResult()
}
