package com.daxido.admin.models

import com.daxido.core.models.Location
import com.daxido.core.models.VehicleType
import java.util.Date

/**
 * ADMIN DASHBOARD MODELS
 * Complete admin control system like Ola/Uber/Rapido
 */

// Dashboard Analytics
data class DashboardAnalytics(
    val totalRides: Int = 0,
    val activeRides: Int = 0,
    val completedRidesToday: Int = 0,
    val cancelledRidesToday: Int = 0,
    val totalRevenue: Double = 0.0,
    val revenueToday: Double = 0.0,
    val totalUsers: Int = 0,
    val activeUsers: Int = 0,
    val totalDrivers: Int = 0,
    val onlineDrivers: Int = 0,
    val pendingDocuments: Int = 0,
    val pendingComplaints: Int = 0,
    val averageRating: Float = 0f,
    val peakHours: List<Int> = emptyList(),
    val popularRoutes: List<RouteAnalytics> = emptyList()
)

data class RouteAnalytics(
    val from: String,
    val to: String,
    val count: Int,
    val averageFare: Double,
    val averageDuration: Int
)

// Real-time Operations
data class LiveRideMonitoring(
    val rideId: String,
    val userId: String,
    val userName: String,
    val driverId: String,
    val driverName: String,
    val pickupLocation: Location,
    val dropLocation: Location,
    val currentLocation: Location,
    val status: String,
    val vehicleType: VehicleType,
    val fare: Double,
    val startTime: Date,
    val estimatedEndTime: Date,
    val sosActive: Boolean = false,
    val rating: Float? = null
)

data class DriverStatus(
    val driverId: String,
    val name: String,
    val phoneNumber: String,
    val currentLocation: Location,
    val isOnline: Boolean,
    val isAvailable: Boolean,
    val currentRideId: String? = null,
    val vehicleType: VehicleType,
    val vehicleNumber: String,
    val rating: Float,
    val totalRides: Int,
    val completedToday: Int,
    val earnings: Double,
    val earningsToday: Double,
    val lastSeen: Date,
    val documentsValid: Boolean
)

// User Management
data class UserManagement(
    val userId: String,
    val name: String,
    val phoneNumber: String,
    val email: String?,
    val profilePhoto: String?,
    val rating: Float,
    val totalRides: Int,
    val totalSpent: Double,
    val walletBalance: Double,
    val joinedDate: Date,
    val lastRideDate: Date?,
    val isActive: Boolean,
    val isBanned: Boolean,
    val complaints: Int,
    val referralCount: Int
)

// Driver Management
data class DriverManagement(
    val driverId: String,
    val name: String,
    val phoneNumber: String,
    val email: String?,
    val profilePhoto: String?,
    val vehicleType: VehicleType,
    val vehicleNumber: String,
    val vehicleModel: String,
    val rating: Float,
    val totalRides: Int,
    val acceptanceRate: Float,
    val cancellationRate: Float,
    val earnings: Double,
    val commission: Double,
    val joinedDate: Date,
    val lastActive: Date,
    val isOnline: Boolean,
    val isVerified: Boolean,
    val isBanned: Boolean,
    val documents: DriverDocuments,
    val bankDetails: BankDetails
)

data class DriverDocuments(
    val drivingLicense: DocumentStatus,
    val vehicleRegistration: DocumentStatus,
    val insurance: DocumentStatus,
    val backgroundCheck: DocumentStatus,
    val profilePhoto: DocumentStatus
)

data class DocumentStatus(
    val url: String?,
    val status: DocumentVerificationStatus,
    val expiryDate: Date?,
    val verifiedDate: Date?,
    val rejectionReason: String? = null
)

enum class DocumentVerificationStatus {
    PENDING,
    VERIFIED,
    REJECTED,
    EXPIRED,
    NOT_SUBMITTED
}

data class BankDetails(
    val accountNumber: String?,
    val ifscCode: String?,
    val accountHolderName: String?,
    val bankName: String?,
    val isVerified: Boolean
)

// Financial Management
data class FinancialOverview(
    val totalRevenue: Double,
    val totalCommission: Double,
    val driverPayouts: Double,
    val platformEarnings: Double,
    val pendingPayouts: Double,
    val walletBalance: Double,
    val refundsIssued: Double,
    val promoDiscounts: Double,
    val revenueByVehicleType: Map<VehicleType, Double>,
    val topRevenueDrivers: List<RevenueDriver>
)

data class RevenueDriver(
    val driverId: String,
    val name: String,
    val totalEarnings: Double,
    val rides: Int
)

data class DriverPayout(
    val payoutId: String,
    val driverId: String,
    val driverName: String,
    val amount: Double,
    val commission: Double,
    val netAmount: Double,
    val rides: Int,
    val startDate: Date,
    val endDate: Date,
    val status: PayoutStatus,
    val processedDate: Date?,
    val transactionId: String?
)

enum class PayoutStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED
}

// Promo & Pricing Management
data class PromoCodeManagement(
    val promoId: String,
    val code: String,
    val description: String,
    val discountType: DiscountType,
    val discountValue: Double,
    val maxDiscount: Double?,
    val minRideAmount: Double,
    val maxUsagePerUser: Int,
    val totalUsageLimit: Int,
    val currentUsage: Int,
    val startDate: Date,
    val endDate: Date,
    val isActive: Boolean,
    val applicableVehicleTypes: List<VehicleType>,
    val applicableUserSegment: UserSegment
)

enum class DiscountType {
    PERCENTAGE,
    FIXED_AMOUNT,
    CASHBACK
}

enum class UserSegment {
    ALL,
    NEW_USERS,
    PREMIUM_USERS,
    INACTIVE_USERS,
    CORPORATE
}

data class SurgePricing(
    val surgeId: String,
    val location: Location,
    val areaName: String,
    val radius: Double,
    val surgeMultiplier: Double,
    val vehicleType: VehicleType,
    val startTime: Date,
    val endTime: Date?,
    val isActive: Boolean,
    val reason: String,
    val demandCount: Int,
    val supplyCount: Int
)

// Support & Complaints
data class SupportTicket(
    val ticketId: String,
    val userId: String?,
    val driverId: String?,
    val userName: String,
    val userType: UserType,
    val rideId: String?,
    val category: ComplaintCategory,
    val subject: String,
    val description: String,
    val priority: Priority,
    val status: TicketStatus,
    val createdAt: Date,
    val updatedAt: Date,
    val assignedTo: String?,
    val resolution: String?,
    val attachments: List<String>,
    val chatMessages: List<ChatMessage>
)

enum class UserType {
    RIDER,
    DRIVER
}

enum class ComplaintCategory {
    PAYMENT_ISSUE,
    DRIVER_BEHAVIOR,
    RIDE_CANCELLATION,
    FARE_DISPUTE,
    SAFETY_CONCERN,
    APP_ISSUE,
    VEHICLE_CONDITION,
    ROUTE_ISSUE,
    OTHER
}

enum class Priority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
}

enum class TicketStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED,
    ESCALATED
}

data class ChatMessage(
    val messageId: String,
    val senderId: String,
    val senderName: String,
    val message: String,
    val timestamp: Date,
    val isAdminMessage: Boolean
)

// Reports & Analytics
data class AnalyticsReport(
    val reportType: ReportType,
    val startDate: Date,
    val endDate: Date,
    val data: Map<String, Any>
)

enum class ReportType {
    DAILY_SUMMARY,
    WEEKLY_SUMMARY,
    MONTHLY_SUMMARY,
    DRIVER_PERFORMANCE,
    REVENUE_BREAKDOWN,
    USER_ENGAGEMENT,
    RIDE_ANALYTICS,
    CANCELLATION_ANALYSIS,
    PEAK_HOUR_ANALYSIS,
    GEOGRAPHICAL_ANALYSIS
}

// System Configuration
data class SystemConfig(
    val configId: String,
    val baseFare: Map<VehicleType, Double>,
    val perKmRate: Map<VehicleType, Double>,
    val perMinuteRate: Map<VehicleType, Double>,
    val nightChargeMultiplier: Double,
    val nightChargeStartHour: Int,
    val nightChargeEndHour: Int,
    val cancellationFee: Double,
    val cancellationFreeTimeMinutes: Int,
    val driverCommissionPercentage: Double,
    val minimumWalletBalance: Double,
    val maximumRideDistance: Double,
    val surgeEnabled: Boolean,
    val ridePoolingEnabled: Boolean,
    val scheduledRidesEnabled: Boolean,
    val multiStopEnabled: Boolean
)

// Emergency & Safety
data class EmergencyAlert(
    val alertId: String,
    val rideId: String,
    val userId: String,
    val userName: String,
    val driverId: String,
    val driverName: String,
    val location: Location,
    val timestamp: Date,
    val type: EmergencyType,
    val status: AlertStatus,
    val responseTime: Long?,
    val resolvedAt: Date?,
    val actionTaken: String?
)

enum class EmergencyType {
    SOS,
    SUSPICIOUS_ACTIVITY,
    ACCIDENT,
    ROUTE_DEVIATION,
    EXTENDED_STOP
}

enum class AlertStatus {
    ACTIVE,
    ACKNOWLEDGED,
    RESOLVED,
    FALSE_ALARM
}

// Fleet Management
data class VehicleManagement(
    val vehicleId: String,
    val driverId: String,
    val vehicleType: VehicleType,
    val vehicleNumber: String,
    val vehicleModel: String,
    val vehicleYear: Int,
    val color: String,
    val registrationExpiry: Date,
    val insuranceExpiry: Date,
    val lastServiceDate: Date?,
    val nextServiceDate: Date?,
    val totalKm: Double,
    val condition: VehicleCondition,
    val isActive: Boolean
)

enum class VehicleCondition {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR,
    MAINTENANCE_REQUIRED
}

// Notification Management
data class AdminNotification(
    val notificationId: String,
    val title: String,
    val message: String,
    val targetAudience: NotificationAudience,
    val targetUserIds: List<String>?,
    val scheduleTime: Date?,
    val sentTime: Date?,
    val status: NotificationStatus,
    val deliveryCount: Int,
    val openCount: Int
)

enum class NotificationAudience {
    ALL_USERS,
    ALL_DRIVERS,
    SPECIFIC_USERS,
    SPECIFIC_DRIVERS,
    ACTIVE_USERS,
    INACTIVE_USERS,
    NEW_USERS
}

enum class NotificationStatus {
    DRAFT,
    SCHEDULED,
    SENT,
    FAILED
}
