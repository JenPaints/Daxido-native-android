package com.daxido.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Room entity for offline ride storage
 */
@Entity(tableName = "rides")
data class RideEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val driverId: String?,
    val pickupLat: Double,
    val pickupLng: Double,
    val pickupAddress: String,
    val dropoffLat: Double,
    val dropoffLng: Double,
    val dropoffAddress: String,
    val status: String,
    val vehicleType: String,
    val fare: Double,
    val distance: Double,
    val duration: Long,
    val otp: String?,
    val createdAt: Long,
    val startedAt: Long?,
    val completedAt: Long?,
    val isSynced: Boolean = false, // Track if synced with server
    val lastModified: Long = System.currentTimeMillis()
)
