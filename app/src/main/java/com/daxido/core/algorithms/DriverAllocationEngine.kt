package com.daxido.core.algorithms

import android.util.Log
import com.daxido.core.models.*
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

@Singleton
class DriverAllocationEngine @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseDatabase: FirebaseDatabase
) {

    private val realtimeDb = firebaseDatabase

    private val driverPool = ConcurrentHashMap<String, DriverPoolEntry>()
    private val activeAllocations = ConcurrentHashMap<String, AllocationStatus>()
    private val zoneDriverCounts = ConcurrentHashMap<String, Int>()

    companion object {
        private const val TAG = "DriverAllocationEngine"
        const val MAX_SEARCH_RADIUS_KM = 10.0
        const val INITIAL_SEARCH_RADIUS_KM = 2.0
        const val RADIUS_INCREMENT_KM = 1.0
        const val MAX_DRIVERS_TO_NOTIFY = 5
        const val DRIVER_RESPONSE_TIMEOUT_SEC = 15
        const val GEOHASH_PRECISION = 7
    }

    data class DriverPoolEntry(
        val driverId: String,
        val location: Location,
        val vehicleType: VehicleType,
        val rating: Float,
        val completedTrips: Int,
        val lastUpdateTime: Long,
        val isAvailable: Boolean,
        val currentRideId: String? = null,
        val geohash: String,
        val zone: String
    )

    data class AllocationStatus(
        val rideId: String,
        val notifiedDrivers: List<String>,
        val startTime: Long,
        val currentRadius: Double,
        val attempts: Int
    )

    suspend fun allocateDriver(rideRequest: RideRequest): Result<String> = withContext(Dispatchers.IO) {
        try {
            val allocation = AllocationStatus(
                rideId = rideRequest.userId + System.currentTimeMillis(),
                notifiedDrivers = emptyList(),
                startTime = System.currentTimeMillis(),
                currentRadius = INITIAL_SEARCH_RADIUS_KM,
                attempts = 0
            )

            activeAllocations[allocation.rideId] = allocation

            // Progressive search with expanding radius
            var currentRadius = INITIAL_SEARCH_RADIUS_KM
            while (currentRadius <= MAX_SEARCH_RADIUS_KM) {
                val nearbyDrivers = findNearbyDrivers(
                    location = rideRequest.pickupLocation,
                    radiusKm = currentRadius,
                    vehicleType = rideRequest.vehicleType,
                    excludeDrivers = allocation.notifiedDrivers
                )

                if (nearbyDrivers.isNotEmpty()) {
                    // Rank drivers using multi-factor scoring
                    val rankedDrivers = rankDrivers(
                        drivers = nearbyDrivers,
                        pickupLocation = rideRequest.pickupLocation,
                        dropLocation = rideRequest.dropLocation
                    )

                    // Send notifications to top drivers
                    val driversToNotify = rankedDrivers.take(MAX_DRIVERS_TO_NOTIFY)
                    val acceptedDriverId = notifyAndWaitForAcceptance(
                        drivers = driversToNotify,
                        rideRequest = rideRequest,
                        allocation = allocation
                    )

                    if (acceptedDriverId != null) {
                        // Update driver status
                        updateDriverStatus(acceptedDriverId, false, allocation.rideId)
                        activeAllocations.remove(allocation.rideId)
                        return@withContext Result.success(acceptedDriverId)
                    }
                }

                currentRadius += RADIUS_INCREMENT_KM
                allocation.copy(currentRadius = currentRadius, attempts = allocation.attempts + 1)
            }

            activeAllocations.remove(allocation.rideId)
            Result.failure(Exception("No available drivers found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun findNearbyDrivers(
        location: Location,
        radiusKm: Double,
        vehicleType: VehicleType,
        excludeDrivers: List<String>
    ): List<DriverPoolEntry> {
        val geohash = encodeGeohash(location.latitude, location.longitude, GEOHASH_PRECISION)
        val neighbors = getGeohashNeighbors(geohash, radiusKm)

        return driverPool.values.filter { driver ->
            driver.isAvailable &&
            driver.vehicleType == vehicleType &&
            driver.driverId !in excludeDrivers &&
            neighbors.any { driver.geohash.startsWith(it) } &&
            calculateDistance(
                location.latitude, location.longitude,
                driver.location.latitude, driver.location.longitude
            ) <= radiusKm
        }
    }

    private fun rankDrivers(
        drivers: List<DriverPoolEntry>,
        pickupLocation: Location,
        dropLocation: Location
    ): List<DriverPoolEntry> {
        return drivers.map { driver ->
            val distance = calculateDistance(
                pickupLocation.latitude, pickupLocation.longitude,
                driver.location.latitude, driver.location.longitude
            )

            // Multi-factor scoring algorithm
            val distanceScore = (1.0 - (distance / MAX_SEARCH_RADIUS_KM)) * 40 // 40% weight
            val ratingScore = (driver.rating / 5.0) * 30 // 30% weight
            val experienceScore = min(1.0, driver.completedTrips / 100.0) * 20 // 20% weight
            val responseTimeScore = 10.0 // 10% weight for active drivers

            val totalScore = distanceScore + ratingScore + experienceScore + responseTimeScore

            driver to totalScore
        }.sortedByDescending { it.second }.map { it.first }
    }

    private suspend fun notifyAndWaitForAcceptance(
        drivers: List<DriverPoolEntry>,
        rideRequest: RideRequest,
        allocation: AllocationStatus
    ): String? = coroutineScope {
        val responseChannel = Channel<String>(Channel.BUFFERED)

        // Send notifications to all selected drivers
        drivers.forEach { driver ->
            launch {
                sendRideRequestToDriver(driver.driverId, rideRequest)

                // Listen for acceptance
                val accepted = waitForDriverResponse(driver.driverId, allocation.rideId)
                if (accepted) {
                    responseChannel.send(driver.driverId)
                }
            }
        }

        // Wait for first acceptance or timeout
        return@coroutineScope withTimeoutOrNull(DRIVER_RESPONSE_TIMEOUT_SEC * 1000L) {
            responseChannel.receive()
        }
    }

    private suspend fun sendRideRequestToDriver(driverId: String, rideRequest: RideRequest) {
        val notification = hashMapOf(
            "type" to "RIDE_REQUEST",
            "rideId" to rideRequest.userId,
            "pickup" to mapOf(
                "lat" to rideRequest.pickupLocation.latitude,
                "lng" to rideRequest.pickupLocation.longitude,
                "address" to rideRequest.pickupLocation.address
            ),
            "drop" to mapOf(
                "lat" to rideRequest.dropLocation.latitude,
                "lng" to rideRequest.dropLocation.longitude,
                "address" to rideRequest.dropLocation.address
            ),
            "estimatedFare" to rideRequest.estimatedFare.total,
            "vehicleType" to rideRequest.vehicleType.name,
            "timestamp" to System.currentTimeMillis()
        )

        // Send notification via Firebase
        firebaseDatabase.getReference("driver_notifications/$driverId")
            .push()
            .setValue(notification)
            .await()

        // Also send FCM push notification
        sendFCMNotification(driverId, "New Ride Request", "Pickup: ${rideRequest.pickupLocation.address}")

        realtimeDb.getReference("driver_notifications/$driverId")
            .push()
            .setValue(notification)
    }

    private suspend fun waitForDriverResponse(driverId: String, rideId: String): Boolean {
        return withTimeoutOrNull(DRIVER_RESPONSE_TIMEOUT_SEC * 1000L) {
            // Poll for driver response
            var accepted = false
            while (!accepted) {
                delay(500)
                // Check if driver accepted in Firebase
                // This would be replaced with actual Firebase listener
                accepted = checkDriverAcceptance(driverId, rideId)
            }
            accepted
        } ?: false
    }

    private suspend fun checkDriverAcceptance(driverId: String, rideId: String): Boolean {
        return try {
            val acceptanceRef = firebaseDatabase.getReference("ride_acceptances/$rideId/$driverId")
            val snapshot = acceptanceRef.get().await()

            if (snapshot.exists()) {
                val accepted = snapshot.child("accepted").getValue(Boolean::class.java) ?: false
                val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L

                // Check if acceptance is recent (within 30 seconds)
                val isRecent = (System.currentTimeMillis() - timestamp) < 30000

                if (accepted && isRecent) {
                    // Update driver status
                    updateDriverStatus(driverId, false, rideId)

                    // Update ride status in Firestore
                    firestore.collection("rides").document(rideId)
                        .update(
                            "status", "DRIVER_ASSIGNED",
                            "driverId", driverId,
                            "assignedAt", System.currentTimeMillis()
                        ).await()

                    return true
                }
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking driver acceptance", e)
            false
        }
    }

    fun startDriverLocationPolling(driverId: String) {
        GlobalScope.launch {
            while (true) {
                delay(5000) // Poll every 5 seconds
                updateDriverLocation(driverId)
            }
        }
    }

    private suspend fun updateDriverLocation(driverId: String) {
        try {
            val locationRef = firebaseDatabase.getReference("driver_locations/$driverId")
            val snapshot = locationRef.get().await()

            if (snapshot.exists()) {
                val lat = snapshot.child("latitude").getValue(Double::class.java) ?: return
                val lng = snapshot.child("longitude").getValue(Double::class.java) ?: return
                val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L

                // Only update if location is fresh (within 30 seconds)
                if ((System.currentTimeMillis() - timestamp) < 30000) {
                    val location = Location(lat, lng)
                    val geohash = encodeGeohash(lat, lng, 6)
                    val zone = getZoneFromGeohash(geohash)

                    driverPool[driverId]?.let { driver ->
                        driverPool[driverId] = driver.copy(
                            location = location,
                            lastUpdateTime = timestamp,
                            geohash = geohash,
                            zone = zone
                        )
                        updateZoneCount(zone)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating driver location", e)
        }
        realtimeDb.getReference("driver_locations/$driverId")
            .get()
            .addOnSuccessListener { snapshot ->
                val lat = snapshot.child("latitude").getValue(Double::class.java) ?: return@addOnSuccessListener
                val lng = snapshot.child("longitude").getValue(Double::class.java) ?: return@addOnSuccessListener
                val vehicleType = snapshot.child("vehicleType").getValue(String::class.java)?.let {
                    VehicleType.valueOf(it)
                } ?: VehicleType.CAR

                val location = Location(lat, lng)
                val geohash = encodeGeohash(lat, lng, GEOHASH_PRECISION)
                val zone = getZoneFromGeohash(geohash)

                val entry = DriverPoolEntry(
                    driverId = driverId,
                    location = location,
                    vehicleType = vehicleType,
                    rating = snapshot.child("rating").getValue(Float::class.java) ?: 5.0f,
                    completedTrips = snapshot.child("completedTrips").getValue(Int::class.java) ?: 0,
                    lastUpdateTime = System.currentTimeMillis(),
                    isAvailable = snapshot.child("isAvailable").getValue(Boolean::class.java) ?: true,
                    currentRideId = snapshot.child("currentRideId").getValue(String::class.java),
                    geohash = geohash,
                    zone = zone
                )

                driverPool[driverId] = entry
                updateZoneCount(zone)
            }
    }

    private fun updateDriverStatus(driverId: String, isAvailable: Boolean, rideId: String?) {
        driverPool[driverId]?.let { driver ->
            driverPool[driverId] = driver.copy(
                isAvailable = isAvailable,
                currentRideId = rideId
            )
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    private fun encodeGeohash(lat: Double, lon: Double, precision: Int): String {
        val base32 = "0123456789bcdefghjkmnpqrstuvwxyz"
        var minLat = -90.0
        var maxLat = 90.0
        var minLon = -180.0
        var maxLon = 180.0
        var geohash = ""
        var bits = 0
        var bit = 0
        var ch = 0
        var even = true

        while (geohash.length < precision) {
            if (even) {
                val mid = (minLon + maxLon) / 2
                if (lon > mid) {
                    ch = ch or (1 shl (4 - bit))
                    minLon = mid
                } else {
                    maxLon = mid
                }
            } else {
                val mid = (minLat + maxLat) / 2
                if (lat > mid) {
                    ch = ch or (1 shl (4 - bit))
                    minLat = mid
                } else {
                    maxLat = mid
                }
            }

            even = !even

            if (bit < 4) {
                bit++
            } else {
                geohash += base32[ch]
                bits++
                bit = 0
                ch = 0
            }
        }

        return geohash
    }

    private fun getGeohashNeighbors(geohash: String, radiusKm: Double): List<String> {
        // Return geohash prefixes for neighboring areas based on radius
        val precision = when {
            radiusKm <= 1 -> 7
            radiusKm <= 5 -> 6
            radiusKm <= 20 -> 5
            else -> 4
        }

        return if (geohash.length >= precision) {
            listOf(geohash.substring(0, precision))
        } else {
            listOf(geohash)
        }
    }

    private fun getZoneFromGeohash(geohash: String): String {
        // Map geohash to zone for zone-based driver management
        return geohash.take(4)
    }

    private fun updateZoneCount(zone: String) {
        val count = driverPool.values.count { it.zone == zone && it.isAvailable }
        zoneDriverCounts[zone] = count
    }

    fun getSurgeMultiplier(location: Location): Float {
        val geohash = encodeGeohash(location.latitude, location.longitude, 4)
        val zone = getZoneFromGeohash(geohash)
        val availableDrivers = zoneDriverCounts[zone] ?: 0

        return when {
            availableDrivers == 0 -> 2.5f
            availableDrivers < 3 -> 2.0f
            availableDrivers < 5 -> 1.5f
            availableDrivers < 10 -> 1.2f
            else -> 1.0f
        }
    }

    private suspend fun sendFCMNotification(driverId: String, title: String, body: String) {
        try {
            // Get driver's FCM token
            val driverDoc = firestore.collection("drivers").document(driverId).get().await()
            val fcmToken = driverDoc.getString("fcmToken") ?: return

            // Send notification via FCM (this would typically be done via Cloud Functions)
            val notificationData = hashMapOf(
                "to" to fcmToken,
                "notification" to hashMapOf(
                    "title" to title,
                    "body" to body,
                    "priority" to "high",
                    "sound" to "default"
                ),
                "data" to hashMapOf(
                    "type" to "RIDE_REQUEST",
                    "click_action" to "DRIVER_RIDE_REQUEST"
                )
            )

            // Store notification request for Cloud Function to process
            firebaseDatabase.getReference("fcm_queue")
                .push()
                .setValue(notificationData)
                .await()

            Log.d(TAG, "FCM notification queued for driver: $driverId")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending FCM notification", e)
        }
    }
}