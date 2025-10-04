package com.daxido.core.corporate

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Corporate accounts service for business users
 */
@Singleton
class CorporateAccountsService @Inject constructor(
    private val context: Context
) {
    
    private val corporateAccounts = mutableMapOf<String, CorporateAccount>()
    private val corporateUsers = mutableMapOf<String, CorporateUser>()
    private val corporateRides = mutableMapOf<String, CorporateRide>()
    
    /**
     * Create a corporate account
     */
    suspend fun createCorporateAccount(
        accountId: String,
        companyName: String,
        contactPerson: String,
        email: String,
        phone: String,
        address: String,
        billingAddress: String,
        creditLimit: Double,
        paymentTerms: PaymentTerms
    ): CorporateAccountResult = withContext(Dispatchers.IO) {
        try {
            val corporateAccount = CorporateAccount(
                id = accountId,
                companyName = companyName,
                contactPerson = contactPerson,
                email = email,
                phone = phone,
                address = address,
                billingAddress = billingAddress,
                creditLimit = creditLimit,
                currentBalance = 0.0,
                paymentTerms = paymentTerms,
                status = CorporateAccountStatus.ACTIVE,
                createdAt = System.currentTimeMillis(),
                settings = CorporateSettings(
                    allowPoolRides = true,
                    allowScheduledRides = true,
                    allowMultiStopRides = true,
                    maxRideFare = creditLimit * 0.1, // 10% of credit limit per ride
                    requireApproval = false,
                    allowedVehicleTypes = listOf("economy", "comfort", "premium"),
                    allowedDepartments = emptyList()
                )
            )
            
            corporateAccounts[accountId] = corporateAccount
            
            Log.d("CorporateAccountsService", "Corporate account created: $accountId")
            CorporateAccountResult.Success(corporateAccount)
        } catch (e: Exception) {
            Log.e("CorporateAccountsService", "Error creating corporate account: ${e.message}", e)
            CorporateAccountResult.Failure("Failed to create corporate account: ${e.message}")
        }
    }
    
    /**
     * Add user to corporate account
     */
    suspend fun addCorporateUser(
        userId: String,
        accountId: String,
        employeeId: String,
        name: String,
        email: String,
        phone: String,
        department: String,
        role: CorporateUserRole,
        managerId: String? = null
    ): CorporateUserResult = withContext(Dispatchers.IO) {
        try {
            val corporateAccount = corporateAccounts[accountId]
                ?: return@withContext CorporateUserResult.Failure("Corporate account not found")
            
            val corporateUser = CorporateUser(
                id = userId,
                accountId = accountId,
                employeeId = employeeId,
                name = name,
                email = email,
                phone = phone,
                department = department,
                role = role,
                managerId = managerId,
                status = CorporateUserStatus.ACTIVE,
                createdAt = System.currentTimeMillis(),
                monthlyLimit = when (role) {
                    CorporateUserRole.EMPLOYEE -> 1000.0
                    CorporateUserRole.MANAGER -> 2000.0
                    CorporateUserRole.ADMIN -> 5000.0
                },
                currentMonthSpent = 0.0
            )
            
            corporateUsers[userId] = corporateUser
            
            Log.d("CorporateAccountsService", "Corporate user added: $userId")
            CorporateUserResult.Success(corporateUser)
        } catch (e: Exception) {
            Log.e("CorporateAccountsService", "Error adding corporate user: ${e.message}", e)
            CorporateUserResult.Failure("Failed to add corporate user: ${e.message}")
        }
    }
    
    /**
     * Book corporate ride
     */
    suspend fun bookCorporateRide(
        rideId: String,
        userId: String,
        pickupLocation: String,
        dropoffLocation: String,
        vehicleType: String,
        fare: Double,
        purpose: String? = null
    ): CorporateRideResult = withContext(Dispatchers.IO) {
        try {
            val corporateUser = corporateUsers[userId]
                ?: return@withContext CorporateRideResult.Failure("Corporate user not found")
            
            val corporateAccount = corporateAccounts[corporateUser.accountId]
                ?: return@withContext CorporateRideResult.Failure("Corporate account not found")
            
            // Check if user has exceeded monthly limit
            if (corporateUser.currentMonthSpent + fare > corporateUser.monthlyLimit) {
                return@withContext CorporateRideResult.Failure("Monthly limit exceeded")
            }
            
            // Check if account has sufficient credit
            if (corporateAccount.currentBalance + fare > corporateAccount.creditLimit) {
                return@withContext CorporateRideResult.Failure("Credit limit exceeded")
            }
            
            val corporateRide = CorporateRide(
                id = rideId,
                userId = userId,
                accountId = corporateUser.accountId,
                pickupLocation = pickupLocation,
                dropoffLocation = dropoffLocation,
                vehicleType = vehicleType,
                fare = fare,
                purpose = purpose,
                status = CorporateRideStatus.BOOKED,
                bookedAt = System.currentTimeMillis(),
                driverId = null,
                completedAt = null
            )
            
            corporateRides[rideId] = corporateRide
            
            // Update user's monthly spending
            val updatedUser = corporateUser.copy(
                currentMonthSpent = corporateUser.currentMonthSpent + fare
            )
            corporateUsers[userId] = updatedUser
            
            // Update account balance
            val updatedAccount = corporateAccount.copy(
                currentBalance = corporateAccount.currentBalance + fare
            )
            corporateAccounts[corporateUser.accountId] = updatedAccount
            
            Log.d("CorporateAccountsService", "Corporate ride booked: $rideId")
            CorporateRideResult.Success(corporateRide)
        } catch (e: Exception) {
            Log.e("CorporateAccountsService", "Error booking corporate ride: ${e.message}", e)
            CorporateRideResult.Failure("Failed to book corporate ride: ${e.message}")
        }
    }
    
    /**
     * Get corporate account details
     */
    suspend fun getCorporateAccount(accountId: String): CorporateAccount? = withContext(Dispatchers.IO) {
        return@withContext corporateAccounts[accountId]
    }
    
    /**
     * Get corporate users for account
     */
    suspend fun getCorporateUsers(accountId: String): Flow<List<CorporateUser>> = flow {
        val users = corporateUsers.values.filter { it.accountId == accountId }
        emit(users)
    }
    
    /**
     * Get corporate rides for account
     */
    suspend fun getCorporateRides(accountId: String): Flow<List<CorporateRide>> = flow {
        val rides = corporateRides.values.filter { it.accountId == accountId }
            .sortedByDescending { it.bookedAt }
        emit(rides)
    }
    
    /**
     * Get corporate rides for user
     */
    suspend fun getUserCorporateRides(userId: String): Flow<List<CorporateRide>> = flow {
        val rides = corporateRides.values.filter { it.userId == userId }
            .sortedByDescending { it.bookedAt }
        emit(rides)
    }
    
    /**
     * Generate corporate billing report
     */
    suspend fun generateBillingReport(
        accountId: String,
        startDate: Long,
        endDate: Long
    ): CorporateBillingReport = withContext(Dispatchers.IO) {
        val rides = corporateRides.values.filter { ride ->
            ride.accountId == accountId &&
            ride.bookedAt >= startDate &&
            ride.bookedAt <= endDate
        }
        
        val totalFare = rides.sumOf { it.fare }
        val rideCount = rides.size
        val averageFare = if (rideCount > 0) totalFare / rideCount else 0.0
        
        val ridesByUser = rides.groupBy { it.userId }
        val userBreakdown = ridesByUser.map { (userId, userRides) ->
            val user = corporateUsers[userId]
            UserRideBreakdown(
                userId = userId,
                userName = user?.name ?: "Unknown",
                department = user?.department ?: "Unknown",
                rideCount = userRides.size,
                totalFare = userRides.sumOf { it.fare }
            )
        }
        
        CorporateBillingReport(
            accountId = accountId,
            startDate = startDate,
            endDate = endDate,
            totalFare = totalFare,
            rideCount = rideCount,
            averageFare = averageFare,
            userBreakdown = userBreakdown,
            generatedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * Update corporate account settings
     */
    suspend fun updateCorporateSettings(
        accountId: String,
        settings: CorporateSettings
    ): CorporateAccountResult = withContext(Dispatchers.IO) {
        try {
            val corporateAccount = corporateAccounts[accountId]
                ?: return@withContext CorporateAccountResult.Failure("Corporate account not found")
            
            val updatedAccount = corporateAccount.copy(settings = settings)
            corporateAccounts[accountId] = updatedAccount
            
            Log.d("CorporateAccountsService", "Corporate settings updated: $accountId")
            CorporateAccountResult.Success(updatedAccount)
        } catch (e: Exception) {
            Log.e("CorporateAccountsService", "Error updating corporate settings: ${e.message}", e)
            CorporateAccountResult.Failure("Failed to update settings: ${e.message}")
        }
    }
}

data class CorporateAccount(
    val id: String,
    val companyName: String,
    val contactPerson: String,
    val email: String,
    val phone: String,
    val address: String,
    val billingAddress: String,
    val creditLimit: Double,
    val currentBalance: Double,
    val paymentTerms: PaymentTerms,
    val status: CorporateAccountStatus,
    val createdAt: Long,
    val settings: CorporateSettings
)

data class CorporateUser(
    val id: String,
    val accountId: String,
    val employeeId: String,
    val name: String,
    val email: String,
    val phone: String,
    val department: String,
    val role: CorporateUserRole,
    val managerId: String?,
    val status: CorporateUserStatus,
    val createdAt: Long,
    val monthlyLimit: Double,
    val currentMonthSpent: Double
)

data class CorporateRide(
    val id: String,
    val userId: String,
    val accountId: String,
    val pickupLocation: String,
    val dropoffLocation: String,
    val vehicleType: String,
    val fare: Double,
    val purpose: String?,
    val status: CorporateRideStatus,
    val bookedAt: Long,
    val driverId: String?,
    val completedAt: Long?
)

data class CorporateSettings(
    val allowPoolRides: Boolean,
    val allowScheduledRides: Boolean,
    val allowMultiStopRides: Boolean,
    val maxRideFare: Double,
    val requireApproval: Boolean,
    val allowedVehicleTypes: List<String>,
    val allowedDepartments: List<String>
)

data class CorporateBillingReport(
    val accountId: String,
    val startDate: Long,
    val endDate: Long,
    val totalFare: Double,
    val rideCount: Int,
    val averageFare: Double,
    val userBreakdown: List<UserRideBreakdown>,
    val generatedAt: Long
)

data class UserRideBreakdown(
    val userId: String,
    val userName: String,
    val department: String,
    val rideCount: Int,
    val totalFare: Double
)

enum class PaymentTerms {
    NET_15, NET_30, NET_45, NET_60
}

enum class CorporateAccountStatus {
    ACTIVE, SUSPENDED, CANCELLED
}

enum class CorporateUserRole {
    EMPLOYEE, MANAGER, ADMIN
}

enum class CorporateUserStatus {
    ACTIVE, SUSPENDED, INACTIVE
}

enum class CorporateRideStatus {
    BOOKED, IN_PROGRESS, COMPLETED, CANCELLED
}

sealed class CorporateAccountResult {
    data class Success(val corporateAccount: CorporateAccount) : CorporateAccountResult()
    data class Failure(val message: String) : CorporateAccountResult()
}

sealed class CorporateUserResult {
    data class Success(val corporateUser: CorporateUser) : CorporateUserResult()
    data class Failure(val message: String) : CorporateUserResult()
}

sealed class CorporateRideResult {
    data class Success(val corporateRide: CorporateRide) : CorporateRideResult()
    data class Failure(val message: String) : CorporateRideResult()
}
