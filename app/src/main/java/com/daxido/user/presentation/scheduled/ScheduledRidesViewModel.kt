package com.daxido.user.presentation.scheduled

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daxido.core.scheduling.ScheduledRidesService
import com.daxido.core.scheduling.DayOfWeek
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ScheduledRidesViewModel @Inject constructor(
    private val scheduledRidesService: ScheduledRidesService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduledRidesUiState())
    val uiState: StateFlow<ScheduledRidesUiState> = _uiState.asStateFlow()

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    init {
        loadScheduledRides()
        loadRecurringRides()
    }

    fun loadScheduledRides() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val userId = "user123" // Mock user ID

            scheduledRidesService.getUserScheduledRides(userId).collect { rides ->
                val currentTime = System.currentTimeMillis()

                val upcomingRides = rides
                    .filter { it.scheduledTime >= currentTime }
                    .map { ride ->
                        ScheduledRideInfo(
                            id = ride.id,
                            pickupAddress = "Pickup Location",
                            dropoffAddress = "Dropoff Location",
                            scheduledDate = dateFormat.format(Date(ride.scheduledTime)),
                            scheduledTime = timeFormat.format(Date(ride.scheduledTime)),
                            vehicleType = ride.vehicleType,
                            estimatedFare = ride.estimatedFare
                        )
                    }

                val pastRides = rides
                    .filter { it.scheduledTime < currentTime }
                    .map { ride ->
                        ScheduledRideInfo(
                            id = ride.id,
                            pickupAddress = "Pickup Location",
                            dropoffAddress = "Dropoff Location",
                            scheduledDate = dateFormat.format(Date(ride.scheduledTime)),
                            scheduledTime = timeFormat.format(Date(ride.scheduledTime)),
                            vehicleType = ride.vehicleType,
                            estimatedFare = ride.estimatedFare
                        )
                    }

                _uiState.value = _uiState.value.copy(
                    upcomingRides = upcomingRides,
                    pastRides = pastRides,
                    isLoading = false
                )
            }
        }
    }

    fun loadRecurringRides() {
        viewModelScope.launch {
            val userId = "user123" // Mock user ID

            scheduledRidesService.getUserRecurringRides(userId).collect { rides ->
                val recurringRides = rides.map { ride ->
                    RecurringRideInfo(
                        id = ride.id,
                        pickupAddress = "Pickup Location",
                        dropoffAddress = "Dropoff Location",
                        time = ride.time,
                        days = ride.days.map { it.name.take(3) },
                        vehicleType = ride.vehicleType,
                        estimatedFare = ride.estimatedFare,
                        isActive = ride.status.name == "ACTIVE"
                    )
                }

                _uiState.value = _uiState.value.copy(recurringRides = recurringRides)
            }
        }
    }

    fun scheduleRide(
        pickupLocation: LatLng,
        dropoffLocation: LatLng,
        scheduledTime: Long,
        vehicleType: String,
        estimatedFare: Double
    ) {
        viewModelScope.launch {
            val rideId = "scheduled_${System.currentTimeMillis()}"
            val userId = "user123"

            val result = scheduledRidesService.scheduleRide(
                rideId = rideId,
                userId = userId,
                pickupLocation = pickupLocation,
                dropoffLocation = dropoffLocation,
                scheduledTime = scheduledTime,
                vehicleType = vehicleType,
                estimatedFare = estimatedFare,
                notes = null
            )

            loadScheduledRides()
        }
    }

    fun createRecurringRide(
        pickupLocation: LatLng,
        dropoffLocation: LatLng,
        time: String,
        days: List<DayOfWeek>,
        vehicleType: String,
        estimatedFare: Double
    ) {
        viewModelScope.launch {
            val recurringId = "recurring_${System.currentTimeMillis()}"
            val userId = "user123"
            val startDate = System.currentTimeMillis()

            val result = scheduledRidesService.createRecurringRide(
                recurringId = recurringId,
                userId = userId,
                pickupLocation = pickupLocation,
                dropoffLocation = dropoffLocation,
                time = time,
                days = days,
                vehicleType = vehicleType,
                estimatedFare = estimatedFare,
                startDate = startDate,
                endDate = null,
                notes = null
            )

            loadRecurringRides()
        }
    }

    fun cancelScheduledRide(rideId: String) {
        viewModelScope.launch {
            val result = scheduledRidesService.cancelScheduledRide(rideId)
            loadScheduledRides()
        }
    }

    fun cancelRecurringRide(recurringId: String) {
        viewModelScope.launch {
            val result = scheduledRidesService.cancelRecurringRide(recurringId)
            loadRecurringRides()
        }
    }
}