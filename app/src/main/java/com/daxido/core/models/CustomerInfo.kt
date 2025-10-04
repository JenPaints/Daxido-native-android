package com.daxido.core.models

data class CustomerInfo(
    val id: String,
    val name: String,
    val phone: String,
    val rating: Double,
    val totalRides: Int,
    val profileImage: String? = null
)
