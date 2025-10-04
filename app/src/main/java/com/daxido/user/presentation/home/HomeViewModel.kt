package com.daxido.user.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daxido.core.models.*
import com.daxido.data.repository.RideRepository
import com.daxido.core.algorithms.DriverAllocationEngine
import com.daxido.core.maps.DirectionsService
import com.daxido.core.tracking.RealTimeTrackingManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val rideRepository: RideRepository,
    private val driverAllocationEngine: DriverAllocationEngine,
    private val directionsService: DirectionsService,
    private val trackingManager: RealTimeTrackingManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<HomeEvent>()
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    fun setPickupLocation(location: Location) {
        _uiState.update { it.copy(pickupLocation = location) }
        calculateFareEstimate()
    }

    fun setDropLocation(location: Location) {
        _uiState.update { it.copy(dropLocation = location) }
        calculateFareEstimate()
        fetchRoutePolyline()
    }

    fun selectVehicleType(type: VehicleType) {
        _uiState.update { it.copy(selectedVehicleType = type) }
        calculateFareEstimate()
    }

    fun startLocationTracking() {
        // Location tracking will be handled in the UI layer
        // This method is called when permissions are granted
        searchNearbyDrivers()
    }

    fun updateCurrentLocation(location: com.daxido.core.location.LocationData) {
        viewModelScope.launch {
            // Update pickup location if not set
            if (_uiState.value.pickupLocation == null) {
                val locationObj = Location(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    address = "Current Location"
                )
                _uiState.update { it.copy(pickupLocation = locationObj) }
            }

            // Search for nearby drivers with updated location
            searchNearbyDrivers()
        }
    }

    fun searchNearbyDrivers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearchingDrivers = true) }

            val pickupLocation = _uiState.value.pickupLocation
            val vehicleType = _uiState.value.selectedVehicleType

            if (pickupLocation != null && vehicleType != null) {
                val drivers = rideRepository.getNearbyDrivers(pickupLocation, vehicleType)
                _uiState.update {
                    it.copy(
                        nearbyDrivers = drivers,
                        isSearchingDrivers = false
                    )
                }
            } else {
                _uiState.update { it.copy(isSearchingDrivers = false) }
            }
        }
    }

    fun createRideRequest() {
        viewModelScope.launch {
            val state = _uiState.value
            val pickup = state.pickupLocation
            val drop = state.dropLocation
            val vehicleType = state.selectedVehicleType
            val paymentMethod = state.selectedPaymentMethod

            if (pickup != null && drop != null && vehicleType != null && paymentMethod != null) {
                _uiState.update { it.copy(isBookingRide = true, allocationStatus = "Finding drivers...") }

                // Create ride request
                val rideResult = rideRepository.createRideRequest(
                    pickup,
                    drop,
                    vehicleType,
                    paymentMethod
                )

                rideResult.fold(
                    onSuccess = { ride ->
                        // Allocate driver using the allocation engine
                        _uiState.update { it.copy(allocationStatus = "Notifying nearby drivers...") }

                        val allocationResult = driverAllocationEngine.allocateDriver(
                            RideRequest(
                                userId = ride.userId,
                                pickupLocation = pickup,
                                dropLocation = drop,
                                vehicleType = vehicleType,
                                paymentMethod = paymentMethod,
                                estimatedFare = state.estimatedFare ?: Fare(),
                                requestedAt = java.util.Date()
                            )
                        )

                        allocationResult.fold(
                            onSuccess = { driverId ->
                                _uiState.update {
                                    it.copy(
                                        isBookingRide = false,
                                        allocationStatus = "Driver found!"
                                    )
                                }

                                // Start real-time tracking
                                startRideTracking(ride.id, driverId)

                                _events.emit(HomeEvent.NavigateToTracking(ride.id))
                            },
                            onFailure = { error ->
                                _uiState.update {
                                    it.copy(
                                        isBookingRide = false,
                                        allocationStatus = null,
                                        errorMessage = "No drivers available. Please try again."
                                    )
                                }
                            }
                        )
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isBookingRide = false,
                                allocationStatus = null,
                                errorMessage = error.message
                            )
                        }
                    }
                )
            }
        }
    }

    private fun calculateFareEstimate() {
        viewModelScope.launch {
            val pickup = _uiState.value.pickupLocation
            val drop = _uiState.value.dropLocation
            val vehicleType = _uiState.value.selectedVehicleType

            if (pickup != null && drop != null && vehicleType != null) {
                // Get surge multiplier for current location
                val surgeMultiplier = driverAllocationEngine.getSurgeMultiplier(pickup)

                // Get route info for accurate distance
                val routeResult = directionsService.getRoute(pickup, drop)

                routeResult.fold(
                    onSuccess = { routeInfo ->
                        val baseFare = rideRepository.calculateFare(pickup, drop, vehicleType)
                        val surgeFare = baseFare.copy(
                            surgeCharge = baseFare.total * (surgeMultiplier - 1),
                            total = baseFare.total * surgeMultiplier
                        )

                        _uiState.update {
                            it.copy(
                                estimatedFare = surgeFare,
                                estimatedTime = routeInfo.durationInTraffic ?: routeInfo.duration,
                                estimatedDistance = routeInfo.distance,
                                surgeMultiplier = surgeMultiplier
                            )
                        }
                    },
                    onFailure = {
                        // Fallback to basic calculation
                        val fare = rideRepository.calculateFare(pickup, drop, vehicleType)
                        _uiState.update { it.copy(estimatedFare = fare) }
                    }
                )
            }
        }
    }

    private fun fetchRoutePolyline() {
        viewModelScope.launch {
            val pickup = _uiState.value.pickupLocation
            val drop = _uiState.value.dropLocation

            if (pickup != null && drop != null) {
                val routeResult = directionsService.getRoute(pickup, drop)

                routeResult.fold(
                    onSuccess = { routeInfo ->
                        _uiState.update {
                            it.copy(
                                routePolyline = routeInfo.encodedPolyline,
                                routeSteps = routeInfo.steps
                            )
                        }
                    },
                    onFailure = {
                        // Handle error
                    }
                )
            }
        }
    }

    private fun startRideTracking(rideId: String, driverId: String) {
        viewModelScope.launch {
            val polyline = _uiState.value.routePolyline ?: ""

            trackingManager.startTracking(
                rideId = rideId,
                driverId = driverId,
                userId = "current_user_id", // Get from auth
                initialPolyline = polyline
            ).collect { update ->
                // Handle tracking updates
                _uiState.update {
                    it.copy(
                        driverLocation = Location(
                            update.currentLocation.latitude,
                            update.currentLocation.longitude
                        ),
                        driverSpeed = update.speed,
                        driverETA = update.estimatedTimeRemaining
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

data class HomeUiState(
    val pickupLocation: Location? = null,
    val dropLocation: Location? = null,
    val selectedVehicleType: VehicleType? = null,
    val selectedPaymentMethod: PaymentMethod? = PaymentMethod(
        id = "1",
        type = PaymentType.CASH,
        isDefault = true
    ),
    val nearbyDrivers: List<NearbyDriver> = emptyList(),
    val estimatedFare: Fare? = null,
    val estimatedTime: Int? = null,
    val estimatedDistance: Double? = null,
    val surgeMultiplier: Float = 1.0f,
    val routePolyline: String? = null,
    val routeSteps: List<com.daxido.core.maps.DirectionsService.NavigationStep> = emptyList(),
    val isSearchingDrivers: Boolean = false,
    val isBookingRide: Boolean = false,
    val allocationStatus: String? = null,
    val driverLocation: Location? = null,
    val driverSpeed: Float? = null,
    val driverETA: Int? = null,
    val errorMessage: String? = null
)

sealed class HomeEvent {
    data class NavigateToTracking(val rideId: String) : HomeEvent()
    object ShowPaymentOptions : HomeEvent()
    data class ShowError(val message: String) : HomeEvent()
}