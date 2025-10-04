package com.daxido.admin.data

import com.daxido.admin.models.*
import com.daxido.core.models.VehicleType
import com.daxido.core.optimization.FirestoreOptimizer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ADMIN REPOSITORY
 * Complete admin control and management system
 */
@Singleton
class AdminRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions,
    private val firestoreOptimizer: FirestoreOptimizer
) {

    // ==================== DASHBOARD ANALYTICS ====================

    suspend fun getDashboardAnalytics(): Result<DashboardAnalytics> = try {
        val today = Date()
        val todayStart = Date(today.time - (today.time % 86400000))

        // Parallel queries for better performance
        val totalRides = firestore.collection("rides").get().await().size()
        val activeRides = firestore.collection("rides")
            .whereIn("status", listOf("SEARCHING", "DRIVER_ASSIGNED", "TRIP_STARTED"))
            .get().await().size()

        val completedToday = firestore.collection("rides")
            .whereEqualTo("status", "COMPLETED")
            .whereGreaterThan("completedAt", todayStart)
            .get().await().size()

        val cancelledToday = firestore.collection("rides")
            .whereEqualTo("status", "CANCELLED")
            .whereGreaterThan("cancelledAt", todayStart)
            .get().await().size()

        val totalUsers = firestore.collection("users").get().await().size()
        val totalDrivers = firestore.collection("drivers").get().await().size()
        val onlineDrivers = firestore.collection("drivers")
            .whereEqualTo("isOnline", true)
            .get().await().size()

        // Calculate revenue
        val revenueSnapshot = firestore.collection("rides")
            .whereEqualTo("status", "COMPLETED")
            .get().await()

        var totalRevenue = 0.0
        var revenueToday = 0.0
        revenueSnapshot.documents.forEach { doc ->
            val fare = doc.getDouble("fare.total") ?: 0.0
            totalRevenue += fare
            val completedAt = doc.getDate("completedAt")
            if (completedAt != null && completedAt.after(todayStart)) {
                revenueToday += fare
            }
        }

        val analytics = DashboardAnalytics(
            totalRides = totalRides,
            activeRides = activeRides,
            completedRidesToday = completedToday,
            cancelledRidesToday = cancelledToday,
            totalRevenue = totalRevenue,
            revenueToday = revenueToday,
            totalUsers = totalUsers,
            totalDrivers = totalDrivers,
            onlineDrivers = onlineDrivers
        )

        Result.success(analytics)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ==================== LIVE RIDE MONITORING ====================

    fun getLiveRides(): Flow<List<LiveRideMonitoring>> = flow {
        val snapshot = firestore.collection("rides")
            .whereIn("status", listOf("DRIVER_ASSIGNED", "TRIP_STARTED"))
            .get().await()

        val liveRides = snapshot.documents.mapNotNull { doc ->
            try {
                // Parse ride data
                LiveRideMonitoring(
                    rideId = doc.id,
                    userId = doc.getString("userId") ?: "",
                    userName = "User", // Fetch from users collection
                    driverId = doc.getString("driverId") ?: "",
                    driverName = "Driver", // Fetch from drivers collection
                    pickupLocation = doc.get("pickupLocation") as? com.daxido.core.models.Location
                        ?: com.daxido.core.models.Location(0.0, 0.0),
                    dropLocation = doc.get("dropLocation") as? com.daxido.core.models.Location
                        ?: com.daxido.core.models.Location(0.0, 0.0),
                    currentLocation = doc.get("currentLocation") as? com.daxido.core.models.Location
                        ?: com.daxido.core.models.Location(0.0, 0.0),
                    status = doc.getString("status") ?: "",
                    vehicleType = VehicleType.valueOf(doc.getString("vehicleType") ?: "CAR"),
                    fare = doc.getDouble("fare.total") ?: 0.0,
                    startTime = doc.getDate("startedAt") ?: Date(),
                    estimatedEndTime = Date(), // Calculate ETA
                    sosActive = doc.getBoolean("sosTriggered") ?: false
                )
            } catch (e: Exception) {
                null
            }
        }

        emit(liveRides)
    }

    // ==================== USER MANAGEMENT ====================

    suspend fun getAllUsers(limit: Int = 50): Result<List<UserManagement>> = try {
        val query = firestoreOptimizer.paginatedQuery<UserManagement>(
            collectionPath = "users",
            orderByField = "joinedDate",
            limit = limit
        )

        val users = query.get().await().documents.mapNotNull { doc ->
            try {
                UserManagement(
                    userId = doc.id,
                    name = "${doc.getString("firstName")} ${doc.getString("lastName")}",
                    phoneNumber = doc.getString("phoneNumber") ?: "",
                    email = doc.getString("email"),
                    profilePhoto = doc.getString("profilePhotoUrl"),
                    rating = doc.getDouble("rating")?.toFloat() ?: 4.0f,
                    totalRides = doc.getLong("totalRides")?.toInt() ?: 0,
                    totalSpent = doc.getDouble("totalSpent") ?: 0.0,
                    walletBalance = doc.getDouble("walletBalance") ?: 0.0,
                    joinedDate = doc.getDate("joinedDate") ?: Date(),
                    lastRideDate = doc.getDate("lastRideDate"),
                    isActive = doc.getBoolean("isActive") ?: true,
                    isBanned = doc.getBoolean("isBanned") ?: false,
                    complaints = doc.getLong("complaints")?.toInt() ?: 0,
                    referralCount = doc.getLong("referralInfo.referralCount")?.toInt() ?: 0
                )
            } catch (e: Exception) {
                null
            }
        }

        Result.success(users)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun banUser(userId: String, reason: String): Result<Unit> = try {
        firestore.collection("users").document(userId).update(
            mapOf(
                "isBanned" to true,
                "banReason" to reason,
                "bannedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun unbanUser(userId: String): Result<Unit> = try {
        firestore.collection("users").document(userId).update(
            mapOf(
                "isBanned" to false,
                "banReason" to null,
                "unbannedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ==================== DRIVER MANAGEMENT ====================

    suspend fun getAllDrivers(limit: Int = 50): Result<List<DriverManagement>> = try {
        val query = firestoreOptimizer.paginatedQuery<DriverManagement>(
            collectionPath = "drivers",
            orderByField = "joinedDate",
            limit = limit
        )

        val drivers = query.get().await().documents.mapNotNull { doc ->
            try {
                DriverManagement(
                    driverId = doc.id,
                    name = doc.getString("name") ?: "",
                    phoneNumber = doc.getString("phoneNumber") ?: "",
                    email = doc.getString("email"),
                    profilePhoto = doc.getString("profilePhotoUrl"),
                    vehicleType = VehicleType.valueOf(doc.getString("vehicleType") ?: "CAR"),
                    vehicleNumber = doc.getString("vehicleNumber") ?: "",
                    vehicleModel = doc.getString("vehicleModel") ?: "",
                    rating = doc.getDouble("rating")?.toFloat() ?: 4.0f,
                    totalRides = doc.getLong("totalRides")?.toInt() ?: 0,
                    acceptanceRate = doc.getDouble("acceptanceRate")?.toFloat() ?: 0f,
                    cancellationRate = doc.getDouble("cancellationRate")?.toFloat() ?: 0f,
                    earnings = doc.getDouble("totalEarnings") ?: 0.0,
                    commission = doc.getDouble("totalCommission") ?: 0.0,
                    joinedDate = doc.getDate("joinedDate") ?: Date(),
                    lastActive = doc.getDate("lastActive") ?: Date(),
                    isOnline = doc.getBoolean("isOnline") ?: false,
                    isVerified = doc.getBoolean("isVerified") ?: false,
                    isBanned = doc.getBoolean("isBanned") ?: false,
                    documents = DriverDocuments(
                        drivingLicense = DocumentStatus(null, DocumentVerificationStatus.PENDING, null, null),
                        vehicleRegistration = DocumentStatus(null, DocumentVerificationStatus.PENDING, null, null),
                        insurance = DocumentStatus(null, DocumentVerificationStatus.PENDING, null, null),
                        backgroundCheck = DocumentStatus(null, DocumentVerificationStatus.PENDING, null, null),
                        profilePhoto = DocumentStatus(null, DocumentVerificationStatus.PENDING, null, null)
                    ),
                    bankDetails = BankDetails(null, null, null, null, false)
                )
            } catch (e: Exception) {
                null
            }
        }

        Result.success(drivers)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun verifyDriver(driverId: String): Result<Unit> = try {
        firestore.collection("drivers").document(driverId).update(
            mapOf(
                "isVerified" to true,
                "verifiedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun rejectDriver(driverId: String, reason: String): Result<Unit> = try {
        firestore.collection("drivers").document(driverId).update(
            mapOf(
                "isVerified" to false,
                "rejectionReason" to reason,
                "rejectedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ==================== FINANCIAL MANAGEMENT ====================

    suspend fun getFinancialOverview(): Result<FinancialOverview> = try {
        val ridesSnapshot = firestore.collection("rides")
            .whereEqualTo("status", "COMPLETED")
            .get().await()

        var totalRevenue = 0.0
        var totalCommission = 0.0
        val revenueByVehicleType = mutableMapOf<VehicleType, Double>()

        ridesSnapshot.documents.forEach { doc ->
            val fare = doc.getDouble("fare.total") ?: 0.0
            val commission = fare * 0.2 // 20% commission
            totalRevenue += fare
            totalCommission += commission

            val vehicleType = VehicleType.valueOf(doc.getString("vehicleType") ?: "CAR")
            revenueByVehicleType[vehicleType] = (revenueByVehicleType[vehicleType] ?: 0.0) + fare
        }

        val overview = FinancialOverview(
            totalRevenue = totalRevenue,
            totalCommission = totalCommission,
            driverPayouts = totalRevenue - totalCommission,
            platformEarnings = totalCommission,
            pendingPayouts = 0.0,
            walletBalance = 0.0,
            refundsIssued = 0.0,
            promoDiscounts = 0.0,
            revenueByVehicleType = revenueByVehicleType,
            topRevenueDrivers = emptyList()
        )

        Result.success(overview)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ==================== PROMO CODE MANAGEMENT ====================

    suspend fun createPromoCode(promo: PromoCodeManagement): Result<String> = try {
        val docRef = firestore.collection("promo_codes").add(
            mapOf(
                "code" to promo.code,
                "description" to promo.description,
                "discountType" to promo.discountType.name,
                "discountValue" to promo.discountValue,
                "maxDiscount" to promo.maxDiscount,
                "minRideAmount" to promo.minRideAmount,
                "maxUsagePerUser" to promo.maxUsagePerUser,
                "totalUsageLimit" to promo.totalUsageLimit,
                "currentUsage" to 0,
                "startDate" to promo.startDate,
                "endDate" to promo.endDate,
                "isActive" to promo.isActive,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
        ).await()

        Result.success(docRef.id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateSurgePricing(surge: SurgePricing): Result<Unit> = try {
        firestore.collection("surge_pricing").document(surge.surgeId).set(
            mapOf(
                "location" to surge.location,
                "areaName" to surge.areaName,
                "radius" to surge.radius,
                "surgeMultiplier" to surge.surgeMultiplier,
                "vehicleType" to surge.vehicleType.name,
                "startTime" to surge.startTime,
                "endTime" to surge.endTime,
                "isActive" to surge.isActive,
                "reason" to surge.reason
            )
        ).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ==================== SUPPORT MANAGEMENT ====================

    suspend fun getAllTickets(status: TicketStatus? = null): Result<List<SupportTicket>> = try {
        var query: Query = firestore.collection("support_tickets")
            .orderBy("createdAt", Query.Direction.DESCENDING)

        if (status != null) {
            query = query.whereEqualTo("status", status.name)
        }

        val tickets = query.limit(50).get().await().documents.mapNotNull { doc ->
            try {
                SupportTicket(
                    ticketId = doc.id,
                    userId = doc.getString("userId"),
                    driverId = doc.getString("driverId"),
                    userName = doc.getString("userName") ?: "",
                    userType = UserType.valueOf(doc.getString("userType") ?: "RIDER"),
                    rideId = doc.getString("rideId"),
                    category = ComplaintCategory.valueOf(doc.getString("category") ?: "OTHER"),
                    subject = doc.getString("subject") ?: "",
                    description = doc.getString("description") ?: "",
                    priority = Priority.valueOf(doc.getString("priority") ?: "MEDIUM"),
                    status = TicketStatus.valueOf(doc.getString("status") ?: "OPEN"),
                    createdAt = doc.getDate("createdAt") ?: Date(),
                    updatedAt = doc.getDate("updatedAt") ?: Date(),
                    assignedTo = doc.getString("assignedTo"),
                    resolution = doc.getString("resolution"),
                    attachments = emptyList(),
                    chatMessages = emptyList()
                )
            } catch (e: Exception) {
                null
            }
        }

        Result.success(tickets)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun resolveTicket(ticketId: String, resolution: String): Result<Unit> = try {
        firestore.collection("support_tickets").document(ticketId).update(
            mapOf(
                "status" to TicketStatus.RESOLVED.name,
                "resolution" to resolution,
                "resolvedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ==================== EMERGENCY ALERTS ====================

    suspend fun getActiveEmergencyAlerts(): Result<List<EmergencyAlert>> = try {
        val alerts = firestore.collection("emergencies")
            .whereEqualTo("status", AlertStatus.ACTIVE.name)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get().await().documents.mapNotNull { doc ->
                try {
                    EmergencyAlert(
                        alertId = doc.id,
                        rideId = doc.getString("rideId") ?: "",
                        userId = doc.getString("userId") ?: "",
                        userName = "User",
                        driverId = doc.getString("driverId") ?: "",
                        driverName = "Driver",
                        location = doc.get("location") as? com.daxido.core.models.Location
                            ?: com.daxido.core.models.Location(0.0, 0.0),
                        timestamp = doc.getDate("timestamp") ?: Date(),
                        type = EmergencyType.SOS,
                        status = AlertStatus.ACTIVE,
                        responseTime = null,
                        resolvedAt = null,
                        actionTaken = null
                    )
                } catch (e: Exception) {
                    null
                }
            }

        Result.success(alerts)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun acknowledgeEmergencyAlert(alertId: String): Result<Unit> = try {
        firestore.collection("emergencies").document(alertId).update(
            mapOf(
                "status" to AlertStatus.ACKNOWLEDGED.name,
                "acknowledgedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ==================== SYSTEM CONFIGURATION ====================

    suspend fun getSystemConfig(): Result<SystemConfig> = try {
        val doc = firestore.collection("config").document("system").get().await()

        val config = SystemConfig(
            configId = doc.id,
            baseFare = mapOf(
                VehicleType.BIKE to (doc.getDouble("baseFare.BIKE") ?: 25.0),
                VehicleType.AUTO to (doc.getDouble("baseFare.AUTO") ?: 40.0),
                VehicleType.CAR to (doc.getDouble("baseFare.CAR") ?: 60.0),
                VehicleType.PREMIUM to (doc.getDouble("baseFare.PREMIUM") ?: 100.0)
            ),
            perKmRate = mapOf(
                VehicleType.BIKE to 8.0,
                VehicleType.AUTO to 12.0,
                VehicleType.CAR to 15.0,
                VehicleType.PREMIUM to 25.0
            ),
            perMinuteRate = mapOf(
                VehicleType.BIKE to 1.0,
                VehicleType.AUTO to 1.5,
                VehicleType.CAR to 2.0,
                VehicleType.PREMIUM to 3.0
            ),
            nightChargeMultiplier = doc.getDouble("nightChargeMultiplier") ?: 1.25,
            nightChargeStartHour = doc.getLong("nightChargeStartHour")?.toInt() ?: 23,
            nightChargeEndHour = doc.getLong("nightChargeEndHour")?.toInt() ?: 5,
            cancellationFee = doc.getDouble("cancellationFee") ?: 20.0,
            cancellationFreeTimeMinutes = doc.getLong("cancellationFreeTimeMinutes")?.toInt() ?: 5,
            driverCommissionPercentage = doc.getDouble("driverCommissionPercentage") ?: 20.0,
            minimumWalletBalance = doc.getDouble("minimumWalletBalance") ?: 0.0,
            maximumRideDistance = doc.getDouble("maximumRideDistance") ?: 100.0,
            surgeEnabled = doc.getBoolean("surgeEnabled") ?: true,
            ridePoolingEnabled = doc.getBoolean("ridePoolingEnabled") ?: true,
            scheduledRidesEnabled = doc.getBoolean("scheduledRidesEnabled") ?: true,
            multiStopEnabled = doc.getBoolean("multiStopEnabled") ?: true
        )

        Result.success(config)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateSystemConfig(config: SystemConfig): Result<Unit> = try {
        firestore.collection("config").document("system").set(config).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ==================== NOTIFICATIONS ====================

    suspend fun sendBulkNotification(notification: AdminNotification): Result<Unit> = try {
        val data = hashMapOf(
            "title" to notification.title,
            "message" to notification.message,
            "targetAudience" to notification.targetAudience.name,
            "scheduleTime" to notification.scheduleTime,
            "status" to NotificationStatus.SENT.name
        )

        functions.getHttpsCallable("sendBulkNotification")
            .call(data)
            .await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
