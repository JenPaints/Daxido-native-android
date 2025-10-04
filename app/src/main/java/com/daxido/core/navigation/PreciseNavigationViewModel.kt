package com.daxido.core.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daxido.core.maps.DirectionsService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PreciseNavigationViewModel @Inject constructor(
    private val directionsService: DirectionsService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PreciseNavigationUiState())
    val uiState: StateFlow<PreciseNavigationUiState> = _uiState.asStateFlow()
    
    fun loadRoute(routeId: String) {
        viewModelScope.launch {
            // TODO: Load route from repository
            _uiState.value = _uiState.value.copy(
                route = emptyList(),
                currentStepIndex = 0,
                isNavigating = true
            )
        }
    }
    
    fun setRoute(route: List<com.daxido.core.maps.RouteStep>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                route = route,
                currentStepIndex = 0,
                currentStep = route.firstOrNull(),
                distance = route.sumOf { it.distance.toDouble() },
                duration = route.sumOf { it.duration }
            )
        }
    }
    
    fun startNavigation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isNavigating = true
            )
        }
    }
    
    fun updateCurrentLocation(location: com.google.android.gms.maps.model.LatLng) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                currentLocation = location
            )
        }
    }
    
    fun updateNavigationProgress(stepIndex: Int) {
        viewModelScope.launch {
            val progressPercentage = if (_uiState.value.route.isNotEmpty()) {
                (stepIndex.toFloat() / _uiState.value.route.size) * 100f
            } else 0f
            
            _uiState.value = _uiState.value.copy(
                currentStepIndex = stepIndex,
                currentStep = _uiState.value.route.getOrNull(stepIndex),
                progressPercentage = progressPercentage
            )
        }
    }
    
    fun completeNavigation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isNavigating = false,
                currentStepIndex = _uiState.value.route.size - 1
            )
        }
    }
    
    fun toggleNavigation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isNavigating = !_uiState.value.isNavigating
            )
        }
    }
    
    fun skipStep() {
        viewModelScope.launch {
            val nextIndex = (_uiState.value.currentStepIndex + 1).coerceAtMost(_uiState.value.route.size - 1)
            _uiState.value = _uiState.value.copy(
                currentStepIndex = nextIndex
            )
        }
    }
    
    fun recalculateRoute() {
        viewModelScope.launch {
            // TODO: Recalculate route
        }
    }
    
    fun updateLocation(lat: Double, lng: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                currentLocation = com.google.android.gms.maps.model.LatLng(lat, lng)
            )
        }
    }
    
    fun updateSpeed(speed: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                currentSpeed = speed
            )
        }
    }
    
    fun updateDistanceRemaining(distance: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                distanceRemaining = distance
            )
        }
    }
}

data class PreciseNavigationUiState(
    val currentLocation: com.google.android.gms.maps.model.LatLng = com.google.android.gms.maps.model.LatLng(0.0, 0.0),
    val currentSpeed: Int = 0,
    val distanceRemaining: Double = 0.0,
    val isNavigating: Boolean = false,
    val route: List<com.daxido.core.maps.RouteStep> = emptyList(),
    val currentStepIndex: Int = 0,
    val estimatedArrival: String = "",
    val nextTurn: String = "",
    val nextTurnDistance: Double = 0.0,
    val distance: Double = 0.0,
    val duration: Int = 0,
    val progressPercentage: Float = 0.0f,
    val currentStep: com.daxido.core.maps.RouteStep? = null
)
