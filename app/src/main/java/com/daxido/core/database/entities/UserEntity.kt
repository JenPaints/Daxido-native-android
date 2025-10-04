package com.daxido.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val profileImageUrl: String?,
    val rating: Double,
    val totalRides: Int,
    val isSynced: Boolean = false,
    val lastModified: Long = System.currentTimeMillis()
)
