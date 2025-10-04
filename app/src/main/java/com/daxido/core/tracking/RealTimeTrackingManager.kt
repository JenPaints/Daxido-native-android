package com.daxido.core.tracking

import android.location.Location
import com.daxido.core.algorithms.DriverAllocationEngine
import com.daxido.core.maps.DirectionsService
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

@Singleton
class RealTimeTrackingManager @Inject constructor(
    private val realtimeDb: FirebaseDatabase,
    private val directionsService: DirectionsService,
    private val allocationEngine: DriverAllocationEngine
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val activeTracking = ConcurrentHashMap<String, TrackingSession>()
    private val locationBuffer = ConcurrentHashMap<String, LocationBuffer>()
    private val trackingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        // Location update intervals
        const val LOCATION_UPDATE_INTERVAL_MS = 3000L
        const val POLYLINE_REFRESH_INTERVAL_MS = 15000L
        const val MAX_LOCATION_AGE_MS = 10000L

        // Location accuracy thresholds
        const val LOCATION_ACCURACY_THRESHOLD_M = 50f
        const val SPEED_THRESHOLD_KMH = 5f
        const val HEADING_CHANGE_THRESHOLD_DEG = 30f

        // MAGIC NUMBERS REPLACED: Speed and filtering constants
        const val MAX_REALISTIC_SPEED_KMH = 200.0 // Maximum realistic vehicle speed
        const val KALMAN_FILTER_WINDOW = 5 // Number of recent locations for filtering
        const val LOCATION_BUFFER_MAX_SIZE = 100 // Maximum buffered locations per ride
        const val SNAPPING_CONFIDENCE_THRESHOLD = 0.95f // Road snapping confidence
        const val MIN_DISPLACEMENT_METERS = 10.0 // Minimum displacement to update
    }

    data class TrackingSession(
        val rideId: String,
        val driverId: String,
        val userId: String,
        val startTime: Long,
        val polylinePoints: MutableList<LatLng>,
        var currentPolyline: String,
        var distanceTraveled: Double = 0.0,
        var lastLocation: Location? = null,
        var isActive: Boolean = true
    )

    data class LocationBuffer(
        val locations: MutableList<LocationUpdate>,
        var lastProcessedTime: Long = 0
    )

    data class LocationUpdate(
        val location: Location,
        val timestamp: Long,
        val accuracy: Float,
        val speed: Float,
        val bearing: Float,
        val provider: String
    )

    data class TrackingMetrics(
        val totalDistance: Double,
        val averageSpeed: Double,
        val maxSpeed: Double,
        val totalDuration: Long,
        val idleTime: Long,
        val movingTime: Long,
        val polylineAccuracy: Float
    )

    fun startTracking(
        rideId: String,
        driverId: String,
        userId: String,
        initialPolyline: String
    ): Flow<TrackingUpdate> = callbackFlow {
        val session = TrackingSession(
            rideId = rideId,
            driverId = driverId,
            userId = userId,
            startTime = System.currentTimeMillis(),
            polylinePoints = mutableListOf(),
            currentPolyline = initialPolyline
        )

        activeTracking[rideId] = session
        locationBuffer[rideId] = LocationBuffer(mutableListOf())

        // Start location listener for driver
        val driverLocationRef = realtimeDb.getReference("active_rides/$rideId/driver_location")
        val locationListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lat = snapshot.child("latitude").getValue(Double::class.java) ?: return
                val lng = snapshot.child("longitude").getValue(Double::class.java) ?: return
                val accuracy = snapshot.child("accuracy").getValue(Float::class.java) ?: 0f
                val speed = snapshot.child("speed").getValue(Float::class.java) ?: 0f
                val bearing = snapshot.child("bearing").getValue(Float::class.java) ?: 0f
                val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()

                val location = Location("firebase").apply {
                    latitude = lat
                    longitude = lng
                    this.accuracy = accuracy
                    this.speed = speed
                    this.bearing = bearing
                    time = timestamp
                }

                processLocationUpdate(session, location)

                val update = TrackingUpdate(
                    rideId = rideId,
                    currentLocation = LatLng(lat, lng),
                    speed = speed,
                    bearing = bearing,
                    distanceTraveled = session.distanceTraveled,
                    estimatedTimeRemaining = calculateETA(session, location),
                    polylinePoints = session.polylinePoints.toList(),
                    accuracy = accuracy,
                    timestamp = timestamp
                )

                trySend(update)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        driverLocationRef.addValueEventListener(locationListener)

        // Start polyline refresh job
        val polylineJob = trackingScope.launch {
            while (session.isActive) {
                delay(POLYLINE_REFRESH_INTERVAL_MS)
                refreshPolyline(session)
            }
        }

        // Start metrics calculation job
        val metricsJob = trackingScope.launch {
            while (session.isActive) {
                delay(5000)
                val metrics = calculateMetrics(session)
                updateMetricsInFirebase(rideId, metrics)
            }
        }

        awaitClose {
            session.isActive = false
            driverLocationRef.removeEventListener(locationListener)
            polylineJob.cancel()
            metricsJob.cancel()
            activeTracking.remove(rideId)
            locationBuffer.remove(rideId)
        }
    }

    private fun processLocationUpdate(session: TrackingSession, location: Location) {
        // Filter invalid locations
        if (!isValidLocation(location, session.lastLocation)) {
            return
        }

        // Add to buffer for smoothing
        locationBuffer[session.rideId]?.let { buffer ->
            buffer.locations.add(
                LocationUpdate(
                    location = location,
                    timestamp = location.time,
                    accuracy = location.accuracy,
                    speed = location.speed,
                    bearing = location.bearing,
                    provider = location.provider ?: "unknown"
                )
            )

            // MEMORY LEAK FIX: Limit buffer size to prevent unbounded growth
            // Keep only the last 100 location updates for smoothing
            if (buffer.locations.size > 100) {
                buffer.locations.removeAt(0)
            }

            // Apply Kalman filter for smoothing
            val smoothedLocation = applyKalmanFilter(buffer.locations)

            // Update session
            session.lastLocation?.let { last ->
                val distance = calculateDistance(
                    last.latitude, last.longitude,
                    smoothedLocation.latitude, smoothedLocation.longitude
                )
                session.distanceTraveled += distance
            }

            session.lastLocation = smoothedLocation
            session.polylinePoints.add(LatLng(smoothedLocation.latitude, smoothedLocation.longitude))

            // Snap to road if needed
            coroutineScope.launch {
                snapToRoad(session, smoothedLocation)
            }
        }
    }

    private fun isValidLocation(location: Location, lastLocation: Location?): Boolean {
        // Check accuracy
        if (location.accuracy > LOCATION_ACCURACY_THRESHOLD_M) {
            return false
        }

        // Check age
        val age = System.currentTimeMillis() - location.time
        if (age > MAX_LOCATION_AGE_MS) {
            return false
        }

        // Check for jumps
        lastLocation?.let { last ->
            val timeDiff = (location.time - last.time) / 1000.0 // seconds
            if (timeDiff > 0) {
                val distance = calculateDistance(
                    last.latitude, last.longitude,
                    location.latitude, location.longitude
                )
                val speed = (distance / timeDiff) * 3600 // km/h

                // Reject if speed is unrealistic
                if (speed > MAX_REALISTIC_SPEED_KMH) {
                    return false
                }
            }
        }

        return true
    }

    /**
     * KDOC ADDED: Apply Kalman-like filter to smooth location updates
     *
     * Uses weighted moving average with recent location data to reduce GPS noise.
     * More recent locations receive higher weights for better accuracy.
     *
     * Algorithm:
     * 1. Take last N locations (defined by KALMAN_FILTER_WINDOW)
     * 2. Apply increasing weights to more recent locations
     * 3. Calculate weighted average of coordinates
     *
     * @param locations List of location updates to filter
     * @return Filtered/smoothed location
     */
    private fun applyKalmanFilter(locations: List<LocationUpdate>): Location {
        if (locations.isEmpty()) {
            return Location("filtered")
        }

        val recentLocations = locations.takeLast(KALMAN_FILTER_WINDOW)
        val weightedLat = recentLocations.mapIndexed { index, update ->
            val weight = (index + 1).toDouble() / recentLocations.size
            update.location.latitude * weight
        }.sum() / recentLocations.mapIndexed { index, _ ->
            (index + 1).toDouble() / recentLocations.size
        }.sum()

        val weightedLng = recentLocations.mapIndexed { index, update ->
            val weight = (index + 1).toDouble() / recentLocations.size
            update.location.longitude * weight
        }.sum() / recentLocations.mapIndexed { index, _ ->
            (index + 1).toDouble() / recentLocations.size
        }.sum()

        return Location("filtered").apply {
            latitude = weightedLat
            longitude = weightedLng
            accuracy = recentLocations.map { it.accuracy }.average().toFloat()
            speed = recentLocations.last().speed
            bearing = recentLocations.last().bearing
            time = recentLocations.last().timestamp
        }
    }

    private suspend fun snapToRoad(session: TrackingSession, location: Location) {
        // Use Google Roads API to snap to nearest road
        val url = "https://roads.googleapis.com/v1/snapToRoads?" +
                "path=${location.latitude},${location.longitude}&" +
                "interpolate=true&key=${com.daxido.core.config.AppConfig.GOOGLE_MAPS_API_KEY}"

        // Implementation would call Roads API
        // For now, we'll use the raw location
    }

    private suspend fun refreshPolyline(session: TrackingSession) {
        session.lastLocation?.let { currentLocation ->
            // Get destination from Firebase
            val destinationRef = realtimeDb.getReference("active_rides/${session.rideId}/drop_location")
            destinationRef.get().addOnSuccessListener { snapshot ->
                val destLat = snapshot.child("lat").getValue(Double::class.java) ?: return@addOnSuccessListener
                val destLng = snapshot.child("lng").getValue(Double::class.java) ?: return@addOnSuccessListener

                trackingScope.launch {
                    val result = directionsService.getRoute(
                        origin = com.daxido.core.models.Location(
                            currentLocation.latitude,
                            currentLocation.longitude
                        ),
                        destination = com.daxido.core.models.Location(destLat, destLng)
                    )

                    result.getOrNull()?.let { routeInfo ->
                        session.currentPolyline = routeInfo.encodedPolyline
                        updatePolylineInFirebase(session.rideId, routeInfo.encodedPolyline)
                    }
                }
            }
        }
    }

    private fun calculateETA(session: TrackingSession, currentLocation: Location): Int {
        // Calculate based on remaining distance and current speed
        val averageSpeed = if (session.lastLocation != null) {
            val timeDiff = (currentLocation.time - session.lastLocation!!.time) / 1000.0
            if (timeDiff > 0) {
                val distance = calculateDistance(
                    session.lastLocation!!.latitude,
                    session.lastLocation!!.longitude,
                    currentLocation.latitude,
                    currentLocation.longitude
                )
                (distance / timeDiff) * 3600 // km/h
            } else {
                30.0 // Default 30 km/h
            }
        } else {
            30.0
        }

        // This would use actual remaining distance from polyline
        val remainingDistance = 5.0 // Placeholder km
        return (remainingDistance / averageSpeed * 60).toInt() // minutes
    }

    private fun calculateMetrics(session: TrackingSession): TrackingMetrics {
        val currentTime = System.currentTimeMillis()
        val totalDuration = currentTime - session.startTime

        val speeds = locationBuffer[session.rideId]?.locations?.map {
            it.speed * 3.6 // Convert m/s to km/h
        } ?: emptyList()

        val averageSpeed = if (speeds.isNotEmpty()) speeds.average() else 0.0
        val maxSpeed = speeds.maxOrNull() ?: 0.0

        val movingTime = locationBuffer[session.rideId]?.locations?.count {
            it.speed * 3.6 > SPEED_THRESHOLD_KMH
        }?.times(LOCATION_UPDATE_INTERVAL_MS) ?: 0L

        val idleTime = totalDuration - movingTime

        return TrackingMetrics(
            totalDistance = session.distanceTraveled,
            averageSpeed = averageSpeed,
            maxSpeed = maxSpeed,
            totalDuration = totalDuration,
            idleTime = idleTime,
            movingTime = movingTime,
            polylineAccuracy = calculatePolylineAccuracy(session)
        )
    }

    private fun calculatePolylineAccuracy(session: TrackingSession): Float {
        // Calculate how closely the driver is following the polyline
        return 0.95f // Placeholder
    }

    private fun updateMetricsInFirebase(rideId: String, metrics: TrackingMetrics) {
        val metricsMap = mapOf(
            "totalDistance" to metrics.totalDistance,
            "averageSpeed" to metrics.averageSpeed,
            "maxSpeed" to metrics.maxSpeed,
            "totalDuration" to metrics.totalDuration,
            "idleTime" to metrics.idleTime,
            "movingTime" to metrics.movingTime,
            "polylineAccuracy" to metrics.polylineAccuracy,
            "lastUpdated" to System.currentTimeMillis()
        )

        realtimeDb.getReference("active_rides/$rideId/metrics")
            .setValue(metricsMap)
    }

    private fun updatePolylineInFirebase(rideId: String, encodedPolyline: String) {
        realtimeDb.getReference("active_rides/$rideId/current_polyline")
            .setValue(encodedPolyline)
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

    fun stopTracking(rideId: String) {
        activeTracking[rideId]?.isActive = false
        activeTracking.remove(rideId)
        locationBuffer.remove(rideId)
    }

    data class TrackingUpdate(
        val rideId: String,
        val currentLocation: LatLng,
        val speed: Float,
        val bearing: Float,
        val distanceTraveled: Double,
        val estimatedTimeRemaining: Int,
        val polylinePoints: List<LatLng>,
        val accuracy: Float,
        val timestamp: Long
    )
}