package com.daxido.user.presentation.multistop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daxido.core.multistop.MultiStopRidesService
import com.daxido.core.multistop.RideStop
import com.daxido.core.multistop.StopType
import com.daxido.core.multistop.StopStatus
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MultiStopRideViewModel @Inject constructor(
    private val multiStopRidesService: MultiStopRidesService
) : ViewModel() {

    private val _uiState = MutableStateFlow(MultiStopRideUiState())
    val uiState: StateFlow<MultiStopRideUiState> = _uiState.asStateFlow()

    private val baseFare = 200.0 // Base fare for the ride

    fun addStop(address: String, stopType: StopType, notes: String?) {
        val stopId = "stop_${System.currentTimeMillis()}"
        // Mock location - in real app, geocode the address
        val location = LatLng(12.9716, 77.5946)

        val newStop = StopInfo(
            id = stopId,
            address = address,
            stopType = stopType,
            notes = notes,
            estimatedTime = "${(10..30).random()} min"
        )

        val updatedStops = _uiState.value.stops + newStop
        updateFare(updatedStops)
    }

    fun removeStop(stopId: String) {
        val updatedStops = _uiState.value.stops.filter { it.id != stopId }
        updateFare(updatedStops)
    }

    private fun updateFare(stops: List<StopInfo>) {
        val stopCount = stops.size
        val totalFare = if (stopCount == 0) {
            0.0
        } else if (stopCount == 1) {
            baseFare
        } else {
            // Multi-stop discount calculation
            val additionalStopDiscount = 0.1 // 10% discount per additional stop
            val additionalStops = stopCount - 1
            val discount = additionalStops * additionalStopDiscount
            baseFare * (1.0 - discount.coerceAtMost(0.3)) // Max 30% discount
        }

        val regularFare = baseFare * stopCount
        val savings = regularFare - totalFare

        _uiState.value = _uiState.value.copy(
            stops = stops,
            totalFare = totalFare,
            savings = savings.coerceAtLeast(0.0)
        )
    }

    fun createMultiStopRide() {
        viewModelScope.launch {
            val rideId = "multistop_${System.currentTimeMillis()}"
            val userId = "user123"
            val driverId = "driver123"
            val vehicleType = "Sedan"

            val rideStops = _uiState.value.stops.map { stop ->
                RideStop(
                    id = stop.id,
                    location = LatLng(12.9716, 77.5946), // Mock location
                    address = stop.address,
                    stopType = stop.stopType,
                    notes = stop.notes,
                    status = StopStatus.PENDING
                )
            }

            val result = multiStopRidesService.createMultiStopRide(
                rideId = rideId,
                userId = userId,
                driverId = driverId,
                stops = rideStops,
                vehicleType = vehicleType,
                baseFare = baseFare
            )

            // Handle result
        }
    }
}