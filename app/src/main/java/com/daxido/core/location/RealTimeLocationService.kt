package com.daxido.core.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real-time location tracking service using Google Play Services
 */
@Singleton
class RealTimeLocationService @Inject constructor(
    private val context: Context
) {
    
    private val fusedLocationClient: FusedLocationProviderClient = 
        LocationServices.getFusedLocationProviderClient(context)
    
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let { location ->
                _currentLocation.value = LocationData(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracy = location.accuracy,
                    speed = location.speed,
                    bearing = location.bearing,
                    timestamp = location.time
                )
            }
        }
    }
    
    private val _currentLocation = MutableStateFlow<LocationData?>(null)
    val currentLocation: Flow<LocationData?> = _currentLocation.asStateFlow()
    
    private val _isTracking = MutableStateFlow(false)
    val isTracking: Flow<Boolean> = _isTracking.asStateFlow()
    
    /**
     * Start real-time location tracking
     */
    fun startLocationTracking(
        interval: Long = 1000L, // 1 second
        fastestInterval: Long = 500L, // 0.5 seconds
        priority: Int = LocationRequest.PRIORITY_HIGH_ACCURACY
    ): Flow<LocationData> = callbackFlow {
        // Check permissions before starting
        if (!hasLocationPermission()) {
            android.util.Log.e("RealTimeLocationService", "Location permission not granted")
            _isTracking.value = false
            close(SecurityException("Location permission not granted"))
            return@callbackFlow
        }

        val locationRequest = LocationRequest.Builder(interval)
            .setPriority(priority)
            .setMinUpdateIntervalMillis(fastestInterval)
            .setMaxUpdateDelayMillis(2000L)
            .build()

        try {
            // Request location updates with permission check
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )

                _isTracking.value = true

                // Get last known location immediately
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        val locationData = LocationData(
                            latitude = it.latitude,
                            longitude = it.longitude,
                            accuracy = it.accuracy,
                            speed = it.speed,
                            bearing = it.bearing,
                            timestamp = it.time
                        )
                        _currentLocation.value = locationData
                        trySend(locationData)
                    }
                }.addOnFailureListener { e ->
                    android.util.Log.e("RealTimeLocationService", "Failed to get last location", e)
                }
            } else {
                android.util.Log.e("RealTimeLocationService", "Location permission not available")
                _isTracking.value = false
                close(SecurityException("Location permission not available"))
            }

            awaitClose {
                stopLocationTracking()
            }

        } catch (e: SecurityException) {
            android.util.Log.e("RealTimeLocationService", "SecurityException in location tracking", e)
            _isTracking.value = false
            close(e)
        } catch (e: Exception) {
            android.util.Log.e("RealTimeLocationService", "Exception in location tracking", e)
            _isTracking.value = false
            close(e)
        }
    }
    
    /**
     * Stop location tracking
     */
    fun stopLocationTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        _isTracking.value = false
    }
    
    /**
     * Get current location once
     */
    suspend fun getCurrentLocation(): LocationData? {
        if (!hasLocationPermission()) {
            return null
        }
        
        return try {
            val location = fusedLocationClient.lastLocation.await()
            location?.let {
                LocationData(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    accuracy = it.accuracy,
                    speed = it.speed,
                    bearing = it.bearing,
                    timestamp = it.time
                )
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get location updates for a specific duration
     */
    fun getLocationUpdates(
        duration: Long = 30000L, // 30 seconds
        interval: Long = 1000L
    ): Flow<List<LocationData>> = callbackFlow {
        // Check permissions first
        if (!hasLocationPermission()) {
            android.util.Log.e("RealTimeLocationService", "Location permission not granted for updates")
            close(SecurityException("Location permission not granted"))
            return@callbackFlow
        }

        val locations = mutableListOf<LocationData>()
        val startTime = System.currentTimeMillis()

        val locationRequest = LocationRequest.Builder(interval)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setMinUpdateIntervalMillis(500L)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val locationData = LocationData(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracy = location.accuracy,
                        speed = location.speed,
                        bearing = location.bearing,
                        timestamp = location.time
                    )

                    locations.add(locationData)

                    if (System.currentTimeMillis() - startTime >= duration) {
                        trySend(locations.toList())
                        close()
                    }
                }
            }
        }

        try {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    callback,
                    Looper.getMainLooper()
                )
            } else {
                android.util.Log.e("RealTimeLocationService", "Permission denied for location updates")
                close(SecurityException("Permission denied"))
            }

            awaitClose {
                fusedLocationClient.removeLocationUpdates(callback)
            }

        } catch (e: SecurityException) {
            android.util.Log.e("RealTimeLocationService", "SecurityException in getLocationUpdates", e)
            close(e)
        } catch (e: Exception) {
            android.util.Log.e("RealTimeLocationService", "Exception in getLocationUpdates", e)
            close(e)
        }
    }
    
    /**
     * Calculate distance between two points
     */
    fun calculateDistance(point1: LatLng, point2: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            point1.latitude, point1.longitude,
            point2.latitude, point2.longitude,
            results
        )
        return results[0]
    }
    
    /**
     * Calculate bearing between two points
     */
    fun calculateBearing(point1: LatLng, point2: LatLng): Float {
        val results = FloatArray(2)
        Location.distanceBetween(
            point1.latitude, point1.longitude,
            point2.latitude, point2.longitude,
            results
        )
        return results[1]
    }
    
    /**
     * Check if location permission is granted
     */
    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Get location accuracy level
     */
    fun getLocationAccuracy(accuracy: Float): LocationAccuracy {
        return when {
            accuracy <= 5f -> LocationAccuracy.EXCELLENT
            accuracy <= 10f -> LocationAccuracy.GOOD
            accuracy <= 20f -> LocationAccuracy.FAIR
            accuracy <= 50f -> LocationAccuracy.POOR
            else -> LocationAccuracy.VERY_POOR
        }
    }
}

/**
 * Data class for location information
 */
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val speed: Float,
    val bearing: Float,
    val timestamp: Long
) {
    fun toLatLng(): LatLng = LatLng(latitude, longitude)
    
    fun isValid(): Boolean {
        return latitude != 0.0 && longitude != 0.0 && accuracy > 0
    }
    
    fun isRecent(maxAge: Long = 30000L): Boolean {
        return System.currentTimeMillis() - timestamp < maxAge
    }
}

/**
 * Location accuracy levels
 */
enum class LocationAccuracy {
    EXCELLENT,  // <= 5m
    GOOD,       // <= 10m
    FAIR,       // <= 20m
    POOR,       // <= 50m
    VERY_POOR   // > 50m
}

