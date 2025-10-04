package com.daxido.user.presentation.ride

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daxido.core.models.*
import com.daxido.data.repository.RideRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RideTrackingViewModel @Inject constructor(
    private val rideRepository: RideRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RideTrackingUiState())
    val uiState: StateFlow<RideTrackingUiState> = _uiState.asStateFlow()

    fun loadRideDetails(rideId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            rideRepository.getRideById(rideId)
                .fold(
                    onSuccess = { ride ->
                        _uiState.update { 
                            it.copy(
                                ride = ride,
                                isLoading = false
                            ) 
                        }
                    },
                    onFailure = { error ->
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                errorMessage = error.message
                            ) 
                        }
                    }
                )
        }
    }

    fun startTracking() {
        viewModelScope.launch {
            val ride = _uiState.value.ride
            if (ride != null && ride.driverId != null) {
                // Start real-time tracking
                rideRepository.startRideTracking(ride.id, ride.driverId!!)
                    .collect { trackingUpdate ->
                        _uiState.update {
                            it.copy(
                                driverLocation = trackingUpdate.driverLocation,
                                estimatedTime = trackingUpdate.estimatedTimeRemaining,
                                routePolyline = trackingUpdate.routePolyline
                            )
                        }
                    }
            }
        }
    }

    fun callDriver() {
        viewModelScope.launch {
            val driver = _uiState.value.driver
            if (driver != null) {
                // Implement calling functionality
                // This would typically open the phone dialer
            }
        }
    }

    fun shareTrip() {
        viewModelScope.launch {
            val ride = _uiState.value.ride
            if (ride != null) {
                // Implement sharing functionality
                // This would typically open the share dialog
            }
        }
    }

    fun triggerSOS() {
        viewModelScope.launch {
            val ride = _uiState.value.ride
            if (ride != null) {
                rideRepository.triggerSOS(ride.id)
                    .fold(
                        onSuccess = {
                            _uiState.update { 
                                it.copy(
                                    sosTriggered = true,
                                    errorMessage = "SOS alert sent to emergency contacts"
                                ) 
                            }
                        },
                        onFailure = { error ->
                            _uiState.update { 
                                it.copy(
                                    errorMessage = "Failed to send SOS: ${error.message}"
                                ) 
                            }
                        }
                    )
            }
        }
    }

    fun cancelRide() {
        viewModelScope.launch {
            val ride = _uiState.value.ride
            if (ride != null) {
                rideRepository.cancelRide(ride.id, "User cancelled")
                    .fold(
                        onSuccess = {
                            _uiState.update { 
                                it.copy(
                                    ride = ride.copy(status = RideStatus.CANCELLED),
                                    errorMessage = "Ride cancelled successfully"
                                ) 
                            }
                        },
                        onFailure = { error ->
                            _uiState.update { 
                                it.copy(
                                    errorMessage = "Failed to cancel ride: ${error.message}"
                                ) 
                            }
                        }
                    )
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    fun startRideTracking(rideId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                // Load ride details first
                loadRideDetails(rideId)

                // Start real-time tracking
                val ride = _uiState.value.ride
                if (ride != null) {
                    startTracking()
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to start tracking: ${e.message}"
                    )
                }
            }
        }
    }

    fun updateRideStatus(newStatus: RideStatus) {
        viewModelScope.launch {
            val currentRide = _uiState.value.ride ?: return@launch

            try {
                // TODO: Implement updateRideStatus method in RideRepository
                // For now, just update the local state
                _uiState.update {
                    it.copy(
                        ride = currentRide.copy(status = newStatus),
                        rideStatus = newStatus
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Failed to update ride status: ${e.message}")
                }
            }
        }
    }

    fun updateUserLocation(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            val currentRide = _uiState.value.ride ?: return@launch

            try {
                val location = Location(latitude, longitude, "Current Location")
                // TODO: Implement updateUserLocation method in RideRepository
                // For now, just log the location update
                android.util.Log.d("RideTrackingViewModel", "Location updated: $latitude, $longitude")
            } catch (e: Exception) {
                // Log error but don't show to user (non-critical)
                android.util.Log.e("RideTrackingViewModel", "Failed to update location", e)
            }
        }
    }

    fun triggerEmergency() {
        viewModelScope.launch {
            val currentRide = _uiState.value.ride ?: return@launch

            try {
                _uiState.update { it.copy(sosTriggered = true) }

                // Use existing triggerSOS method
                val result = rideRepository.triggerSOS(currentRide.id)
                result.fold(
                    onSuccess = {
                        _uiState.update {
                            it.copy(errorMessage = "Emergency alert sent successfully")
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                sosTriggered = false,
                                errorMessage = "Failed to send emergency alert: ${error.message}"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        sosTriggered = false,
                        errorMessage = "Emergency alert failed: ${e.message}"
                    )
                }
            }
        }
    }
}

data class RideTrackingUiState(
    val ride: Ride? = null,
    val driver: Driver? = null,
    val driverLocation: Location? = null,
    val estimatedTime: Int? = null,
    val routePolyline: String? = null,
    val isLoading: Boolean = false,
    val sosTriggered: Boolean = false,
    val errorMessage: String? = null,
    val rideStatus: RideStatus = RideStatus.SEARCHING,
    val driverInfo: DriverInfo? = null,
    val pickupLocation: String = "",
    val dropoffLocation: String = "",
    val estimatedFare: Double = 0.0
)
