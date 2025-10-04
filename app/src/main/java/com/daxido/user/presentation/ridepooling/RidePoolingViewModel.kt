package com.daxido.user.presentation.ridepooling

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daxido.core.ridepooling.RidePoolingService
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RidePoolingViewModel @Inject constructor(
    private val ridePoolingService: RidePoolingService
) : ViewModel() {

    private val _uiState = MutableStateFlow(RidePoolingUiState())
    val uiState: StateFlow<RidePoolingUiState> = _uiState.asStateFlow()

    init {
        loadAvailablePools()
        loadMyPools()
    }

    fun loadAvailablePools() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Mock current location - in real app, get from LocationService
            val currentLocation = LatLng(12.9716, 77.5946) // Bangalore

            ridePoolingService.getAvailablePoolRides(currentLocation, 5.0).collect { pools ->
                val poolInfos = pools.map { pool ->
                    PoolRideInfo(
                        id = pool.id,
                        vehicleType = pool.vehicleType,
                        maxPassengers = pool.maxPassengers,
                        currentPassengers = pool.currentPassengers,
                        sharedFare = pool.sharedFare,
                        savings = pool.baseFare - pool.sharedFare,
                        departureTime = "5 min",
                        status = pool.status.name
                    )
                }
                _uiState.value = _uiState.value.copy(
                    availablePools = poolInfos,
                    isLoading = false
                )
            }
        }
    }

    fun loadMyPools() {
        viewModelScope.launch {
            // Mock user ID - in real app, get from auth service
            val userId = "user123"

            // In a real implementation, you'd have a method to get user's pools
            // For now, we'll keep it empty
            _uiState.value = _uiState.value.copy(myPools = emptyList())
        }
    }

    fun joinPool(poolId: String) {
        viewModelScope.launch {
            val userId = "user123"
            val userName = "John Doe"
            val pickupLocation = LatLng(12.9716, 77.5946)
            val dropoffLocation = LatLng(12.9350, 77.6244)

            val result = ridePoolingService.addPassengerToPool(
                rideId = poolId,
                passengerId = userId,
                pickupLocation = pickupLocation,
                dropoffLocation = dropoffLocation,
                passengerName = userName
            )

            // Reload pools after joining
            loadAvailablePools()
            loadMyPools()
        }
    }

    fun leavePool(poolId: String) {
        viewModelScope.launch {
            val userId = "user123"

            val result = ridePoolingService.removePassengerFromPool(
                rideId = poolId,
                passengerId = userId
            )

            // Reload pools after leaving
            loadAvailablePools()
            loadMyPools()
        }
    }

    fun createNewPool() {
        viewModelScope.launch {
            val rideId = "pool_${System.currentTimeMillis()}"
            val driverId = "driver123"
            val vehicleType = "Sedan"
            val pickupLocation = LatLng(12.9716, 77.5946)
            val dropoffLocation = LatLng(12.9350, 77.6244)
            val baseFare = 300.0

            val result = ridePoolingService.createPoolRide(
                rideId = rideId,
                driverId = driverId,
                vehicleType = vehicleType,
                maxPassengers = 4,
                pickupLocation = pickupLocation,
                dropoffLocation = dropoffLocation,
                baseFare = baseFare
            )

            // Reload pools after creating
            loadAvailablePools()
        }
    }
}