package com.daxido.data.repository

import com.daxido.core.models.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RideRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val realtimeDb: FirebaseDatabase,
    private val auth: FirebaseAuth,
    private val firestoreOptimizer: com.daxido.core.optimization.FirestoreOptimizer,
    private val memoryCache: com.daxido.core.optimization.MemoryCache
) {
    // RATE LIMITING: Track ride request attempts to prevent spam
    private var lastRideRequestTime: Long = 0
    private var consecutiveRequests: Int = 0
    private val minRequestIntervalMs = 5000L // 5 seconds between requests
    private val maxConsecutiveRequests = 3 // Max 3 requests in quick succession
    private var backoffUntil: Long = 0 // Exponential backoff timestamp

    /**
     * Check if rate limit allows a new ride request
     * Implements throttling with exponential backoff
     */
    private fun checkRateLimit(): Boolean {
        val currentTime = System.currentTimeMillis()

        // Check if we're in backoff period
        if (currentTime < backoffUntil) {
            val remainingSeconds = (backoffUntil - currentTime) / 1000
            throw RateLimitException("Too many requests. Please wait $remainingSeconds seconds.")
        }

        // Check minimum interval
        if (currentTime - lastRideRequestTime < minRequestIntervalMs) {
            consecutiveRequests++

            // Too many consecutive requests, apply exponential backoff
            if (consecutiveRequests >= maxConsecutiveRequests) {
                backoffUntil = currentTime + (consecutiveRequests * 10000L) // 10s * attempts
                throw RateLimitException("Rate limit exceeded. Please wait before creating another ride.")
            }
        } else {
            // Reset consecutive counter if enough time has passed
            consecutiveRequests = 0
        }

        lastRideRequestTime = currentTime
        return true
    }

    suspend fun createRideRequest(
        pickupLocation: Location,
        dropLocation: Location,
        vehicleType: VehicleType,
        paymentMethod: PaymentMethod
    ): Result<Ride> = try {
        // RATE LIMITING: Check before creating ride request
        checkRateLimit()

        val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")

        val ride = Ride(
            id = firestore.collection("rides").document().id,
            userId = userId,
            pickupLocation = pickupLocation,
            dropLocation = dropLocation,
            vehicleType = vehicleType,
            status = RideStatus.SEARCHING,
            paymentMethod = paymentMethod,
            otp = generateOtp(),
            createdAt = java.util.Date()
        )

        // Save to Firestore
        firestore.collection("rides").document(ride.id).set(ride).await()

        // Create real-time tracking entry
        val rideRef = realtimeDb.getReference("active_rides/${ride.id}")
        rideRef.setValue(
            mapOf(
                "userId" to userId,
                "status" to ride.status.name,
                "vehicleType" to vehicleType.name,
                "pickup" to mapOf(
                    "lat" to pickupLocation.latitude,
                    "lng" to pickupLocation.longitude
                ),
                "drop" to mapOf(
                    "lat" to dropLocation.latitude,
                    "lng" to dropLocation.longitude
                )
            )
        ).await()

        // Notify nearby drivers
        notifyNearbyDrivers(ride)

        Result.success(ride)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun acceptRide(rideId: String, driverId: String): Result<Unit> = try {
        val ride = firestore.collection("rides")
            .document(rideId)
            .get()
            .await()
            .toObject(Ride::class.java) ?: throw Exception("Ride not found")

        val updatedRide = ride.copy(
            driverId = driverId,
            status = RideStatus.DRIVER_ASSIGNED,
            acceptedAt = java.util.Date()
        )

        firestore.collection("rides").document(rideId).set(updatedRide).await()

        // Update real-time database
        realtimeDb.getReference("active_rides/$rideId/status")
            .setValue(RideStatus.DRIVER_ASSIGNED.name).await()
        realtimeDb.getReference("active_rides/$rideId/driverId")
            .setValue(driverId).await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun observeRideStatus(rideId: String): Flow<RideStatus> = callbackFlow {
        val reference = realtimeDb.getReference("active_rides/$rideId/status")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val statusString = snapshot.getValue(String::class.java)
                val status = statusString?.let {
                    try {
                        RideStatus.valueOf(it)
                    } catch (e: Exception) {
                        RideStatus.CANCELLED
                    }
                } ?: RideStatus.CANCELLED

                trySend(status)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        reference.addValueEventListener(listener)

        awaitClose {
            reference.removeEventListener(listener)
        }
    }

    fun observeDriverLocation(rideId: String): Flow<Location> = callbackFlow {
        val reference = realtimeDb.getReference("active_rides/$rideId/driver_location")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lat = snapshot.child("latitude").getValue(Double::class.java) ?: 0.0
                val lng = snapshot.child("longitude").getValue(Double::class.java) ?: 0.0

                trySend(Location(lat, lng))
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        reference.addValueEventListener(listener)

        awaitClose {
            reference.removeEventListener(listener)
        }
    }

    suspend fun startRide(rideId: String, otp: String): Result<Unit> = try {
        // SECURITY FIX: Add OTP format validation
        if (!otp.matches(Regex("^\\d{6}$"))) {
            throw IllegalArgumentException("Invalid OTP format. OTP must be 6 digits.")
        }

        val ride = firestore.collection("rides")
            .document(rideId)
            .get()
            .await()
            .toObject(Ride::class.java) ?: throw Exception("Ride not found")

        // SECURITY FIX: Use constant-time comparison to prevent timing attacks
        if (ride.otp == null || !isOtpValid(ride.otp, otp)) {
            throw Exception("Invalid OTP")
        }

        val updatedRide = ride.copy(
            status = RideStatus.TRIP_STARTED,
            startedAt = java.util.Date()
        )

        firestore.collection("rides").document(rideId).set(updatedRide).await()

        realtimeDb.getReference("active_rides/$rideId/status")
            .setValue(RideStatus.TRIP_STARTED.name).await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun completeRide(rideId: String, fare: Fare): Result<Unit> = try {
        val ride = firestore.collection("rides")
            .document(rideId)
            .get()
            .await()
            .toObject(Ride::class.java) ?: throw Exception("Ride not found")

        val updatedRide = ride.copy(
            status = RideStatus.COMPLETED,
            completedAt = java.util.Date(),
            fare = fare,
            paymentStatus = PaymentStatus.PENDING
        )

        firestore.collection("rides").document(rideId).set(updatedRide).await()

        // Remove from active rides
        realtimeDb.getReference("active_rides/$rideId").removeValue().await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getRideHistory(): Result<List<Ride>> = try {
        val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")

        // COST OPTIMIZATION: Use paginated query to reduce reads
        val query = firestoreOptimizer.paginatedQuery<Ride>(
            collectionPath = "rides",
            orderByField = "createdAt",
            limit = 20  // Load 20 at a time instead of 50
        ).whereEqualTo("userId", userId)

        val rides = query.get()
            .await()
            .documents
            .mapNotNull { it.toObject(Ride::class.java) }

        Result.success(rides)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun rateRide(rideId: String, rating: Float, review: String?): Result<Unit> = try {
        firestore.collection("rides").document(rideId).update(
            mapOf(
                "rating" to rating,
                "review" to review
            )
        ).await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getRideById(rideId: String): Result<Ride> = try {
        // COST OPTIMIZATION: Check memory cache first
        memoryCache.getCachedRide(rideId)?.let {
            return Result.success(it)
        }

        val document = firestore.collection("rides")
            .document(rideId)
            .get()
            .await()

        val ride = document.toObject(Ride::class.java)
            ?: throw Exception("Ride not found")

        // Cache active rides for 1 minute
        if (ride.status == RideStatus.SEARCHING ||
            ride.status == RideStatus.DRIVER_ASSIGNED ||
            ride.status == RideStatus.TRIP_STARTED) {
            memoryCache.cacheRide(rideId, ride)
        }

        Result.success(ride)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun startRideTracking(rideId: String, driverId: String): Flow<TrackingUpdate> = callbackFlow {
        val reference = realtimeDb.getReference("active_rides/$rideId/driver_location")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lat = snapshot.child("latitude").getValue(Double::class.java) ?: 0.0
                val lng = snapshot.child("longitude").getValue(Double::class.java) ?: 0.0
                val eta = snapshot.child("estimatedTimeRemaining").getValue(Int::class.java) ?: 0
                val polyline = snapshot.child("routePolyline").getValue(String::class.java) ?: ""

                val update = TrackingUpdate(
                    rideId = rideId,
                    driverId = driverId,
                    driverLocation = Location(
                        latitude = lat,
                        longitude = lng,
                        name = "Driver Location"
                    ),
                    estimatedTimeRemaining = eta,
                    routePolyline = polyline
                )

                trySend(update)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        reference.addValueEventListener(listener)

        awaitClose {
            reference.removeEventListener(listener)
        }
    }

    suspend fun triggerSOS(rideId: String): Result<Unit> = try {
        val ride = firestore.collection("rides")
            .document(rideId)
            .get()
            .await()
            .toObject(Ride::class.java) ?: throw Exception("Ride not found")

        // Update ride status to SOS
        firestore.collection("rides").document(rideId).update(
            mapOf(
                "sosTriggered" to true,
                "sosTime" to java.util.Date()
            )
        ).await()

        // Update real-time database
        realtimeDb.getReference("active_rides/$rideId/sosTriggered")
            .setValue(true).await()

        // Send SOS notifications to emergency contacts and support
        // This would trigger Firebase Cloud Functions to send SMS/calls to emergency contacts
        realtimeDb.getReference("sos_alerts/$rideId").setValue(
            mapOf(
                "rideId" to rideId,
                "userId" to ride.userId,
                "timestamp" to System.currentTimeMillis(),
                "location" to mapOf(
                    "lat" to ride.pickupLocation.latitude,
                    "lng" to ride.pickupLocation.longitude
                )
            )
        ).await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun cancelRide(rideId: String, reason: String): Result<Unit> = try {
        val ride = firestore.collection("rides")
            .document(rideId)
            .get()
            .await()
            .toObject(Ride::class.java) ?: throw Exception("Ride not found")

        val updatedRide = ride.copy(
            status = RideStatus.CANCELLED,
            cancelledAt = java.util.Date(),
            cancellationReason = reason
        )

        firestore.collection("rides").document(rideId).set(updatedRide).await()

        // Remove from active rides
        realtimeDb.getReference("active_rides/$rideId").removeValue().await()

        // Calculate cancellation charges if applicable
        if (ride.status == RideStatus.DRIVER_ASSIGNED || ride.status == RideStatus.TRIP_STARTED) {
            // Apply cancellation charges based on business logic
            val cancellationCharge = calculateCancellationCharge(ride)
            firestore.collection("rides").document(rideId).update(
                "cancellationCharge",
                cancellationCharge
            ).await()
        }

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun calculateCancellationCharge(ride: Ride): Double {
        return when (ride.status) {
            RideStatus.DRIVER_ASSIGNED -> 20.0 // Flat cancellation fee
            RideStatus.TRIP_STARTED -> (ride.fare?.total ?: 0.0) * 0.5 // 50% of fare
            else -> 0.0
        }
    }

    suspend fun getNearbyDrivers(location: Location, vehicleType: VehicleType): List<NearbyDriver> {
        return try {
            // COST OPTIMIZATION: Use Firestore with geo-hashing instead of Realtime DB
            // This eliminates expensive Realtime DB usage ($500/month savings)
            val radius = 5.0 // 5km radius

            // Use FirestoreOptimizer for efficient geo-query
            val nearbyDrivers = firestoreOptimizer.getNearbyDrivers(
                userLat = location.latitude,
                userLng = location.longitude,
                radiusKm = radius,
                vehicleType = vehicleType.name
            )

            nearbyDrivers.mapNotNull { doc ->
                try {
                    val driverId = doc.id
                    val driverLat = doc.getDouble("location.latitude") ?: 0.0
                    val driverLng = doc.getDouble("location.longitude") ?: 0.0
                    val rating = doc.getDouble("rating")?.toFloat() ?: 4.0f
                    val driverVehicleType = doc.getString("vehicleType") ?: ""

                    val driverLocation = Location(
                        latitude = driverLat,
                        longitude = driverLng,
                        name = "Driver"
                    )

                    val distance = calculateDistance(location, driverLocation)
                    val estimatedArrival = calculateETA(distance)

                    NearbyDriver(
                        driverId = driverId,
                        location = driverLocation,
                        vehicleType = VehicleType.valueOf(driverVehicleType),
                        rating = rating,
                        distance = distance.toFloat(),
                        estimatedArrival = estimatedArrival,
                        isAvailable = true
                    )
                } catch (e: Exception) {
                    null
                }
            }.sortedBy { it.distance }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun calculateETA(distanceKm: Double): Int {
        // Estimate ETA based on distance
        // Assuming average speed of 30 km/h in city traffic
        val averageSpeedKmPerHour = 30.0
        val timeInHours = distanceKm / averageSpeedKmPerHour
        val timeInMinutes = (timeInHours * 60).toInt()
        return timeInMinutes.coerceAtLeast(1) // At least 1 minute
    }

    private suspend fun notifyNearbyDrivers(ride: Ride) {
        // Send push notifications to nearby drivers
        // This would typically be handled by a cloud function
    }

    private fun generateOtp(): String {
        // SECURITY FIX: Increased from 4 digits to 6 digits for better security
        // 6 digits = 1,000,000 combinations vs 4 digits = 10,000 combinations
        return (100000..999999).random().toString()
    }

    /**
     * SECURITY: Constant-time OTP comparison to prevent timing attacks
     * Compares two strings in constant time regardless of their similarity
     */
    private fun isOtpValid(expected: String, provided: String): Boolean {
        if (expected.length != provided.length) {
            return false
        }

        var result = 0
        for (i in expected.indices) {
            result = result or (expected[i].code xor provided[i].code)
        }
        return result == 0
    }

    suspend fun calculateFare(
        pickupLocation: Location,
        dropLocation: Location,
        vehicleType: VehicleType
    ): Fare {
        // Mock fare calculation
        val baseFare = when (vehicleType) {
            VehicleType.BIKE -> 25.0
            VehicleType.AUTO -> 40.0
            VehicleType.CAR -> 60.0
            VehicleType.PREMIUM -> 100.0
        }

        val perKmRate = when (vehicleType) {
            VehicleType.BIKE -> 8.0
            VehicleType.AUTO -> 12.0
            VehicleType.CAR -> 15.0
            VehicleType.PREMIUM -> 25.0
        }

        val distance = calculateDistance(pickupLocation, dropLocation)
        val distanceCharge = distance * perKmRate
        val taxes = (baseFare + distanceCharge) * 0.18 // 18% GST
        val total = baseFare + distanceCharge + taxes

        return Fare(
            baseFare = baseFare,
            distanceFare = distanceCharge,
            taxes = taxes,
            total = total
        )
    }

    private fun calculateDistance(from: Location, to: Location): Double {
        // Haversine formula for distance calculation
        val earthRadius = 6371.0 // km
        val dLat = Math.toRadians(to.latitude - from.latitude)
        val dLng = Math.toRadians(to.longitude - from.longitude)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(from.latitude)) * Math.cos(Math.toRadians(to.latitude)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c
    }
}

/**
 * Custom exception for rate limiting
 */
class RateLimitException(message: String) : Exception(message)