package com.daxido.user.presentation.ride

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daxido.core.models.DriverInfo
import com.daxido.core.models.RideStatus
import com.daxido.data.repository.RideRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CompleteRiderRideViewModel @Inject constructor(
    private val rideRepository: RideRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CompleteRiderRideUiState())
    val uiState: StateFlow<CompleteRiderRideUiState> = _uiState.asStateFlow()
    
    fun loadRideDetails(rideId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                rideStatus = RideStatus.DRIVER_ASSIGNED,
                driverInfo = DriverInfo("1", "Rajesh Kumar", "+91-9876543210", 4.8, 200, "KA01AB1234", "Toyota Innova"),
                pickupLocation = "MG Road, Bangalore",
                dropoffLocation = "Airport, Bangalore",
                estimatedArrival = "5 mins",
                fare = 450.0
            )
        }
    }
    
    fun updateRiderLocation(lat: Double, lng: Double) {
        viewModelScope.launch {
            // TODO: Update rider location
        }
    }
    
    fun updateRideStatus(status: RideStatus) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                rideStatus = status
            )
        }
    }
    
    fun rateDriver(rating: Float) {
        viewModelScope.launch {
            // TODO: Rate driver
        }
    }
}

data class CompleteRiderRideUiState(
    val isLoading: Boolean = true,
    val rideStatus: RideStatus = RideStatus.SEARCHING,
    val driverInfo: DriverInfo? = null,
    val pickupLocation: String = "",
    val dropoffLocation: String = "",
    val estimatedArrival: String = "",
    val fare: Double = 0.0
)
