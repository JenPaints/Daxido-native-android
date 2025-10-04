package com.daxido.core.verification

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.util.Random
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OTP verification service for ride verification
 */
@Singleton
class OTPVerificationService @Inject constructor(
    private val context: Context
) {
    
    private val otpStorage = mutableMapOf<String, OTPData>()
    private val random = Random()
    
    /**
     * Generate and send OTP for ride verification
     */
    suspend fun generateAndSendOTP(
        rideId: String,
        phoneNumber: String,
        userType: UserType
    ): OTPResult = withContext(Dispatchers.IO) {
        try {
            val otp = generateOTP()
            val expiryTime = System.currentTimeMillis() + (5 * 60 * 1000) // 5 minutes
            
            val otpData = OTPData(
                otp = otp,
                phoneNumber = phoneNumber,
                userType = userType,
                generatedAt = System.currentTimeMillis(),
                expiryTime = expiryTime,
                attempts = 0,
                isVerified = false
            )
            
            otpStorage[rideId] = otpData
            
            // In a real app, this would send SMS via Firebase or Twilio
            sendOTPSMS(phoneNumber, otp)
            
            Log.d("OTPVerificationService", "OTP generated for ride $rideId: $otp")
            
            OTPResult.Success(otpData)
        } catch (e: Exception) {
            Log.e("OTPVerificationService", "Error generating OTP: ${e.message}", e)
            OTPResult.Failure("Failed to generate OTP: ${e.message}")
        }
    }
    
    /**
     * Verify OTP for ride
     */
    suspend fun verifyOTP(
        rideId: String,
        enteredOTP: String,
        userType: UserType
    ): OTPVerificationResult = withContext(Dispatchers.IO) {
        try {
            val otpData = otpStorage[rideId]
                ?: return@withContext OTPVerificationResult.Failure("OTP not found for this ride")
            
            // Check if OTP has expired
            if (System.currentTimeMillis() > otpData.expiryTime) {
                otpStorage.remove(rideId)
                return@withContext OTPVerificationResult.Failure("OTP has expired")
            }
            
            // Check if user type matches
            if (otpData.userType != userType) {
                return@withContext OTPVerificationResult.Failure("Invalid user type")
            }
            
            // Increment attempts
            val updatedOtpData = otpData.copy(attempts = otpData.attempts + 1)
            otpStorage[rideId] = updatedOtpData
            
            // Check if too many attempts
            if (updatedOtpData.attempts > 3) {
                otpStorage.remove(rideId)
                return@withContext OTPVerificationResult.Failure("Too many failed attempts")
            }
            
            // Verify OTP
            if (enteredOTP == otpData.otp) {
                val verifiedOtpData = updatedOtpData.copy(isVerified = true)
                otpStorage[rideId] = verifiedOtpData
                
                Log.d("OTPVerificationService", "OTP verified successfully for ride $rideId")
                return@withContext OTPVerificationResult.Success(verifiedOtpData)
            } else {
                Log.d("OTPVerificationService", "OTP verification failed for ride $rideId")
                return@withContext OTPVerificationResult.Failure("Invalid OTP")
            }
        } catch (e: Exception) {
            Log.e("OTPVerificationService", "Error verifying OTP: ${e.message}", e)
            OTPVerificationResult.Failure("Failed to verify OTP: ${e.message}")
        }
    }
    
    /**
     * Resend OTP
     */
    suspend fun resendOTP(rideId: String): OTPResult = withContext(Dispatchers.IO) {
        try {
            val otpData = otpStorage[rideId]
                ?: return@withContext OTPResult.Failure("OTP not found for this ride")
            
            // Check if we can resend (not too many resends)
            if (otpData.resendCount >= 3) {
                return@withContext OTPResult.Failure("Maximum resend attempts reached")
            }
            
            val newOtp = generateOTP()
            val expiryTime = System.currentTimeMillis() + (5 * 60 * 1000) // 5 minutes
            
            val updatedOtpData = otpData.copy(
                otp = newOtp,
                expiryTime = expiryTime,
                resendCount = otpData.resendCount + 1,
                attempts = 0
            )
            
            otpStorage[rideId] = updatedOtpData
            
            // Send new OTP
            sendOTPSMS(otpData.phoneNumber, newOtp)
            
            Log.d("OTPVerificationService", "OTP resent for ride $rideId: $newOtp")
            
            OTPResult.Success(updatedOtpData)
        } catch (e: Exception) {
            Log.e("OTPVerificationService", "Error resending OTP: ${e.message}", e)
            OTPResult.Failure("Failed to resend OTP: ${e.message}")
        }
    }
    
    /**
     * Check if OTP is verified for a ride
     */
    fun isOTPVerified(rideId: String): Boolean {
        val otpData = otpStorage[rideId]
        return otpData?.isVerified == true && System.currentTimeMillis() <= otpData.expiryTime
    }
    
    /**
     * Get OTP data for a ride
     */
    fun getOTPData(rideId: String): OTPData? {
        return otpStorage[rideId]
    }
    
    /**
     * Clear OTP data for a ride
     */
    fun clearOTP(rideId: String) {
        otpStorage.remove(rideId)
    }
    
    private fun generateOTP(): String {
        return String.format("%04d", random.nextInt(10000))
    }
    
    private suspend fun sendOTPSMS(phoneNumber: String, otp: String) {
        // In a real app, this would integrate with SMS service like Firebase or Twilio
        // For now, we'll just log it
        Log.d("OTPVerificationService", "Sending OTP $otp to $phoneNumber")
        
        // Simulate SMS sending delay
        delay(1000)
        
        // In production, you would use:
        // Firebase Functions to send SMS
        // Twilio SDK
        // AWS SNS
        // Or any other SMS service
    }
}

data class OTPData(
    val otp: String,
    val phoneNumber: String,
    val userType: UserType,
    val generatedAt: Long,
    val expiryTime: Long,
    val attempts: Int = 0,
    val resendCount: Int = 0,
    val isVerified: Boolean = false
)

enum class UserType {
    RIDER, DRIVER
}

sealed class OTPResult {
    data class Success(val otpData: OTPData) : OTPResult()
    data class Failure(val message: String) : OTPResult()
}

sealed class OTPVerificationResult {
    data class Success(val otpData: OTPData) : OTPVerificationResult()
    data class Failure(val message: String) : OTPVerificationResult()
}
