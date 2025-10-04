package com.daxido.data.repository

import android.util.Log
import com.daxido.core.models.PaymentMethod
import com.daxido.core.models.PaymentType
import com.daxido.core.payment.RazorpayPaymentService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions,
    private val razorpayService: RazorpayPaymentService
) {
    
    private val TAG = "PaymentRepository"

    suspend fun getPaymentMethods(): List<PaymentMethod> {
        return try {
            val userId = auth.currentUser?.uid ?: return emptyList()

            // Get saved payment methods from Firestore
            val paymentMethodsSnapshot = firestore.collection("users")
                .document(userId)
                .collection("paymentMethods")
                .get()
                .await()

            val savedMethods = paymentMethodsSnapshot.documents.mapNotNull { doc ->
                doc.toObject(PaymentMethod::class.java)
            }

            // Always include cash option
            val allMethods = mutableListOf(
                PaymentMethod(
                    type = PaymentType.CASH,
                    walletBalance = null
                )
            )
            allMethods.addAll(savedMethods)

            // Add wallet with current balance
            val walletBalance = getWalletBalanceSync()
            allMethods.add(
                PaymentMethod(
                    type = PaymentType.WALLET,
                    walletBalance = walletBalance
                )
            )

            allMethods
        } catch (e: Exception) {
            // Return default payment methods on error
            listOf(
                PaymentMethod(type = PaymentType.CASH, walletBalance = null),
                PaymentMethod(type = PaymentType.WALLET, walletBalance = 0.0)
            )
        }
    }

    fun getWalletBalance(): Flow<Double> = callbackFlow {
        val userId = auth.currentUser?.uid ?: run {
            trySend(0.0)
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("wallets")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val balance = snapshot?.getDouble("balance") ?: 0.0
                trySend(balance)
            }

        awaitClose { listener.remove() }
    }

    private suspend fun getWalletBalanceSync(): Double {
        return try {
            val userId = auth.currentUser?.uid ?: return 0.0
            val walletDoc = firestore.collection("wallets")
                .document(userId)
                .get()
                .await()

            walletDoc.getDouble("balance") ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }

    suspend fun addMoneyToWallet(amount: Double, paymentMethod: PaymentMethod): Result<Double> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not authenticated"))

            when (paymentMethod.type) {
                PaymentType.CARD, PaymentType.UPI, PaymentType.NET_BANKING -> {
                    // Process payment through Firebase function with Razorpay
                    val paymentData = mapOf(
                        "userId" to userId,
                        "amount" to amount,
                        "paymentMethod" to paymentMethod.type.name
                    )

                    val result = functions.getHttpsCallable("processPayment")
                        .call(mapOf(
                            "rideId" to "wallet_recharge_${System.currentTimeMillis()}",
                            "paymentMethod" to "razorpay",
                            "paymentData" to paymentData
                        ))
                        .await()

                    val data = result.data as? Map<String, Any>
                    if (data?.get("success") == true) {
                        // Update wallet balance
                        val walletRef = firestore.collection("wallets").document(userId)
                        val currentBalance = getWalletBalanceSync()
                        val newBalance = currentBalance + amount

                        // Create transaction record
                        val transactionId = "TXN_${System.currentTimeMillis()}"
                        val transaction = mapOf(
                            "transactionId" to transactionId,
                            "userId" to userId,
                            "amount" to amount,
                            "type" to "WALLET_RECHARGE",
                            "status" to "COMPLETED",
                            "gatewayPaymentId" to data["paymentId"],
                            "timestamp" to com.google.firebase.Timestamp.now()
                        )

                        firestore.collection("transactions")
                            .document(transactionId)
                            .set(transaction)
                            .await()

                        // Update wallet balance
                        walletRef.set(
                            mapOf(
                                "balance" to newBalance,
                                "lastUpdated" to com.google.firebase.Timestamp.now()
                            )
                        ).await()

                        Result.success(newBalance)
                    } else {
                        Result.failure(Exception(data?.get("message") as? String ?: "Payment failed"))
                    }
                }
                else -> {
                    Result.failure(Exception("Unsupported payment method for wallet recharge"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Wallet recharge error", e)
            Result.failure(e)
        }
    }

    suspend fun processPayment(
        amount: Double,
        paymentMethod: PaymentMethod,
        rideId: String
    ): Result<String> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not authenticated"))

            val transactionId = "TXN_${System.currentTimeMillis()}"

            when (paymentMethod.type) {
                PaymentType.CASH -> {
                    // Cash payment - just record the transaction
                    val transaction = mapOf(
                        "transactionId" to transactionId,
                        "userId" to userId,
                        "rideId" to rideId,
                        "amount" to amount,
                        "paymentMethod" to "CASH",
                        "status" to "PENDING", // Will be marked as completed when driver confirms
                        "timestamp" to com.google.firebase.Timestamp.now()
                    )

                    firestore.collection("transactions")
                        .document(transactionId)
                        .set(transaction)
                        .await()

                    Result.success(transactionId)
                }

                PaymentType.WALLET -> {
                    // Process wallet payment through Firebase function
                    val paymentData = mapOf(
                        "rideId" to rideId,
                        "userId" to userId,
                        "amount" to amount
                    )

                    val result = functions.getHttpsCallable("processPayment")
                        .call(mapOf(
                            "rideId" to rideId,
                            "paymentMethod" to "wallet",
                            "paymentData" to paymentData
                        ))
                        .await()

                    val data = result.data as? Map<String, Any>
                    if (data?.get("success") == true) {
                        Result.success(data["paymentId"] as? String ?: transactionId)
                    } else {
                        Result.failure(Exception(data?.get("message") as? String ?: "Payment failed"))
                    }
                }

                PaymentType.CARD, PaymentType.UPI, PaymentType.NET_BANKING -> {
                    // Process through Firebase function with Razorpay
                    val paymentData = mapOf(
                        "rideId" to rideId,
                        "userId" to userId,
                        "amount" to amount,
                        "paymentMethod" to paymentMethod.type.name
                    )

                    val result = functions.getHttpsCallable("processPayment")
                        .call(mapOf(
                            "rideId" to rideId,
                            "paymentMethod" to "razorpay",
                            "paymentData" to paymentData
                        ))
                        .await()

                    val data = result.data as? Map<String, Any>
                    if (data?.get("success") == true) {
                        val paymentId = data["paymentId"] as? String ?: transactionId
                        
                        // Record transaction
                        val transaction = mapOf(
                            "transactionId" to transactionId,
                            "userId" to userId,
                            "rideId" to rideId,
                            "amount" to amount,
                            "paymentMethod" to paymentMethod.type.name,
                            "status" to "PROCESSING",
                            "gatewayPaymentId" to paymentId,
                            "timestamp" to com.google.firebase.Timestamp.now()
                        )

                        firestore.collection("transactions")
                            .document(transactionId)
                            .set(transaction)
                            .await()

                        Result.success(transactionId)
                    } else {
                        Result.failure(Exception(data?.get("message") as? String ?: "Payment failed"))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Payment processing error", e)
            Result.failure(e)
        }
    }

    suspend fun getTransactionHistory(): Result<List<Map<String, Any>>> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not authenticated"))

            val transactions = firestore.collection("transactions")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()

            val transactionList = transactions.documents.map { it.data ?: emptyMap() }
            Result.success(transactionList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addPaymentMethod(paymentMethod: PaymentMethod): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not authenticated"))

            firestore.collection("users")
                .document(userId)
                .collection("paymentMethods")
                .add(paymentMethod)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removePaymentMethod(paymentMethodId: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not authenticated"))

            firestore.collection("users")
                .document(userId)
                .collection("paymentMethods")
                .document(paymentMethodId)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun validatePromoCode(code: String): Result<com.daxido.user.presentation.ride.PromoDiscount> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not authenticated"))

            // Fetch promo code from Firestore
            val promoDoc = firestore.collection("promoCodes")
                .document(code.uppercase())
                .get()
                .await()

            if (!promoDoc.exists()) {
                return Result.failure(Exception("Invalid promo code"))
            }

            val promoData = promoDoc.data ?: return Result.failure(Exception("Invalid promo code"))

            // Check if promo code is active
            val isActive = promoData["isActive"] as? Boolean ?: false
            if (!isActive) {
                return Result.failure(Exception("This promo code has expired"))
            }

            // Check expiry date
            val expiryTimestamp = promoData["expiryDate"] as? com.google.firebase.Timestamp
            if (expiryTimestamp != null && expiryTimestamp.toDate().before(java.util.Date())) {
                return Result.failure(Exception("This promo code has expired"))
            }

            // Check usage limit
            val usageLimit = (promoData["usageLimit"] as? Long)?.toInt()
            val usedCount = (promoData["usedCount"] as? Long)?.toInt() ?: 0
            if (usageLimit != null && usedCount >= usageLimit) {
                return Result.failure(Exception("This promo code has reached its usage limit"))
            }

            // Check user-specific usage limit
            val perUserLimit = (promoData["perUserLimit"] as? Long)?.toInt()
            if (perUserLimit != null) {
                val userUsageDoc = firestore.collection("promoCodes")
                    .document(code.uppercase())
                    .collection("userUsage")
                    .document(userId)
                    .get()
                    .await()

                val userUsedCount = (userUsageDoc.data?.get("count") as? Long)?.toInt() ?: 0
                if (userUsedCount >= perUserLimit) {
                    return Result.failure(Exception("You have already used this promo code"))
                }
            }

            // Check minimum order value
            val minOrderValue = promoData["minOrderValue"] as? Double
            // Note: This check should be done when applying to actual ride amount

            val discount = com.daxido.user.presentation.ride.PromoDiscount(
                code = code.uppercase(),
                discountType = promoData["discountType"] as? String ?: "FIXED",
                discountValue = promoData["discountValue"] as? Double ?: 0.0,
                maxDiscount = promoData["maxDiscount"] as? Double,
                description = promoData["description"] as? String ?: "Discount applied"
            )

            Result.success(discount)
        } catch (e: Exception) {
            Log.e(TAG, "Promo code validation error", e)
            Result.failure(Exception("Failed to validate promo code"))
        }
    }

    suspend fun getWalletBalanceValue(): Double {
        return getWalletBalanceSync()
    }

    suspend fun withdrawFromWallet(amount: Double, bankAccountId: String): Result<Double> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not authenticated"))

            val currentBalance = getWalletBalanceSync()
            if (currentBalance < amount) {
                return Result.failure(Exception("Insufficient wallet balance"))
            }

            // Minimum withdrawal amount check
            if (amount < 100.0) {
                return Result.failure(Exception("Minimum withdrawal amount is ₹100"))
            }

            // Process withdrawal through Firebase function
            val withdrawalData = mapOf(
                "userId" to userId,
                "amount" to amount,
                "bankAccountId" to bankAccountId
            )

            val result = functions.getHttpsCallable("processWithdrawal")
                .call(withdrawalData)
                .await()

            val data = result.data as? Map<String, Any>
            if (data?.get("success") == true) {
                // Update wallet balance
                val newBalance = currentBalance - amount
                val walletRef = firestore.collection("wallets").document(userId)

                // Create transaction record
                val transactionId = "TXN_WD_${System.currentTimeMillis()}"
                val transaction = mapOf(
                    "transactionId" to transactionId,
                    "userId" to userId,
                    "amount" to -amount,
                    "type" to "WALLET_WITHDRAWAL",
                    "status" to "PROCESSING",
                    "bankAccountId" to bankAccountId,
                    "timestamp" to com.google.firebase.Timestamp.now()
                )

                firestore.collection("transactions")
                    .document(transactionId)
                    .set(transaction)
                    .await()

                // Update wallet balance
                walletRef.set(
                    mapOf(
                        "balance" to newBalance,
                        "lastUpdated" to com.google.firebase.Timestamp.now()
                    )
                ).await()

                Result.success(newBalance)
            } else {
                Result.failure(Exception(data?.get("message") as? String ?: "Withdrawal failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Wallet withdrawal error", e)
            Result.failure(e)
        }
    }
}
