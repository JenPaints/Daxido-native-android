package com.daxido.driver.presentation.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daxido.core.models.RecentRide
import com.daxido.core.models.RideStatus
import com.daxido.core.models.VehicleType
import com.daxido.data.repository.RideRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class DriverHomeViewModel @Inject constructor(
    private val rideRepository: RideRepository,
    private val firestore: FirebaseFirestore,
    private val realtimeDb: FirebaseDatabase,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val TAG = "DriverHomeViewModel"
    private val _uiState = MutableStateFlow(DriverHomeUiState())
    val uiState: StateFlow<DriverHomeUiState> = _uiState.asStateFlow()

    private var rideRequestListener: ValueEventListener? = null

    init {
        loadDriverData()
        startListeningForRideRequests()
    }

    private fun loadDriverData() {
        viewModelScope.launch {
            val driverId = auth.currentUser?.uid
            if (driverId == null) {
                Log.e(TAG, "Driver not authenticated")
                return@launch
            }

            try {
                // Get driver document from Firestore
                val driverDoc = firestore.collection("drivers")
                    .document(driverId)
                    .get()
                    .await()

                val todaysEarnings = driverDoc.getDouble("todayEarnings") ?: 0.0
                val todaysRides = (driverDoc.getLong("todaysRides") ?: 0L).toInt()
                val rating = (driverDoc.getDouble("rating") ?: 5.0).toFloat()
                val onlineHours = driverDoc.getDouble("todaysHours") ?: 0.0
                val isOnline = driverDoc.getBoolean("isOnline") ?: false
                val currentLocation = driverDoc.getString("currentLocation") ?: "Unknown"

                // Get recent rides
                val ridesSnapshot = firestore.collection("rides")
                    .whereEqualTo("driverId", driverId)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(10)
                    .get()
                    .await()

                val recentRides = ridesSnapshot.documents.mapNotNull { doc ->
                    try {
                        val status = when (doc.getString("status")) {
                            "COMPLETED" -> RideStatus.COMPLETED
                            "CANCELLED" -> RideStatus.CANCELLED
                            "TRIP_STARTED" -> RideStatus.TRIP_STARTED
                            else -> RideStatus.COMPLETED
                        }

                        val vehicleType = when (doc.getString("vehicleType")) {
                            "CAR" -> VehicleType.CAR
                            "BIKE" -> VehicleType.BIKE
                            "AUTO" -> VehicleType.AUTO
                            else -> VehicleType.CAR
                        }

                        RecentRide(
                            id = doc.id,
                            pickupLocation = doc.getString("pickupLocation") ?: "",
                            dropoffLocation = doc.getString("dropoffLocation") ?: "",
                            fare = doc.getDouble("fare") ?: 0.0,
                            date = doc.getDate("createdAt")?.time ?: System.currentTimeMillis(),
                            status = status,
                            vehicleType = vehicleType,
                            rating = doc.getDouble("rating") ?: 0.0
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing ride", e)
                        null
                    }
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isOnline = isOnline,
                    currentLocation = currentLocation,
                    todaysEarnings = todaysEarnings,
                    todaysRides = todaysRides,
                    currentRating = rating,
                    onlineHours = onlineHours,
                    recentRides = recentRides
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error loading driver data", e)
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private fun startListeningForRideRequests() {
        val driverId = auth.currentUser?.uid ?: return

        // Listen to driver notifications for incoming ride requests
        val notificationsRef = realtimeDb.getReference("driver_notifications/$driverId")

        rideRequestListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val rideRequestId = snapshot.child("rideRequestId").getValue(String::class.java)
                    val pickupLocation = snapshot.child("pickupLocation").getValue(String::class.java) ?: ""
                    val dropoffLocation = snapshot.child("dropoffLocation").getValue(String::class.java) ?: ""
                    val fare = snapshot.child("fare").getValue(Double::class.java) ?: 0.0
                    val distance = snapshot.child("distance").getValue(Int::class.java) ?: 0
                    val vehicleType = snapshot.child("vehicleType").getValue(String::class.java) ?: "CAR"

                    if (rideRequestId != null && _uiState.value.isOnline) {
                        val rideRequest = RideRequest(
                            rideId = rideRequestId,
                            pickupLocation = pickupLocation,
                            dropoffLocation = dropoffLocation,
                            fare = fare,
                            distance = distance,
                            vehicleType = vehicleType
                        )

                        _uiState.value = _uiState.value.copy(currentRideRequest = rideRequest)
                        Log.d(TAG, "New ride request received: $rideRequestId")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing ride request", e)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Ride request listener cancelled", error.toException())
            }
        }

        notificationsRef.addValueEventListener(rideRequestListener!!)
        Log.d(TAG, "Started listening for ride requests")
    }

    fun updateLocation(lat: Double, lng: Double) {
        viewModelScope.launch {
            val driverId = auth.currentUser?.uid ?: return@launch

            try {
                // Update location in Realtime Database
                val locationData = mapOf(
                    "latitude" to lat,
                    "longitude" to lng,
                    "timestamp" to System.currentTimeMillis()
                )

                realtimeDb.getReference("driver_locations/$driverId")
                    .setValue(locationData)
                    .await()

                // Update availability in drivers_available
                if (_uiState.value.isOnline) {
                    realtimeDb.getReference("drivers_available/$driverId")
                        .setValue(
                            mapOf(
                                "isAvailable" to true,
                                "latitude" to lat,
                                "longitude" to lng,
                                "timestamp" to System.currentTimeMillis()
                            )
                        )
                        .await()
                }

                Log.d(TAG, "Location updated successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating location", e)
            }
        }
    }

    fun checkForRideRequests() {
        viewModelScope.launch {
            val driverId = auth.currentUser?.uid ?: return@launch

            try {
                // Check for pending ride requests in Firestore
                val snapshot = firestore.collection("ride_requests")
                    .whereEqualTo("status", "SEARCHING")
                    .whereEqualTo("assignedDriverId", driverId)
                    .limit(1)
                    .get()
                    .await()

                if (!snapshot.isEmpty) {
                    val doc = snapshot.documents.first()
                    val rideRequest = RideRequest(
                        rideId = doc.id,
                        pickupLocation = doc.getString("pickupLocation") ?: "",
                        dropoffLocation = doc.getString("dropoffLocation") ?: "",
                        fare = doc.getDouble("fare") ?: 0.0,
                        distance = (doc.getLong("distance") ?: 0L).toInt(),
                        vehicleType = doc.getString("vehicleType") ?: "CAR"
                    )

                    _uiState.value = _uiState.value.copy(currentRideRequest = rideRequest)
                    Log.d(TAG, "Found pending ride request: ${doc.id}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking for ride requests", e)
            }
        }
    }

    fun acceptRideRequest(rideId: String) {
        viewModelScope.launch {
            val driverId = auth.currentUser?.uid ?: return@launch

            try {
                val result = rideRepository.acceptRide(rideId, driverId)

                if (result.isSuccess) {
                    // Clear current ride request
                    _uiState.value = _uiState.value.copy(currentRideRequest = null)

                    // Clear notification
                    realtimeDb.getReference("driver_notifications/$driverId").removeValue().await()

                    Log.d(TAG, "Ride accepted successfully")
                } else {
                    Log.e(TAG, "Failed to accept ride: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error accepting ride request", e)
            }
        }
    }

    fun rejectRideRequest(rideId: String) {
        viewModelScope.launch {
            val driverId = auth.currentUser?.uid ?: return@launch

            try {
                // Update ride request status
                firestore.collection("ride_requests")
                    .document(rideId)
                    .update("rejectedBy", com.google.firebase.firestore.FieldValue.arrayUnion(driverId))
                    .await()

                // Clear current ride request
                _uiState.value = _uiState.value.copy(currentRideRequest = null)

                // Clear notification
                realtimeDb.getReference("driver_notifications/$driverId").removeValue().await()

                Log.d(TAG, "Ride rejected successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error rejecting ride request", e)
            }
        }
    }

    fun toggleOnlineStatus() {
        viewModelScope.launch {
            val driverId = auth.currentUser?.uid ?: return@launch
            val newStatus = !_uiState.value.isOnline

            try {
                // Update driver's online status in Firestore
                firestore.collection("drivers")
                    .document(driverId)
                    .update("isOnline", newStatus)
                    .await()

                // Update availability in Realtime Database
                if (newStatus) {
                    realtimeDb.getReference("drivers_available/$driverId")
                        .setValue(
                            mapOf(
                                "isAvailable" to true,
                                "timestamp" to System.currentTimeMillis()
                            )
                        )
                        .await()
                } else {
                    // Remove from available drivers when going offline
                    realtimeDb.getReference("drivers_available/$driverId").removeValue().await()
                }

                _uiState.value = _uiState.value.copy(isOnline = newStatus)

                Log.d(TAG, "Online status updated to: $newStatus")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating online status", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Remove listener when ViewModel is cleared
        rideRequestListener?.let {
            val driverId = auth.currentUser?.uid
            if (driverId != null) {
                realtimeDb.getReference("driver_notifications/$driverId")
                    .removeEventListener(it)
            }
        }
    }
}

data class DriverHomeUiState(
    val isLoading: Boolean = true,
    val isOnline: Boolean = false,
    val currentLocation: String = "",
    val todaysEarnings: Double = 0.0,
    val todaysRides: Int = 0,
    val currentRating: Float = 0.0f,
    val onlineHours: Double = 0.0,
    val currentRideRequest: RideRequest? = null,
    val recentRides: List<RecentRide> = emptyList()
)

data class RideRequest(
    val rideId: String,
    val pickupLocation: String,
    val dropoffLocation: String,
    val fare: Double,
    val distance: Int,
    val vehicleType: String
)
