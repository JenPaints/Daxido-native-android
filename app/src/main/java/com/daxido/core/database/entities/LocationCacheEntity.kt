package com.daxido.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "location_cache")
data class LocationCacheEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rideId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val speed: Float,
    val bearing: Float,
    val timestamp: Long,
    val isSynced: Boolean = false
)
