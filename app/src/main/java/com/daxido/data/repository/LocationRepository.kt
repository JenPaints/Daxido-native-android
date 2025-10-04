package com.daxido.data.repository

import com.daxido.core.models.Location
import com.daxido.core.location.LocationService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor(
    private val locationService: LocationService
) {
    
    suspend fun getCurrentLocation(): Result<Location> {
        return try {
            val location = locationService.getCurrentLocation()
            Result.success(location)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchPlaces(query: String): Result<List<Location>> {
        return try {
            val places = locationService.searchPlaces(query)
            Result.success(places)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getRecentLocations(): Flow<List<Location>> {
        return locationService.getRecentLocations()
    }

    fun getSavedPlaces(): Flow<List<Location>> {
        return locationService.getSavedPlaces()
    }

    fun getPopularPlaces(): Flow<List<Location>> {
        return locationService.getPopularPlaces()
    }

    suspend fun saveLocation(location: Location) {
        locationService.saveLocation(location)
    }

    suspend fun removeSavedLocation(location: Location) {
        locationService.removeSavedLocation(location)
    }

    fun getLocationFromCoordinates(latitude: Double, longitude: Double): Result<Location> {
        return try {
            val location = locationService.getLocationFromCoordinates(latitude, longitude)
            Result.success(location)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
