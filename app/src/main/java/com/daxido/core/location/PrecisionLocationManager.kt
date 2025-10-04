package com.daxido.core.location

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.channels.awaitClose
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

@Singleton
class PrecisionLocationManager @Inject constructor(
    private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient
) : SensorEventListener {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val locationBuffer = ConcurrentLinkedQueue<PrecisionLocation>()
    private val kalmanFilter = ExtendedKalmanFilter()
    private val deadReckoning = DeadReckoningEngine()

    private var lastKnownGoodLocation: PrecisionLocation? = null
    private var isInTunnel = false
    private var lastGpsFixTime = 0L

    companion object {
        const val HIGH_ACCURACY_INTERVAL_MS = 1000L
        const val BALANCED_INTERVAL_MS = 3000L
        const val MAX_LOCATION_AGE_MS = 5000L
        const val MIN_ACCURACY_METERS = 5f
        const val TUNNEL_DETECTION_TIMEOUT_MS = 10000L
        const val FUSION_WEIGHT_GPS = 0.7f
        const val FUSION_WEIGHT_NETWORK = 0.2f
        const val FUSION_WEIGHT_SENSORS = 0.1f
    }

    data class PrecisionLocation(
        val latitude: Double,
        val longitude: Double,
        val altitude: Double?,
        val accuracy: Float,
        val bearing: Float,
        val bearingAccuracy: Float?,
        val speed: Float,
        val speedAccuracy: Float?,
        val timestamp: Long,
        val provider: String,
        val confidence: Float,
        val isInterpolated: Boolean = false,
        val satelliteCount: Int? = null,
        val hdop: Float? = null, // Horizontal Dilution of Precision
        val vdop: Float? = null, // Vertical Dilution of Precision
        val magneticDeclination: Float? = null
    )

    data class SensorData(
        var accelerometer: FloatArray = FloatArray(3),
        var gyroscope: FloatArray = FloatArray(3),
        var magnetometer: FloatArray = FloatArray(3),
        var rotationMatrix: FloatArray = FloatArray(9),
        var orientation: FloatArray = FloatArray(3),
        var linearAcceleration: FloatArray = FloatArray(3)
    )

    private var currentSensorData = SensorData()

    fun startPrecisionTracking(mode: TrackingMode = TrackingMode.HIGH_ACCURACY): Flow<PrecisionLocation> = callbackFlow {
        // Check permissions first
        if (!hasLocationPermission()) {
            android.util.Log.e("PrecisionLocationManager", "Location permissions not granted")
            close(SecurityException("Location permissions not granted"))
            return@callbackFlow
        }

        // Start sensor monitoring
        startSensorMonitoring()

        // Configure location request based on mode
        val locationRequest = when (mode) {
            TrackingMode.HIGH_ACCURACY -> createHighAccuracyRequest()
            TrackingMode.BALANCED -> createBalancedRequest()
            TrackingMode.LOW_POWER -> createLowPowerRequest()
        }

        // GPS Provider
        val gpsListener = LocationListener { location ->
            processGpsLocation(location)
        }

        // Network Provider
        val networkListener = LocationListener { location ->
            processNetworkLocation(location)
        }

        // Fused Location Provider
        val fusedCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                processFusedLocation(result.lastLocation)
            }

            override fun onLocationAvailability(availability: LocationAvailability) {
                if (!availability.isLocationAvailable) {
                    handleLocationUnavailable()
                }
            }
        }

        // Start all location providers
        try {
            if (hasLocationPermission()) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    HIGH_ACCURACY_INTERVAL_MS,
                    1f,
                    gpsListener,
                    Looper.getMainLooper()
                )

                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    BALANCED_INTERVAL_MS,
                    10f,
                    networkListener,
                    Looper.getMainLooper()
                )

                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    fusedCallback,
                    Looper.getMainLooper()
                )

                // GNSS Measurements for Android N+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    registerGnssMeasurements()
                }
            } else {
                android.util.Log.e("PrecisionLocationManager", "Permission check failed during tracking start")
                close(SecurityException("Permission denied"))
            }

        } catch (e: SecurityException) {
            android.util.Log.e("PrecisionLocationManager", "SecurityException in startPrecisionTracking", e)
            close(e)
        } catch (e: Exception) {
            android.util.Log.e("PrecisionLocationManager", "Exception in startPrecisionTracking", e)
            close(e)
        }

        // Emit fused locations
        val emissionJob = CoroutineScope(Dispatchers.IO).launch {
            while (currentCoroutineContext().isActive) {
                delay(100) // Emit at 10Hz for smooth tracking

                val bestLocation = getBestLocation()
                bestLocation?.let {
                    trySend(it)
                }

                // Check for tunnel/poor GPS
                detectTunnelMode()

                // Apply dead reckoning if needed
                if (isInTunnel) {
                    val deadReckonedLocation = deadReckoning.estimatePosition(
                        lastKnownGoodLocation,
                        currentSensorData
                    )
                    deadReckonedLocation?.let {
                        trySend(it.copy(isInterpolated = true))
                    }
                }
            }
        }

        awaitClose {
            locationManager.removeUpdates(gpsListener)
            locationManager.removeUpdates(networkListener)
            fusedLocationClient.removeLocationUpdates(fusedCallback)
            stopSensorMonitoring()
            emissionJob.cancel()
        }
    }

    private fun createHighAccuracyRequest() = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        HIGH_ACCURACY_INTERVAL_MS
    ).apply {
        setMinUpdateIntervalMillis(500L)
        setMaxUpdateDelayMillis(1000L)
        setMinUpdateDistanceMeters(1f)
        setGranularity(Granularity.GRANULARITY_FINE)
        setWaitForAccurateLocation(true)
    }.build()

    private fun createBalancedRequest() = LocationRequest.Builder(
        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
        BALANCED_INTERVAL_MS
    ).apply {
        setMinUpdateIntervalMillis(1000L)
        setMaxUpdateDelayMillis(5000L)
        setMinUpdateDistanceMeters(5f)
        setGranularity(Granularity.GRANULARITY_FINE)
    }.build()

    private fun createLowPowerRequest() = LocationRequest.Builder(
        Priority.PRIORITY_LOW_POWER,
        10000L
    ).apply {
        setMinUpdateIntervalMillis(5000L)
        setMaxUpdateDelayMillis(30000L)
        setMinUpdateDistanceMeters(50f)
        setGranularity(Granularity.GRANULARITY_COARSE)
    }.build()

    private fun processGpsLocation(location: Location?) {
        location?.let {
            val precisionLocation = convertToPrecisionLocation(it, "GPS")
            locationBuffer.offer(precisionLocation)
            lastGpsFixTime = System.currentTimeMillis()
            lastKnownGoodLocation = precisionLocation
            isInTunnel = false

            // Apply Kalman filter
            val filtered = kalmanFilter.filter(precisionLocation)
            locationBuffer.offer(filtered)
        }
    }

    private fun processNetworkLocation(location: Location?) {
        location?.let {
            val precisionLocation = convertToPrecisionLocation(it, "Network")
            locationBuffer.offer(precisionLocation)
        }
    }

    private fun processFusedLocation(location: Location?) {
        location?.let {
            val precisionLocation = convertToPrecisionLocation(it, "Fused")
            locationBuffer.offer(precisionLocation)
        }
    }

    private fun convertToPrecisionLocation(location: Location, provider: String): PrecisionLocation {
        return PrecisionLocation(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = if (location.hasAltitude()) location.altitude else null,
            accuracy = location.accuracy,
            bearing = if (location.hasBearing()) location.bearing else estimateBearing(),
            bearingAccuracy = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && location.hasBearingAccuracy()) {
                location.bearingAccuracyDegrees
            } else null,
            speed = if (location.hasSpeed()) location.speed else estimateSpeed(),
            speedAccuracy = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && location.hasSpeedAccuracy()) {
                location.speedAccuracyMetersPerSecond
            } else null,
            timestamp = location.time,
            provider = provider,
            confidence = calculateConfidence(location),
            satelliteCount = location.extras?.getInt("satellites"),
            hdop = location.extras?.getFloat("hdop"),
            vdop = location.extras?.getFloat("vdop")
        )
    }

    private fun calculateConfidence(location: Location): Float {
        var confidence = 1.0f

        // Accuracy factor
        confidence *= when {
            location.accuracy <= 5 -> 1.0f
            location.accuracy <= 10 -> 0.9f
            location.accuracy <= 20 -> 0.7f
            location.accuracy <= 50 -> 0.5f
            else -> 0.3f
        }

        // Age factor
        val age = System.currentTimeMillis() - location.time
        confidence *= when {
            age <= 1000 -> 1.0f
            age <= 3000 -> 0.9f
            age <= 5000 -> 0.7f
            else -> 0.5f
        }

        // Speed consistency factor
        if (location.hasSpeed()) {
            val speedKmh = location.speed * 3.6
            confidence *= when {
                speedKmh <= 150 -> 1.0f // Reasonable vehicle speed
                speedKmh <= 200 -> 0.7f
                else -> 0.3f // Unrealistic speed
            }
        }

        return confidence.coerceIn(0f, 1f)
    }

    private fun getBestLocation(): PrecisionLocation? {
        val recentLocations = locationBuffer.filter {
            System.currentTimeMillis() - it.timestamp <= MAX_LOCATION_AGE_MS
        }

        if (recentLocations.isEmpty()) return lastKnownGoodLocation

        // Weighted fusion of multiple sources
        val gpsLocations = recentLocations.filter { it.provider == "GPS" }
        val networkLocations = recentLocations.filter { it.provider == "Network" }
        val fusedLocations = recentLocations.filter { it.provider == "Fused" }

        return when {
            gpsLocations.isNotEmpty() && gpsLocations.first().accuracy <= MIN_ACCURACY_METERS -> {
                gpsLocations.first()
            }
            fusedLocations.isNotEmpty() -> {
                fuseLocations(gpsLocations, networkLocations, fusedLocations)
            }
            else -> recentLocations.maxByOrNull { it.confidence }
        }
    }

    private fun fuseLocations(
        gpsLocations: List<PrecisionLocation>,
        networkLocations: List<PrecisionLocation>,
        fusedLocations: List<PrecisionLocation>
    ): PrecisionLocation {
        val weights = mutableMapOf<PrecisionLocation, Float>()

        gpsLocations.forEach { weights[it] = FUSION_WEIGHT_GPS * it.confidence }
        networkLocations.forEach { weights[it] = FUSION_WEIGHT_NETWORK * it.confidence }
        fusedLocations.forEach { weights[it] = (1f - FUSION_WEIGHT_GPS - FUSION_WEIGHT_NETWORK) * it.confidence }

        val totalWeight = weights.values.sum()

        var fusedLat = 0.0
        var fusedLng = 0.0
        var fusedAcc = 0f

        weights.forEach { (location, weight) ->
            val normalizedWeight = weight / totalWeight
            fusedLat += location.latitude * normalizedWeight
            fusedLng += location.longitude * normalizedWeight
            fusedAcc += location.accuracy * normalizedWeight
        }

        val bestBase = weights.maxBy { it.value }.key

        return bestBase.copy(
            latitude = fusedLat,
            longitude = fusedLng,
            accuracy = fusedAcc,
            confidence = totalWeight / weights.size
        )
    }

    private fun detectTunnelMode() {
        val timeSinceLastGps = System.currentTimeMillis() - lastGpsFixTime
        isInTunnel = timeSinceLastGps > TUNNEL_DETECTION_TIMEOUT_MS
    }

    private fun handleLocationUnavailable() {
        // Switch to dead reckoning
        isInTunnel = true
    }

    private fun startSensorMonitoring() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST)
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST)
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_FASTEST)
        sensorManager.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_FASTEST)
    }

    private fun stopSensorMonitoring() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                currentSensorData.accelerometer = event.values.clone()
                updateLinearAcceleration()
            }
            Sensor.TYPE_GYROSCOPE -> {
                currentSensorData.gyroscope = event.values.clone()
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                currentSensorData.magnetometer = event.values.clone()
                updateOrientation()
            }
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(
                    currentSensorData.rotationMatrix,
                    event.values
                )
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun updateOrientation() {
        if (currentSensorData.accelerometer.isNotEmpty() && currentSensorData.magnetometer.isNotEmpty()) {
            val success = SensorManager.getRotationMatrix(
                currentSensorData.rotationMatrix,
                null,
                currentSensorData.accelerometer,
                currentSensorData.magnetometer
            )

            if (success) {
                SensorManager.getOrientation(
                    currentSensorData.rotationMatrix,
                    currentSensorData.orientation
                )
            }
        }
    }

    private fun updateLinearAcceleration() {
        // Remove gravity from accelerometer readings
        val gravity = FloatArray(3)
        val alpha = 0.8f

        gravity[0] = alpha * gravity[0] + (1 - alpha) * currentSensorData.accelerometer[0]
        gravity[1] = alpha * gravity[1] + (1 - alpha) * currentSensorData.accelerometer[1]
        gravity[2] = alpha * gravity[2] + (1 - alpha) * currentSensorData.accelerometer[2]

        currentSensorData.linearAcceleration[0] = currentSensorData.accelerometer[0] - gravity[0]
        currentSensorData.linearAcceleration[1] = currentSensorData.accelerometer[1] - gravity[1]
        currentSensorData.linearAcceleration[2] = currentSensorData.accelerometer[2] - gravity[2]
    }

    private fun estimateBearing(): Float {
        // Use magnetometer and accelerometer to estimate bearing
        val azimuth = currentSensorData.orientation[0]
        return Math.toDegrees(azimuth.toDouble()).toFloat()
    }

    private fun estimateSpeed(): Float {
        // Use accelerometer to estimate speed changes
        val acceleration = sqrt(
            currentSensorData.linearAcceleration[0].pow(2) +
            currentSensorData.linearAcceleration[1].pow(2)
        )

        // This is a simplified estimation
        return lastKnownGoodLocation?.speed ?: 0f
    }

    private fun registerGnssMeasurements() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (!hasLocationPermission()) {
                android.util.Log.w("PrecisionLocationManager", "Cannot register GNSS measurements without permission")
                return
            }

            try {
                val callback = object : android.location.GnssMeasurementsEvent.Callback() {
                    override fun onGnssMeasurementsReceived(event: android.location.GnssMeasurementsEvent) {
                        // Process raw GNSS measurements for even more precision
                        processGnssMeasurements(event)
                    }
                }

                locationManager.registerGnssMeasurementsCallback(callback)
            } catch (e: SecurityException) {
                android.util.Log.e("PrecisionLocationManager", "SecurityException registering GNSS", e)
            } catch (e: Exception) {
                android.util.Log.e("PrecisionLocationManager", "Exception registering GNSS", e)
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return try {
            val hasFine = context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            val hasCoarse = context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            hasFine || hasCoarse
        } catch (e: Exception) {
            android.util.Log.e("PrecisionLocationManager", "Error checking permission", e)
            false
        }
    }

    private fun processGnssMeasurements(event: android.location.GnssMeasurementsEvent) {
        // Use raw pseudoranges and carrier phase for centimeter-level accuracy
        // This requires complex calculations but provides the highest precision
    }

    enum class TrackingMode {
        HIGH_ACCURACY,
        BALANCED,
        LOW_POWER
    }
}

// Extended Kalman Filter for location smoothing
class ExtendedKalmanFilter {
    private var x = DoubleArray(4) // State: [lat, lon, velocity_lat, velocity_lon]
    private var P = Array(4) { DoubleArray(4) } // Error covariance matrix
    private var Q = Array(4) { DoubleArray(4) } // Process noise
    private var R = Array(2) { DoubleArray(2) } // Measurement noise

    init {
        // Initialize matrices
        for (i in 0..3) {
            P[i][i] = 1000.0 // Large initial uncertainty
            Q[i][i] = if (i < 2) 0.1 else 1.0 // Process noise
        }
        R[0][0] = 5.0 // Measurement noise for position
        R[1][1] = 5.0
    }

    fun filter(location: PrecisionLocationManager.PrecisionLocation): PrecisionLocationManager.PrecisionLocation {
        // Prediction step
        predict()

        // Update step
        update(location.latitude, location.longitude, location.accuracy)

        return location.copy(
            latitude = x[0],
            longitude = x[1],
            confidence = min(1.0f, location.confidence * 1.2f) // Boost confidence after filtering
        )
    }

    private fun predict() {
        // State prediction
        val dt = 0.1 // Time step
        x[0] += x[2] * dt
        x[1] += x[3] * dt

        // Error covariance prediction
        for (i in 0..3) {
            for (j in 0..3) {
                P[i][j] += Q[i][j]
            }
        }
    }

    private fun update(measuredLat: Double, measuredLon: Double, accuracy: Float) {
        // Kalman gain
        val K = Array(4) { DoubleArray(2) }

        // Simplified Kalman gain calculation
        val S = Array(2) { DoubleArray(2) }
        S[0][0] = P[0][0] + R[0][0] * accuracy
        S[1][1] = P[1][1] + R[1][1] * accuracy

        K[0][0] = P[0][0] / S[0][0]
        K[1][1] = P[1][1] / S[1][1]

        // State update
        val innovation = doubleArrayOf(
            measuredLat - x[0],
            measuredLon - x[1]
        )

        x[0] += K[0][0] * innovation[0]
        x[1] += K[1][1] * innovation[1]

        // Error covariance update
        P[0][0] *= (1 - K[0][0])
        P[1][1] *= (1 - K[1][1])
    }
}

// Dead Reckoning for tunnel/poor GPS scenarios
class DeadReckoningEngine {
    private var lastPosition: PrecisionLocationManager.PrecisionLocation? = null
    private var lastUpdateTime = 0L

    fun estimatePosition(
        lastKnown: PrecisionLocationManager.PrecisionLocation?,
        sensorData: PrecisionLocationManager.SensorData
    ): PrecisionLocationManager.PrecisionLocation? {
        if (lastKnown == null) return null

        val currentTime = System.currentTimeMillis()
        val dt = (currentTime - lastUpdateTime) / 1000.0

        if (dt <= 0 || dt > 10) return lastKnown

        // Calculate displacement using sensors
        val displacement = calculateDisplacement(sensorData, dt)

        // Convert displacement to lat/lon change
        val latChange = displacement.north / 111111.0 // meters to degrees
        val lonChange = displacement.east / (111111.0 * cos(Math.toRadians(lastKnown.latitude)))

        lastUpdateTime = currentTime

        return lastKnown.copy(
            latitude = lastKnown.latitude + latChange,
            longitude = lastKnown.longitude + lonChange,
            timestamp = currentTime,
            confidence = lastKnown.confidence * 0.9f, // Reduce confidence
            isInterpolated = true
        )
    }

    private fun calculateDisplacement(
        sensorData: PrecisionLocationManager.SensorData,
        dt: Double
    ): Displacement {
        // Use accelerometer and gyroscope to estimate displacement
        val acceleration = sensorData.linearAcceleration

        // Simple double integration (real implementation would be more complex)
        val northDisplacement = 0.5 * acceleration[1] * dt * dt
        val eastDisplacement = 0.5 * acceleration[0] * dt * dt

        return Displacement(northDisplacement, eastDisplacement)
    }

    data class Displacement(val north: Double, val east: Double)
}