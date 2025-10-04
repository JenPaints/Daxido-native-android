package com.daxido.core.promo

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Promo code validation and management service
 */
@Singleton
class PromoCodeService @Inject constructor(
    private val context: Context,
    private val firestore: FirebaseFirestore
) {

    private val promoCodesCollection = firestore.collection("promoCodes")
    private val userPromoUsageCollection = firestore.collection("userPromoUsage")

    /**
     * Validate promo code
     */
    suspend fun validatePromoCode(
        promoCode: String,
        userId: String,
        rideAmount: Double
    ): PromoCodeValidationResult {
        return try {
            val promoDoc = promoCodesCollection
                .document(promoCode.uppercase())
                .get()
                .await()

            if (!promoDoc.exists()) {
                return PromoCodeValidationResult.Invalid("Promo code not found")
            }

            val promo = promoDoc.toObject(PromoCode::class.java)
                ?: return PromoCodeValidationResult.Invalid("Invalid promo code")

            // Check if promo code is active
            if (!promo.isActive) {
                return PromoCodeValidationResult.Invalid("Promo code is inactive")
            }

            // Check expiry date
            val currentTime = System.currentTimeMillis()
            if (currentTime < promo.startDate) {
                return PromoCodeValidationResult.Invalid("Promo code not yet valid")
            }
            if (currentTime > promo.endDate) {
                return PromoCodeValidationResult.Invalid("Promo code has expired")
            }

            // Check usage limit
            if (promo.usageLimit > 0 && promo.usedCount >= promo.usageLimit) {
                return PromoCodeValidationResult.Invalid("Promo code usage limit reached")
            }

            // Check per-user usage limit
            val userUsage = getUserPromoUsage(userId, promo.code)
            if (promo.perUserLimit > 0 && userUsage >= promo.perUserLimit) {
                return PromoCodeValidationResult.Invalid("You have already used this promo code")
            }

            // Check minimum order amount
            if (rideAmount < promo.minOrderAmount) {
                return PromoCodeValidationResult.Invalid(
                    "Minimum order amount ₹${promo.minOrderAmount} required"
                )
            }

            // Check maximum discount
            val discount = calculateDiscount(promo, rideAmount)
            val finalAmount = (rideAmount - discount).coerceAtLeast(0.0)

            PromoCodeValidationResult.Valid(
                promoCode = promo,
                discountAmount = discount,
                finalAmount = finalAmount
            )

        } catch (e: Exception) {
            Log.e("PromoCodeService", "Error validating promo code: ${e.message}", e)
            PromoCodeValidationResult.Invalid("Error validating promo code")
        }
    }

    /**
     * Apply promo code to ride
     */
    suspend fun applyPromoCode(
        promoCode: String,
        userId: String,
        rideId: String,
        originalAmount: Double
    ): PromoCodeApplicationResult {
        return try {
            val validationResult = validatePromoCode(promoCode, userId, originalAmount)

            when (validationResult) {
                is PromoCodeValidationResult.Valid -> {
                    // Update promo code usage
                    promoCodesCollection.document(promoCode.uppercase())
                        .update("usedCount", validationResult.promoCode.usedCount + 1)
                        .await()

                    // Record user usage
                    val usageRecord = PromoUsageRecord(
                        id = "${userId}_${promoCode}_${System.currentTimeMillis()}",
                        userId = userId,
                        promoCode = promoCode,
                        rideId = rideId,
                        originalAmount = originalAmount,
                        discountAmount = validationResult.discountAmount,
                        finalAmount = validationResult.finalAmount,
                        appliedAt = System.currentTimeMillis()
                    )

                    userPromoUsageCollection.document(usageRecord.id)
                        .set(usageRecord)
                        .await()

                    Log.d("PromoCodeService", "Promo code applied: $promoCode")
                    PromoCodeApplicationResult.Success(
                        discountAmount = validationResult.discountAmount,
                        finalAmount = validationResult.finalAmount
                    )
                }
                is PromoCodeValidationResult.Invalid -> {
                    PromoCodeApplicationResult.Failure(validationResult.reason)
                }
            }
        } catch (e: Exception) {
            Log.e("PromoCodeService", "Error applying promo code: ${e.message}", e)
            PromoCodeApplicationResult.Failure("Error applying promo code")
        }
    }

    /**
     * Get available promo codes for user
     */
    suspend fun getAvailablePromoCodes(userId: String): List<PromoCode> {
        return try {
            val currentTime = System.currentTimeMillis()

            val promoDocs = promoCodesCollection
                .whereEqualTo("isActive", true)
                .whereLessThanOrEqualTo("startDate", currentTime)
                .whereGreaterThanOrEqualTo("endDate", currentTime)
                .get()
                .await()

            promoDocs.documents.mapNotNull { doc ->
                doc.toObject(PromoCode::class.java)
            }.filter { promo ->
                // Filter out codes that user has already used max times
                val userUsage = getUserPromoUsage(userId, promo.code)
                promo.perUserLimit == 0 || userUsage < promo.perUserLimit
            }

        } catch (e: Exception) {
            Log.e("PromoCodeService", "Error getting available promo codes: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Create promo code (admin function)
     */
    suspend fun createPromoCode(promoCode: PromoCode): Boolean {
        return try {
            promoCodesCollection.document(promoCode.code.uppercase())
                .set(promoCode)
                .await()

            Log.d("PromoCodeService", "Promo code created: ${promoCode.code}")
            true
        } catch (e: Exception) {
            Log.e("PromoCodeService", "Error creating promo code: ${e.message}", e)
            false
        }
    }

    /**
     * Calculate discount based on promo code
     */
    private fun calculateDiscount(promoCode: PromoCode, amount: Double): Double {
        val discount = when (promoCode.discountType) {
            DiscountType.PERCENTAGE -> amount * (promoCode.discountValue / 100.0)
            DiscountType.FIXED -> promoCode.discountValue
        }

        return if (promoCode.maxDiscountAmount > 0) {
            discount.coerceAtMost(promoCode.maxDiscountAmount)
        } else {
            discount
        }
    }

    /**
     * Get user's promo code usage count
     */
    private suspend fun getUserPromoUsage(userId: String, promoCode: String): Int {
        return try {
            val usageDocs = userPromoUsageCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("promoCode", promoCode)
                .get()
                .await()

            usageDocs.size()
        } catch (e: Exception) {
            Log.e("PromoCodeService", "Error getting user promo usage: ${e.message}", e)
            0
        }
    }
}

data class PromoCode(
    val code: String = "",
    val description: String = "",
    val discountType: DiscountType = DiscountType.PERCENTAGE,
    val discountValue: Double = 0.0,
    val maxDiscountAmount: Double = 0.0,
    val minOrderAmount: Double = 0.0,
    val startDate: Long = 0,
    val endDate: Long = 0,
    val usageLimit: Int = 0,
    val perUserLimit: Int = 0,
    val usedCount: Int = 0,
    val isActive: Boolean = true,
    val applicableFor: List<String> = emptyList(), // Vehicle types
    val createdAt: Long = 0
)

data class PromoUsageRecord(
    val id: String = "",
    val userId: String = "",
    val promoCode: String = "",
    val rideId: String = "",
    val originalAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val finalAmount: Double = 0.0,
    val appliedAt: Long = 0
)

enum class DiscountType {
    PERCENTAGE, FIXED
}

sealed class PromoCodeValidationResult {
    data class Valid(
        val promoCode: PromoCode,
        val discountAmount: Double,
        val finalAmount: Double
    ) : PromoCodeValidationResult()

    data class Invalid(
        val reason: String
    ) : PromoCodeValidationResult()
}

sealed class PromoCodeApplicationResult {
    data class Success(
        val discountAmount: Double,
        val finalAmount: Double
    ) : PromoCodeApplicationResult()

    data class Failure(
        val reason: String
    ) : PromoCodeApplicationResult()
}