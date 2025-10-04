package com.daxido.core.database.dao

import androidx.room.*
import com.daxido.core.database.entities.RideEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RideDao {

    @Query("SELECT * FROM rides WHERE userId = :userId ORDER BY createdAt DESC")
    fun getUserRides(userId: String): Flow<List<RideEntity>>

    @Query("SELECT * FROM rides WHERE id = :rideId")
    suspend fun getRideById(rideId: String): RideEntity?

    @Query("SELECT * FROM rides WHERE isSynced = 0")
    suspend fun getUnsyncedRides(): List<RideEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRide(ride: RideEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRides(rides: List<RideEntity>)

    @Update
    suspend fun updateRide(ride: RideEntity)

    @Query("UPDATE rides SET isSynced = 1 WHERE id = :rideId")
    suspend fun markAsSynced(rideId: String)

    @Delete
    suspend fun deleteRide(ride: RideEntity)

    @Query("DELETE FROM rides WHERE createdAt < :timestamp")
    suspend fun deleteOldRides(timestamp: Long)

    @Query("SELECT * FROM rides WHERE status = :status ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRidesByStatus(status: String, limit: Int = 10): List<RideEntity>
}
