package com.daxido.core.database.dao

import androidx.room.*
import com.daxido.core.database.entities.LocationCacheEntity

@Dao
interface LocationCacheDao {

    @Query("SELECT * FROM location_cache WHERE rideId = :rideId ORDER BY timestamp ASC")
    suspend fun getLocationsForRide(rideId: String): List<LocationCacheEntity>

    @Query("SELECT * FROM location_cache WHERE isSynced = 0")
    suspend fun getUnsyncedLocations(): List<LocationCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationCacheEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocations(locations: List<LocationCacheEntity>)

    @Query("UPDATE location_cache SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)

    @Query("DELETE FROM location_cache WHERE rideId = :rideId")
    suspend fun deleteLocationsForRide(rideId: String)

    @Query("DELETE FROM location_cache WHERE timestamp < :timestamp")
    suspend fun deleteOldLocations(timestamp: Long)
}
