package com.daxido.user.presentation.surge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SurgePricingViewModel @Inject constructor(
    // Inject services as needed
) : ViewModel() {

    private val _uiState = MutableStateFlow(SurgePricingUiState())
    val uiState: StateFlow<SurgePricingUiState> = _uiState.asStateFlow()

    init {
        loadSurgeZones()
    }

    private fun loadSurgeZones() {
        viewModelScope.launch {
            // Mock surge zones - in production, fetch from backend
            val zones = listOf(
                SurgeZone(
                    id = "zone1",
                    location = LatLng(12.9716, 77.5946), // Bangalore center
                    areaName = "MG Road",
                    multiplier = 2.5,
                    radiusMeters = 2000,
                    demandLevel = "Very High"
                ),
                SurgeZone(
                    id = "zone2",
                    location = LatLng(12.9352, 77.6245), // Koramangala
                    areaName = "Koramangala",
                    multiplier = 2.0,
                    radiusMeters = 1500,
                    demandLevel = "High"
                ),
                SurgeZone(
                    id = "zone3",
                    location = LatLng(13.0067, 77.5963), // Hebbal
                    areaName = "Hebbal",
                    multiplier = 1.5,
                    radiusMeters = 1000,
                    demandLevel = "Medium"
                ),
                SurgeZone(
                    id = "zone4",
                    location = LatLng(12.9698, 77.7500), // Whitefield
                    areaName = "Whitefield",
                    multiplier = 1.8,
                    radiusMeters = 2500,
                    demandLevel = "High"
                ),
                SurgeZone(
                    id = "zone5",
                    location = LatLng(12.9141, 77.6411), // HSR Layout
                    areaName = "HSR Layout",
                    multiplier = 1.3,
                    radiusMeters = 1200,
                    demandLevel = "Low"
                )
            )

            _uiState.value = _uiState.value.copy(
                surgeZones = zones,
                currentAreaSurge = zones.first(), // Mock current location
                isLoading = false
            )
        }
    }

    fun updateCurrentLocation(location: LatLng) {
        viewModelScope.launch {
            // Find closest surge zone
            val closestZone = _uiState.value.surgeZones.minByOrNull { zone ->
                calculateDistance(location, zone.location)
            }

            _uiState.value = _uiState.value.copy(
                currentAreaSurge = closestZone
            )
        }
    }

    private fun calculateDistance(loc1: LatLng, loc2: LatLng): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            loc1.latitude, loc1.longitude,
            loc2.latitude, loc2.longitude,
            results
        )
        return results[0]
    }
}