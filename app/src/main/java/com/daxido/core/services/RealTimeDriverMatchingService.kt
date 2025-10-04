package com.daxido.core.services

import android.content.Context
import android.util.Log
import com.daxido.core.algorithms.DriverAllocationResult
import com.daxido.core.algorithms.DriverLocation
import com.daxido.core.algorithms.RealRideAllocationEngine
import com.daxido.core.location.LocationData
import com.daxido.core.maps.DirectionsApiService
import com.daxido.core.maps.ETAResult
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.*
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real-time driver matching and notification service
 */
@Singleton
class RealTimeDriverMatchingService @Inject constructor(
    private val context: Context,
    private val rideAllocationEngine: RealRideAllocationEngine,
    private val directionsApiService: DirectionsApiService
) {
    
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val driversRef: DatabaseReference = database.getReference("drivers")
    private val rideRequestsRef: DatabaseReference = database.getReference("ride_requests")
    private val activeRidesRef: DatabaseReference = database.getReference("active_rides")
    
    private val _availableDrivers = MutableStateFlow<List<DriverLocation>>(emptyList())
    val availableDrivers: Flow<List<DriverLocation>> = _availableDrivers.asStateFlow()
    
    private val _matchingStatus = MutableStateFlow<MatchingStatus>(MatchingStatus.IDLE)
    val matchingStatus: Flow<MatchingStatus> = _matchingStatus.asStateFlow()
    
    /**
     * Start monitoring available drivers
     */
    fun startDriverMonitoring() {
        driversRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val drivers = mutableListOf<DriverLocation>()
                
                for (driverSnapshot in snapshot.children) {
                    try {
                        val driver = parseDriverLocation(driverSnapshot)
                        if (driver.isAvailable) {
                            drivers.add(driver)
                        }
                    } catch (e: Exception) {
                        Log.e("DriverMatching", "Error parsing driver: ${driverSnapshot.key}", e)
                    }
                }
                
                _availableDrivers.value = drivers
                Log.d("DriverMatching", "Updated available drivers: ${drivers.size}")
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("DriverMatching", "Error monitoring drivers", error.toException())
            }
        })
    }
    
    /**
     * Request a ride and find the best driver
     */
    suspend fun requestRide(
        userId: String,
        pickupLocation: LatLng,
        dropoffLocation: LatLng,
        vehicleType: String,
        fareEstimate: Double
    ): RideRequestResult {
        
        _matchingStatus.value = MatchingStatus.SEARCHING
        
        try {
            // Create ride request
            val rideRequestId = "ride_${System.currentTimeMillis()}_${userId}"
            val rideRequest = RideRequest(
                rideId = rideRequestId,
                userId = userId,
                pickupLocation = LocationData(
                    latitude = pickupLocation.latitude,
                    longitude = pickupLocation.longitude,
                    accuracy = 10f,
                    speed = 0f,
                    bearing = 0f,
                    timestamp = System.currentTimeMillis()
                ),
                dropoffLocation = LocationData(
                    latitude = dropoffLocation.latitude,
                    longitude = dropoffLocation.longitude,
                    accuracy = 10f,
                    speed = 0f,
                    bearing = 0f,
                    timestamp = System.currentTimeMillis()
                ),
                vehicleType = vehicleType,
                fareEstimate = fareEstimate,
                status = "searching",
                requestedAt = System.currentTimeMillis()
            )
            
            // Save ride request to database
            rideRequestsRef.child(rideRequestId).setValue(rideRequest).await()
            
            // Find best driver
            val availableDrivers = _availableDrivers.value
            val allocationResult = rideAllocationEngine.findBestDriver(
                pickupLocation = pickupLocation,
                dropoffLocation = dropoffLocation,
                vehicleType = vehicleType,
                availableDrivers = availableDrivers
            )
            
            if (allocationResult != null) {
                // Driver found - send notification
                val notificationSent = sendDriverNotification(allocationResult, rideRequest)
                
                if (notificationSent) {
                    _matchingStatus.value = MatchingStatus.DRIVER_NOTIFIED
                    
                    // Update ride request with driver info
                    val updatedRequest = rideRequest.copy(
                        driverId = allocationResult.driverId,
                        status = "driver_notified",
                        driverAllocatedAt = System.currentTimeMillis()
                    )
                    
                    rideRequestsRef.child(rideRequestId).setValue(updatedRequest).await()
                    
                    return RideRequestResult.Success(
                        rideId = rideRequestId,
                        driverAllocation = allocationResult,
                        message = "Driver found and notified"
                    )
                } else {
                    _matchingStatus.value = MatchingStatus.NOTIFICATION_FAILED
                    return RideRequestResult.Failure("Failed to notify driver")
                }
            } else {
                _matchingStatus.value = MatchingStatus.NO_DRIVERS_AVAILABLE
                return RideRequestResult.Failure("No drivers available")
            }
            
        } catch (e: Exception) {
            Log.e("DriverMatching", "Error requesting ride", e)
            _matchingStatus.value = MatchingStatus.ERROR
            return RideRequestResult.Failure("Error requesting ride: ${e.message}")
        }
    }
    
    /**
     * Send notification to driver
     */
    private suspend fun sendDriverNotification(
        driverAllocation: DriverAllocationResult,
        rideRequest: RideRequest
    ): Boolean {
        return try {
            // Get driver's FCM token
            val driverToken = getDriverFCMToken(driverAllocation.driverId)
            
            if (driverToken != null) {
                // Send FCM notification
                val notificationData = mapOf(
                    "type" to "ride_request",
                    "rideId" to rideRequest.rideId,
                    "userId" to rideRequest.userId,
                    "pickupLat" to rideRequest.pickupLocation.latitude.toString(),
                    "pickupLng" to rideRequest.pickupLocation.longitude.toString(),
                    "dropoffLat" to rideRequest.dropoffLocation.latitude.toString(),
                    "dropoffLng" to rideRequest.dropoffLocation.longitude.toString(),
                    "vehicleType" to rideRequest.vehicleType,
                    "fareEstimate" to rideRequest.fareEstimate.toString(),
                    "estimatedArrivalTime" to driverAllocation.estimatedArrivalTime.toString(),
                    "distanceToPickup" to driverAllocation.distanceToPickup.toString()
                )
                
                sendFCMNotification(driverToken, "New Ride Request", "You have a new ride request", notificationData)
                true
            } else {
                Log.e("DriverMatching", "Driver FCM token not found: ${driverAllocation.driverId}")
                false
            }
            
        } catch (e: Exception) {
            Log.e("DriverMatching", "Error sending driver notification", e)
            false
        }
    }
    
    /**
     * Handle driver response to ride request
     */
    suspend fun handleDriverResponse(
        rideId: String,
        driverId: String,
        response: DriverResponse
    ): Boolean {
        return try {
            val rideRequestRef = rideRequestsRef.child(rideId)
            val rideRequestSnapshot = rideRequestRef.get().await()
            
            if (rideRequestSnapshot.exists()) {
                val rideRequest = parseRideRequest(rideRequestSnapshot)
                
                when (response) {
                    DriverResponse.ACCEPT -> {
                        // Driver accepted - create active ride
                        val activeRide = ActiveRide(
                            rideId = rideId,
                            userId = rideRequest.userId,
                            driverId = driverId,
                            pickupLocation = rideRequest.pickupLocation,
                            dropoffLocation = rideRequest.dropoffLocation,
                            vehicleType = rideRequest.vehicleType,
                            fareEstimate = rideRequest.fareEstimate,
                            status = "driver_accepted",
                            acceptedAt = System.currentTimeMillis(),
                            estimatedArrivalTime = 0, // Will be updated with real ETA
                            estimatedFare = rideRequest.fareEstimate
                        )
                        
                        // Save active ride
                        activeRidesRef.child(rideId).setValue(activeRide).await()
                        
                        // Update ride request status
                        rideRequestRef.child("status").setValue("driver_accepted").await()
                        rideRequestRef.child("driverAcceptedAt").setValue(System.currentTimeMillis()).await()
                        
                        // Notify user
                        notifyUser(rideId, "Driver Accepted", "Your driver has accepted the ride")
                        
                        _matchingStatus.value = MatchingStatus.DRIVER_ACCEPTED
                        true
                    }
                    
                    DriverResponse.REJECT -> {
                        // Driver rejected - find alternative driver
                        rideRequestRef.child("status").setValue("driver_rejected").await()
                        rideRequestRef.child("driverRejectedAt").setValue(System.currentTimeMillis()).await()
                        
                        // Find alternative driver
                        findAlternativeDriver(rideRequest)
                        true
                    }
                }
            } else {
                Log.e("DriverMatching", "Ride request not found: $rideId")
                false
            }
            
        } catch (e: Exception) {
            Log.e("DriverMatching", "Error handling driver response", e)
            false
        }
    }
    
    /**
     * Find alternative driver if current driver rejects
     */
    private suspend fun findAlternativeDriver(rideRequest: RideRequest) {
        val pickupLocation = LatLng(rideRequest.pickupLocation.latitude, rideRequest.pickupLocation.longitude)
        val dropoffLocation = LatLng(rideRequest.dropoffLocation.latitude, rideRequest.dropoffLocation.longitude)
        
        val alternativeDrivers = rideAllocationEngine.findAlternativeDrivers(
            pickupLocation = pickupLocation,
            dropoffLocation = dropoffLocation,
            vehicleType = rideRequest.vehicleType,
            availableDrivers = _availableDrivers.value,
            excludeDriverIds = listOf(rideRequest.driverId ?: "")
        )
        
        if (alternativeDrivers.isNotEmpty()) {
            val nextDriver = alternativeDrivers.first()
            sendDriverNotification(nextDriver, rideRequest)
        } else {
            _matchingStatus.value = MatchingStatus.NO_DRIVERS_AVAILABLE
        }
    }
    
    /**
     * Update driver location in real-time
     */
    suspend fun updateDriverLocation(
        driverId: String,
        location: LocationData,
        isAvailable: Boolean
    ): Boolean {
        return try {
            val driverRef = driversRef.child(driverId)
            driverRef.child("currentLocation").setValue(location).await()
            driverRef.child("isAvailable").setValue(isAvailable).await()
            driverRef.child("lastSeen").setValue(System.currentTimeMillis()).await()
            true
        } catch (e: Exception) {
            Log.e("DriverMatching", "Error updating driver location", e)
            false
        }
    }
    
    /**
     * Get driver FCM token
     */
    private suspend fun getDriverFCMToken(driverId: String): String? {
        return try {
            val driverSnapshot = driversRef.child(driverId).child("fcmToken").get().await()
            driverSnapshot.getValue(String::class.java)
        } catch (e: Exception) {
            Log.e("DriverMatching", "Error getting driver FCM token", e)
            null
        }
    }
    
    /**
     * Send FCM notification
     */
    private suspend fun sendFCMNotification(
        token: String,
        title: String,
        body: String,
        data: Map<String, String>
    ): Boolean {
        // This would typically use Firebase Cloud Messaging
        // For now, we'll simulate the notification
        Log.d("DriverMatching", "Sending FCM notification to $token: $title - $body")
        return true
    }
    
    /**
     * Notify user about ride status
     */
    private suspend fun notifyUser(rideId: String, title: String, body: String) {
        // This would send notification to user
        Log.d("DriverMatching", "Notifying user for ride $rideId: $title - $body")
    }
    
    /**
     * Parse driver location from Firebase snapshot
     */
    private fun parseDriverLocation(snapshot: DataSnapshot): DriverLocation {
        val driverId = snapshot.key ?: ""
        val driverName = snapshot.child("name").getValue(String::class.java) ?: ""
        val vehicleType = snapshot.child("vehicleType").getValue(String::class.java) ?: ""
        val vehicleNumber = snapshot.child("vehicleNumber").getValue(String::class.java) ?: ""
        val rating = snapshot.child("rating").getValue(Float::class.java) ?: 0f
        val isAvailable = snapshot.child("isAvailable").getValue(Boolean::class.java) ?: false
        val lastSeen = snapshot.child("lastSeen").getValue(Long::class.java) ?: 0L
        
        val locationSnapshot = snapshot.child("currentLocation")
        val latitude = locationSnapshot.child("latitude").getValue(Double::class.java) ?: 0.0
        val longitude = locationSnapshot.child("longitude").getValue(Double::class.java) ?: 0.0
        val accuracy = locationSnapshot.child("accuracy").getValue(Float::class.java) ?: 0f
        val speed = locationSnapshot.child("speed").getValue(Float::class.java) ?: 0f
        val bearing = locationSnapshot.child("bearing").getValue(Float::class.java) ?: 0f
        val timestamp = locationSnapshot.child("timestamp").getValue(Long::class.java) ?: 0L
        
        val currentLocation = LocationData(
            latitude = latitude,
            longitude = longitude,
            accuracy = accuracy,
            speed = speed,
            bearing = bearing,
            timestamp = timestamp
        )
        
        return DriverLocation(
            driverId = driverId,
            driverName = driverName,
            vehicleType = vehicleType,
            vehicleNumber = vehicleNumber,
            currentLocation = currentLocation,
            rating = rating,
            isAvailable = isAvailable,
            lastSeen = lastSeen
        )
    }
    
    /**
     * Parse ride request from Firebase snapshot
     */
    private fun parseRideRequest(snapshot: DataSnapshot): RideRequest {
        // Implementation would parse the ride request from Firebase
        // For now, return a basic structure
        return RideRequest(
            rideId = snapshot.key ?: "",
            userId = "",
            pickupLocation = LocationData(0.0, 0.0, 0f, 0f, 0f, 0L),
            dropoffLocation = LocationData(0.0, 0.0, 0f, 0f, 0f, 0L),
            vehicleType = "",
            fareEstimate = 0.0,
            status = "",
            requestedAt = 0L
        )
    }
}

/**
 * Data classes and enums
 */
data class RideRequest(
    val rideId: String,
    val userId: String,
    val pickupLocation: LocationData,
    val dropoffLocation: LocationData,
    val vehicleType: String,
    val fareEstimate: Double,
    val status: String,
    val requestedAt: Long,
    val driverId: String? = null,
    val driverAllocatedAt: Long? = null,
    val driverAcceptedAt: Long? = null,
    val driverRejectedAt: Long? = null
)

data class ActiveRide(
    val rideId: String,
    val userId: String,
    val driverId: String,
    val pickupLocation: LocationData,
    val dropoffLocation: LocationData,
    val vehicleType: String,
    val fareEstimate: Double,
    val status: String,
    val acceptedAt: Long,
    val estimatedArrivalTime: Int,
    val estimatedFare: Double
)

enum class DriverResponse {
    ACCEPT, REJECT
}

enum class MatchingStatus {
    IDLE,
    SEARCHING,
    DRIVER_NOTIFIED,
    DRIVER_ACCEPTED,
    DRIVER_REJECTED,
    NO_DRIVERS_AVAILABLE,
    NOTIFICATION_FAILED,
    ERROR
}

sealed class RideRequestResult {
    data class Success(
        val rideId: String,
        val driverAllocation: DriverAllocationResult,
        val message: String
    ) : RideRequestResult()
    
    data class Failure(val error: String) : RideRequestResult()
}
