package com.daxido.core.services

import com.daxido.core.models.FavoriteLocation
import com.daxido.core.models.Location
import com.daxido.core.models.RecentSearch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Favorites and recent searches (Ola/Uber/Rapido style)
 */
@Singleton
class LocationHistoryService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    /**
     * Get favorite locations (Home, Work, etc.)
     */
    suspend fun getFavoriteLocations(): Result<List<FavoriteLocation>> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))

            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("favoriteLocations")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val favorites = snapshot.documents.mapNotNull {
                it.toObject(FavoriteLocation::class.java)?.copy(id = it.id)
            }

            Result.success(favorites)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Add favorite location
     */
    suspend fun addFavoriteLocation(
        label: String,
        address: String,
        latitude: Double,
        longitude: Double,
        icon: String = "star"
    ): Result<FavoriteLocation> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))

            // Check if label already exists (Home/Work should be unique)
            if (label.lowercase() in listOf("home", "work")) {
                val existing = firestore.collection("users")
                    .document(userId)
                    .collection("favoriteLocations")
                    .whereEqualTo("label", label)
                    .limit(1)
                    .get()
                    .await()

                if (!existing.isEmpty) {
                    // Update existing
                    val docId = existing.documents[0].id
                    val favorite = FavoriteLocation(
                        id = docId,
                        userId = userId,
                        label = label,
                        address = address,
                        latitude = latitude,
                        longitude = longitude,
                        icon = icon,
                        createdAt = System.currentTimeMillis()
                    )

                    firestore.collection("users")
                        .document(userId)
                        .collection("favoriteLocations")
                        .document(docId)
                        .set(favorite)
                        .await()

                    return Result.success(favorite)
                }
            }

            val favorite = FavoriteLocation(
                userId = userId,
                label = label,
                address = address,
                latitude = latitude,
                longitude = longitude,
                icon = icon,
                createdAt = System.currentTimeMillis()
            )

            val docRef = firestore.collection("users")
                .document(userId)
                .collection("favoriteLocations")
                .add(favorite)
                .await()

            Result.success(favorite.copy(id = docRef.id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Remove favorite location
     */
    suspend fun removeFavoriteLocation(favoriteId: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))

            firestore.collection("users")
                .document(userId)
                .collection("favoriteLocations")
                .document(favoriteId)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get recent searches
     */
    suspend fun getRecentSearches(limit: Int = 10): Result<List<RecentSearch>> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))

            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("recentSearches")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val searches = snapshot.documents.mapNotNull {
                it.toObject(RecentSearch::class.java)?.copy(id = it.id)
            }

            Result.success(searches)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Add recent search
     */
    suspend fun addRecentSearch(
        searchText: String,
        location: Location? = null
    ): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))

            // Check if search already exists
            val existing = firestore.collection("users")
                .document(userId)
                .collection("recentSearches")
                .whereEqualTo("searchText", searchText)
                .limit(1)
                .get()
                .await()

            if (!existing.isEmpty) {
                // Update timestamp
                existing.documents[0].reference.update("timestamp", System.currentTimeMillis()).await()
            } else {
                // Add new search
                val search = RecentSearch(
                    userId = userId,
                    searchText = searchText,
                    location = location,
                    timestamp = System.currentTimeMillis()
                )

                firestore.collection("users")
                    .document(userId)
                    .collection("recentSearches")
                    .add(search)
                    .await()

                // Keep only last 20 searches
                cleanupOldSearches(userId)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Clear recent searches
     */
    suspend fun clearRecentSearches(): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))

            val searches = firestore.collection("users")
                .document(userId)
                .collection("recentSearches")
                .get()
                .await()

            searches.documents.forEach { it.reference.delete() }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get Home location
     */
    suspend fun getHomeLocation(): FavoriteLocation? {
        return getFavoriteLocations()
            .getOrNull()
            ?.find { it.label.lowercase() == "home" }
    }

    /**
     * Get Work location
     */
    suspend fun getWorkLocation(): FavoriteLocation? {
        return getFavoriteLocations()
            .getOrNull()
            ?.find { it.label.lowercase() == "work" }
    }

    private suspend fun cleanupOldSearches(userId: String) {
        try {
            val searches = firestore.collection("users")
                .document(userId)
                .collection("recentSearches")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            // Delete searches beyond 20
            if (searches.size() > 20) {
                searches.documents.drop(20).forEach {
                    it.reference.delete()
                }
            }
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }
}
