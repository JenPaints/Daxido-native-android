package com.daxido.core.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.util.Log
import androidx.core.content.ContextCompat
import com.daxido.core.models.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.IOException
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationService @Inject constructor(
    private val context: Context,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    private val TAG = "LocationService"
    private val geocoder = Geocoder(context, Locale.getDefault())
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    suspend fun getCurrentLocation(): Location {
        return try {
            // Check for location permission
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "Location permission not granted, returning default location")
                return getDefaultLocation()
            }

            // Get current location using FusedLocationProviderClient
            val cancellationToken = CancellationTokenSource()

            val locationResult = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationToken.token
            ).await()

            if (locationResult != null) {
                val address = getAddressFromCoordinates(
                    locationResult.latitude,
                    locationResult.longitude
                )

                Location(
                    latitude = locationResult.latitude,
                    longitude = locationResult.longitude,
                    name = "Current Location",
                    address = address
                )
            } else {
                Log.w(TAG, "Location result is null, returning default")
                getDefaultLocation()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get current location", e)
            getDefaultLocation()
        }
    }

    suspend fun searchPlaces(query: String): List<Location> {
        if (query.isBlank()) return emptyList()

        return try {
            // Use Geocoder for place search as fallback
            val addresses = geocoder.getFromLocationName(query, 10)
            addresses?.mapNotNull { address ->
                if (address.hasLatitude() && address.hasLongitude()) {
                    Location(
                        latitude = address.latitude,
                        longitude = address.longitude,
                        name = address.featureName ?: address.getAddressLine(0) ?: query,
                        address = address.getAddressLine(0)
                    )
                } else {
                    null
                }
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Place search error", e)
            emptyList()
        }
    }

    fun getRecentLocations(): Flow<List<Location>> = callbackFlow {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users")
            .document(userId)
            .collection("recentLocations")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to recent locations", error)
                    close(error)
                    return@addSnapshotListener
                }

                val locations = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        Location(
                            latitude = doc.getDouble("latitude") ?: 0.0,
                            longitude = doc.getDouble("longitude") ?: 0.0,
                            name = doc.getString("name"),
                            address = doc.getString("address")
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing location", e)
                        null
                    }
                } ?: emptyList()

                trySend(locations)
            }

        awaitClose { listener.remove() }
    }

    fun getSavedPlaces(): Flow<List<Location>> = callbackFlow {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users")
            .document(userId)
            .collection("savedPlaces")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to saved places", error)
                    close(error)
                    return@addSnapshotListener
                }

                val locations = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        Location(
                            latitude = doc.getDouble("latitude") ?: 0.0,
                            longitude = doc.getDouble("longitude") ?: 0.0,
                            name = doc.getString("name"),
                            address = doc.getString("address")
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing saved place", e)
                        null
                    }
                } ?: emptyList()

                trySend(locations)
            }

        awaitClose { listener.remove() }
    }

    fun getPopularPlaces(): Flow<List<Location>> = callbackFlow {
        // Get popular places from Firestore config
        val listener = firestore.collection("config")
            .document("popularPlaces")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to popular places", error)
                    // Return default popular places on error
                    trySend(getDefaultPopularPlaces())
                    close(error)
                    return@addSnapshotListener
                }

                try {
                    val places = (snapshot?.get("places") as? List<Map<String, Any>>)?.mapNotNull { placeData ->
                        try {
                            Location(
                                latitude = (placeData["latitude"] as? Number)?.toDouble() ?: 0.0,
                                longitude = (placeData["longitude"] as? Number)?.toDouble() ?: 0.0,
                                name = placeData["name"] as? String,
                                address = placeData["address"] as? String
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing popular place", e)
                            null
                        }
                    } ?: getDefaultPopularPlaces()

                    trySend(places)
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing popular places", e)
                    trySend(getDefaultPopularPlaces())
                }
            }

        awaitClose { listener.remove() }
    }

    suspend fun saveLocation(location: Location) {
        val userId = auth.currentUser?.uid ?: return

        try {
            val locationData = hashMapOf(
                "latitude" to location.latitude,
                "longitude" to location.longitude,
                "name" to location.name,
                "address" to location.address,
                "timestamp" to com.google.firebase.Timestamp.now()
            )

            firestore.collection("users")
                .document(userId)
                .collection("savedPlaces")
                .add(locationData)
                .await()

            Log.d(TAG, "Location saved successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save location", e)
        }
    }

    suspend fun removeSavedLocation(location: Location) {
        val userId = auth.currentUser?.uid ?: return

        try {
            // Find and delete the location
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("savedPlaces")
                .whereEqualTo("latitude", location.latitude)
                .whereEqualTo("longitude", location.longitude)
                .get()
                .await()

            snapshot.documents.forEach { doc ->
                doc.reference.delete().await()
            }

            Log.d(TAG, "Location removed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove location", e)
        }
    }

    suspend fun addToRecent(location: Location) {
        val userId = auth.currentUser?.uid ?: return

        try {
            val locationData = hashMapOf(
                "latitude" to location.latitude,
                "longitude" to location.longitude,
                "name" to location.name,
                "address" to location.address,
                "timestamp" to com.google.firebase.Timestamp.now()
            )

            firestore.collection("users")
                .document(userId)
                .collection("recentLocations")
                .add(locationData)
                .await()

            // Keep only last 10 recent locations
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("recentLocations")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            if (snapshot.documents.size > 10) {
                snapshot.documents.drop(10).forEach { doc ->
                    doc.reference.delete().await()
                }
            }

            Log.d(TAG, "Added to recent locations")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add to recent", e)
        }
    }

    fun getLocationFromCoordinates(latitude: Double, longitude: Double): Location {
        val address = getAddressFromCoordinates(latitude, longitude)
        return Location(
            latitude = latitude,
            longitude = longitude,
            name = address,
            address = address
        )
    }

    private fun getAddressFromCoordinates(latitude: Double, longitude: Double): String {
        return try {
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses?.isNotEmpty() == true) {
                addresses[0].getAddressLine(0) ?: "Unknown Location"
            } else {
                "Lat: ${String.format("%.4f", latitude)}, Lng: ${String.format("%.4f", longitude)}"
            }
        } catch (e: IOException) {
            Log.e(TAG, "Geocoder failed", e)
            "Lat: ${String.format("%.4f", latitude)}, Lng: ${String.format("%.4f", longitude)}"
        }
    }

    private fun getDefaultLocation(): Location {
        return Location(
            latitude = 28.6139,
            longitude = 77.2090,
            name = "New Delhi",
            address = "New Delhi, India"
        )
    }

    private fun getDefaultPopularPlaces(): List<Location> {
        return listOf(
            Location(
                latitude = 28.6139,
                longitude = 77.2090,
                name = "India Gate",
                address = "Rajpath, New Delhi, Delhi 110001"
            ),
            Location(
                latitude = 19.0760,
                longitude = 72.8777,
                name = "Gateway of India",
                address = "Apollo Bandar, Colaba, Mumbai, Maharashtra 400001"
            ),
            Location(
                latitude = 12.9716,
                longitude = 77.5946,
                name = "Cubbon Park",
                address = "Kasturba Rd, Bengaluru, Karnataka 560001"
            ),
            Location(
                latitude = 22.5726,
                longitude = 88.3639,
                name = "Victoria Memorial",
                address = "Victoria Memorial Hall, Kolkata, West Bengal 700071"
            )
        )
    }
}
