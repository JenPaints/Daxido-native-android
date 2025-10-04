package com.daxido.core.services

import com.daxido.core.models.ReferralInfo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Referral system (Ola/Uber/Rapido style)
 */
@Singleton
class ReferralService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions
) {

    /**
     * Generate unique referral code for user
     */
    suspend fun generateReferralCode(): Result<String> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))

            // Check if user already has a code
            val existingCode = getUserReferralCode()
            if (existingCode != null) {
                return Result.success(existingCode)
            }

            // Generate new code (6 characters, alphanumeric)
            val code = generateUniqueCode()

            val referralInfo = ReferralInfo(
                referralCode = code,
                referralCount = 0,
                referralEarnings = 0.0,
                referralBonus = 100.0,
                refereeBonus = 50.0,
                isEligible = true
            )

            firestore.collection("users")
                .document(userId)
                .update("referralInfo", referralInfo)
                .await()

            Result.success(code)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get user's referral code
     */
    suspend fun getUserReferralCode(): String? {
        return try {
            val userId = auth.currentUser?.uid ?: return null

            val doc = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            val referralInfo = doc.toObject(ReferralInfo::class.java)
            referralInfo?.referralCode
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Apply referral code (for new users)
     */
    suspend fun applyReferralCode(code: String): Result<Double> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))

            // Check if code is valid
            val referrerDoc = firestore.collection("users")
                .whereEqualTo("referralInfo.referralCode", code.uppercase())
                .limit(1)
                .get()
                .await()

            if (referrerDoc.isEmpty) {
                return Result.failure(Exception("Invalid referral code"))
            }

            val referrerId = referrerDoc.documents[0].id

            // Check if user hasn't already used a referral code
            val userDoc = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            if (userDoc.get("referralInfo.referredBy") != null) {
                return Result.failure(Exception("You have already used a referral code"))
            }

            // Apply referral code via Firebase Function
            val result = functions.getHttpsCallable("applyReferralCode")
                .call(mapOf(
                    "userId" to userId,
                    "referrerId" to referrerId,
                    "referralCode" to code.uppercase()
                ))
                .await()

            val data = result.data as? Map<*, *>
            val bonus = (data?.get("bonus") as? Number)?.toDouble() ?: 50.0

            Result.success(bonus)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get referral stats
     */
    suspend fun getReferralStats(): Result<ReferralInfo> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))

            val doc = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            val data = doc.data?.get("referralInfo") as? Map<*, *>
            val referralInfo = ReferralInfo(
                referralCode = data?.get("referralCode") as? String ?: "",
                referredBy = data?.get("referredBy") as? String,
                referralCount = (data?.get("referralCount") as? Long)?.toInt() ?: 0,
                referralEarnings = (data?.get("referralEarnings") as? Number)?.toDouble() ?: 0.0,
                referralBonus = (data?.get("referralBonus") as? Number)?.toDouble() ?: 100.0,
                refereeBonus = (data?.get("refereeBonus") as? Number)?.toDouble() ?: 50.0,
                isEligible = data?.get("isEligible") as? Boolean ?: true
            )

            Result.success(referralInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Share referral code
     */
    fun getShareMessage(referralCode: String): String {
        return """
            🎁 Get ₹50 OFF on your first Daxido ride!

            Use my referral code: $referralCode

            Download Daxido app:
            https://daxido.com/app

            Happy riding! 🚗
        """.trimIndent()
    }

    private suspend fun generateUniqueCode(): String {
        var code: String
        var isUnique = false

        do {
            code = generateRandomCode()
            // Check if code already exists
            val existing = firestore.collection("users")
                .whereEqualTo("referralInfo.referralCode", code)
                .limit(1)
                .get()
                .await()

            isUnique = existing.isEmpty
        } while (!isUnique)

        return code
    }

    private fun generateRandomCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6)
            .map { chars.random() }
            .joinToString("")
    }
}
