package com.daxido.driver.presentation.ride

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daxido.core.models.CustomerInfo
import com.daxido.core.models.RideStatus
import com.daxido.data.repository.RideRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CompleteDriverRideViewModel @Inject constructor(
    private val rideRepository: RideRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CompleteDriverRideUiState())
    val uiState: StateFlow<CompleteDriverRideUiState> = _uiState.asStateFlow()
    
    fun loadRideDetails(rideId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                rideStatus = RideStatus.DRIVER_ASSIGNED,
                customerInfo = CustomerInfo("1", "John Doe", "+91-9876543210", 4.5, 150),
                pickupLocation = "MG Road, Bangalore",
                dropoffLocation = "Airport, Bangalore",
                estimatedTime = 15,
                fare = 450.0
            )
        }
    }
    
    fun updateDriverLocation(lat: Double, lng: Double) {
        viewModelScope.launch {
            // TODO: Update driver location
        }
    }
    
    fun updateRideStatus(status: RideStatus) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                rideStatus = status
            )
        }
    }
    
    fun startRide() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                rideStatus = RideStatus.TRIP_STARTED
            )
        }
    }
    
    fun completeRide() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                rideStatus = RideStatus.COMPLETED
            )
        }
    }
}

data class CompleteDriverRideUiState(
    val isLoading: Boolean = true,
    val rideStatus: RideStatus = RideStatus.SEARCHING,
    val customerInfo: CustomerInfo? = null,
    val pickupLocation: String = "",
    val dropoffLocation: String = "",
    val estimatedTime: Int = 0,
    val fare: Double = 0.0
)
