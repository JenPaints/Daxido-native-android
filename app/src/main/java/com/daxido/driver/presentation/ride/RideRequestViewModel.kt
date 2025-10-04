package com.daxido.driver.presentation.ride

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daxido.core.models.RideStatus
import com.daxido.core.models.CustomerInfo
import com.daxido.data.repository.RideRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RideRequestViewModel @Inject constructor(
    private val rideRepository: RideRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(RideRequestUiState())
    val uiState: StateFlow<RideRequestUiState> = _uiState.asStateFlow()
    
    fun loadRideRequest(rideId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                rideStatus = RideStatus.SEARCHING,
                distance = 5.2,
                estimatedTime = 12,
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
    
    fun acceptRide(requestId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true
            )
            // Handle ride acceptance logic
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isAccepted = true
            )
        }
    }
    
    fun declineRide(requestId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true
            )
            // Handle ride decline logic
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isDeclined = true
            )
        }
    }
}

data class RideRequestUiState(
    val isLoading: Boolean = true,
    val rideStatus: RideStatus = RideStatus.SEARCHING,
    val distance: Double = 0.0,
    val estimatedTime: Int = 0,
    val fare: Double = 0.0,
    val isAccepted: Boolean = false,
    val isDeclined: Boolean = false,
    val customerInfo: CustomerInfo? = null,
    val pickupLocation: String = "",
    val dropoffLocation: String = ""
)
