package com.daxido.core.models

data class DriverInfo(
    val id: String,
    val name: String,
    val phone: String,
    val rating: Double,
    val totalRides: Int,
    val vehicleNumber: String,
    val vehicleModel: String,
    val profileImage: String? = null,
    val currentLocation: Location? = null
)
