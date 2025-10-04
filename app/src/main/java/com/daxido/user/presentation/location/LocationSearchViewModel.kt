package com.daxido.user.presentation.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daxido.core.models.Location
import com.daxido.data.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocationSearchViewModel @Inject constructor(
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationSearchUiState())
    val uiState: StateFlow<LocationSearchUiState> = _uiState.asStateFlow()

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        
        if (query.isNotEmpty()) {
            searchPlaces(query)
        } else {
            _uiState.update { it.copy(searchResults = emptyList()) }
        }
    }

    fun getCurrentLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            locationRepository.getCurrentLocation()
                .fold(
                    onSuccess = { location ->
                        _uiState.update { 
                            it.copy(
                                currentLocation = location,
                                isLoading = false
                            ) 
                        }
                    },
                    onFailure = {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                )
        }
    }

    fun loadRecentLocations() {
        viewModelScope.launch {
            val recentPlaces = locationRepository.getRecentLocations()
                .collect { locations ->
                    val places = locations.map { location ->
                        Place(
                            name = location.name ?: "Recent Location",
                            address = location.address ?: "",
                            location = location,
                            type = PlaceType.RECENT
                        )
                    }
                    _uiState.update { it.copy(recentLocations = places) }
                }
        }
    }

    fun loadSavedPlaces() {
        viewModelScope.launch {
            val savedPlaces = locationRepository.getSavedPlaces()
                .collect { locations ->
                    val places = locations.map { location ->
                        Place(
                            name = location.name ?: "Saved Place",
                            address = location.address ?: "",
                            location = location,
                            type = PlaceType.SAVED
                        )
                    }
                    _uiState.update { it.copy(savedPlaces = places) }
                }
        }
    }

    fun loadPopularPlaces() {
        viewModelScope.launch {
            val popularPlaces = locationRepository.getPopularPlaces()
                .collect { locations ->
                    val places = locations.map { location ->
                        Place(
                            name = location.name ?: "Popular Place",
                            address = location.address ?: "",
                            location = location,
                            type = PlaceType.POPULAR
                        )
                    }
                    _uiState.update { it.copy(popularPlaces = places) }
                }
        }
    }

    private fun searchPlaces(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            
            locationRepository.searchPlaces(query)
                .fold(
                    onSuccess = { places ->
                        val searchResults = places.map { location ->
                            Place(
                                name = location.name ?: "Search Result",
                                address = location.address ?: "",
                                location = location,
                                type = PlaceType.POPULAR
                            )
                        }
                        
                        _uiState.update { 
                            it.copy(
                                searchResults = searchResults,
                                isSearching = false
                            ) 
                        }
                    },
                    onFailure = {
                        _uiState.update { 
                            it.copy(
                                searchResults = emptyList(),
                                isSearching = false
                            ) 
                        }
                    }
                )
        }
    }

    fun saveLocation(place: Place) {
        viewModelScope.launch {
            locationRepository.saveLocation(place.location)
        }
    }

    fun removeSavedLocation(place: Place) {
        viewModelScope.launch {
            locationRepository.removeSavedLocation(place.location)
            loadSavedPlaces()
        }
    }
}

data class LocationSearchUiState(
    val searchQuery: String = "",
    val currentLocation: Location? = null,
    val recentLocations: List<Place> = emptyList(),
    val savedPlaces: List<Place> = emptyList(),
    val popularPlaces: List<Place> = emptyList(),
    val searchResults: List<Place> = emptyList(),
    val isLoading: Boolean = false,
    val isSearching: Boolean = false
)
