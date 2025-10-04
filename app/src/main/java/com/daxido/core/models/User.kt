package com.daxido.core.models

import java.util.Date

data class User(
    val id: String = "",
    val phoneNumber: String = "",
    val email: String? = null,
    val firstName: String = "",
    val lastName: String = "",
    val profileImageUrl: String? = null,
    val rating: Float = 5.0f,
    val totalRides: Int = 0,
    val joinedDate: Date = Date(),
    val isVerified: Boolean = false,
    val fcmToken: String? = null,
    val preferredPaymentMethod: PaymentMethod? = null,
    val savedAddresses: List<SavedAddress> = emptyList(),
    val walletBalance: Double = 0.0,
    val referralCode: String? = null,
    val referredBy: String? = null
)

data class Driver(
    val id: String = "",
    val userId: String = "",
    val vehicleDetails: VehicleDetails? = null,
    val licenseNumber: String = "",
    val licenseExpiryDate: Date? = null,
    val kycStatus: KycStatus = KycStatus.PENDING,
    val isOnline: Boolean = false,
    val currentLocation: Location? = null,
    val totalEarnings: Double = 0.0,
    val todayEarnings: Double = 0.0,
    val rating: Float = 5.0f,
    val totalTrips: Int = 0,
    val acceptanceRate: Float = 100f,
    val cancellationRate: Float = 0f,
    val documentsUploaded: List<Document> = emptyList()
)

data class VehicleDetails(
    val vehicleType: VehicleType = VehicleType.CAR,
    val make: String = "",
    val model: String = "",
    val year: Int = 2020,
    val color: String = "",
    val licensePlate: String = "",
    val registrationNumber: String = "",
    val insuranceExpiryDate: Date? = null
)

enum class VehicleType {
    BIKE, AUTO, CAR, PREMIUM
}

enum class KycStatus {
    PENDING, IN_REVIEW, APPROVED, REJECTED
}

data class SavedAddress(
    val id: String = "",
    val type: AddressType = AddressType.OTHER,
    val name: String = "",
    val address: String = "",
    val location: Location = Location(),
    val instructions: String? = null
)

enum class AddressType {
    HOME, WORK, OTHER
}

data class Location(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val name: String? = null,
    val address: String? = null
)

data class Document(
    val type: DocumentType = DocumentType.LICENSE,
    val url: String = "",
    val uploadedAt: Date = Date(),
    val verificationStatus: VerificationStatus = VerificationStatus.PENDING
)

enum class DocumentType {
    LICENSE, VEHICLE_RC, INSURANCE, AADHAR, PAN, PROFILE_PHOTO
}

enum class VerificationStatus {
    PENDING, VERIFIED, REJECTED
}