package com.daxido.core.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.daxido.MainActivity
import com.daxido.R
import com.google.android.gms.location.*
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.*

class LocationTrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var currentRideId: String? = null
    private var isDriver: Boolean = false

    companion object {
        const val ACTION_START_TRACKING = "START_TRACKING"
        const val ACTION_STOP_TRACKING = "STOP_TRACKING"
        const val EXTRA_RIDE_ID = "RIDE_ID"
        const val EXTRA_IS_DRIVER = "IS_DRIVER"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "location_tracking"
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupLocationCallback()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TRACKING -> {
                currentRideId = intent.getStringExtra(EXTRA_RIDE_ID)
                isDriver = intent.getBooleanExtra(EXTRA_IS_DRIVER, false)
                startLocationTracking()
            }
            ACTION_STOP_TRACKING -> {
                stopLocationTracking()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    updateLocationInFirebase(location)
                }
            }
        }
    }

    private fun startLocationTracking() {
        try {
            // Check permissions before starting foreground service
            val hasFineLocation = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            val hasCoarseLocation = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasFineLocation && !hasCoarseLocation) {
                android.util.Log.e("LocationTrackingService", "Location permissions not granted")
                stopSelf()
                return
            }

            // Start foreground service
            startForeground(NOTIFICATION_ID, createNotification())

            // PERFORMANCE FIX: Reduced update frequency from 5s to 10s to save battery
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                10000L // Update every 10 seconds (reduced from 5s for better battery life)
            ).apply {
                setMinUpdateIntervalMillis(5000L) // Reduced from 2s
                setMaxUpdateDelayMillis(15000L) // Increased from 10s
            }.build()

            // Request location updates with explicit permission check
            if (hasFineLocation || hasCoarseLocation) {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                ).addOnSuccessListener {
                    android.util.Log.d("LocationTrackingService", "Location tracking started successfully")
                }.addOnFailureListener { e ->
                    android.util.Log.e("LocationTrackingService", "Failed to start location tracking", e)
                    stopSelf()
                }
            } else {
                android.util.Log.e("LocationTrackingService", "No location permission available")
                stopSelf()
            }
        } catch (e: SecurityException) {
            android.util.Log.e("LocationTrackingService", "SecurityException in startLocationTracking", e)
            stopSelf()
        } catch (e: Exception) {
            android.util.Log.e("LocationTrackingService", "Exception in startLocationTracking", e)
            stopSelf()
        }
    }

    private fun stopLocationTracking() {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (e: Exception) {
            android.util.Log.e("LocationTrackingService", "Error removing location updates", e)
        }
        // MEMORY LEAK FIX: Ensure serviceScope is cancelled in all paths
        if (serviceScope.isActive) {
            serviceScope.cancel()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateLocationInFirebase(location: Location) {
        currentRideId?.let { rideId ->
            serviceScope.launch {
                try {
                    val database = FirebaseDatabase.getInstance()
                    val locationRef = if (isDriver) {
                        database.getReference("rides/$rideId/driver_location")
                    } else {
                        database.getReference("rides/$rideId/user_location")
                    }

                    val locationData = hashMapOf(
                        "latitude" to location.latitude,
                        "longitude" to location.longitude,
                        "speed" to location.speed,
                        "bearing" to location.bearing,
                        "accuracy" to location.accuracy,
                        "timestamp" to System.currentTimeMillis()
                    )

                    locationRef.setValue(locationData)

                    // Update driver availability if driver
                    if (isDriver) {
                        database.getReference("drivers_available/${getUserId()}")
                            .setValue(locationData)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Daxido - Tracking Active")
            .setContentText("Your location is being tracked for the current ride")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when location is being tracked during a ride"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun getUserId(): String {
        // SECURITY FIX: This should use EncryptedSharedPreferences (via UserPreferences injectable)
        // For now, keeping minimal access - TODO: Refactor to inject UserPreferences
        return try {
            getSharedPreferences("daxido_prefs", Context.MODE_PRIVATE)
                .getString("user_id", "") ?: ""
        } catch (e: Exception) {
            android.util.Log.e("LocationTrackingService", "Failed to get user ID", e)
            ""
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // MEMORY LEAK FIX: Ensure proper cleanup in onDestroy
        stopLocationTracking()
    }
}