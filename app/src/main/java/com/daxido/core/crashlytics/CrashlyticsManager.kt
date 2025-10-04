package com.daxido.core.crashlytics

import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager class for Firebase Crashlytics integration
 * Provides methods to log crashes, non-fatal errors, and custom events
 */
@Singleton
class CrashlyticsManager @Inject constructor() {
    
    private val crashlytics = FirebaseCrashlytics.getInstance()
    
    /**
     * Log a non-fatal error
     * @param throwable The exception to log
     * @param message Optional message to include with the error
     */
    fun logError(throwable: Throwable, message: String? = null) {
        message?.let { crashlytics.log(it) }
        crashlytics.recordException(throwable)
    }
    
    /**
     * Log a custom message
     * @param message The message to log
     */
    fun log(message: String) {
        crashlytics.log(message)
    }
    
    /**
     * Set a custom key-value pair for crash reports
     * @param key The key
     * @param value The value
     */
    fun setCustomKey(key: String, value: String) {
        crashlytics.setCustomKey(key, value)
    }
    
    /**
     * Set a custom key-value pair for crash reports (Boolean)
     * @param key The key
     * @param value The boolean value
     */
    fun setCustomKey(key: String, value: Boolean) {
        crashlytics.setCustomKey(key, value)
    }
    
    /**
     * Set a custom key-value pair for crash reports (Int)
     * @param key The key
     * @param value The integer value
     */
    fun setCustomKey(key: String, value: Int) {
        crashlytics.setCustomKey(key, value)
    }
    
    /**
     * Set a custom key-value pair for crash reports (Float)
     * @param key The key
     * @param value The float value
     */
    fun setCustomKey(key: String, value: Float) {
        crashlytics.setCustomKey(key, value)
    }
    
    /**
     * Set a custom key-value pair for crash reports (Double)
     * @param key The key
     * @param value The double value
     */
    fun setCustomKey(key: String, value: Double) {
        crashlytics.setCustomKey(key, value)
    }
    
    /**
     * Set user identifier for crash reports
     * @param userId The user ID
     */
    fun setUserId(userId: String) {
        crashlytics.setUserId(userId)
    }
    
    /**
     * Enable or disable crashlytics collection
     * @param enabled Whether to enable crashlytics
     */
    fun setCrashlyticsCollectionEnabled(enabled: Boolean) {
        crashlytics.setCrashlyticsCollectionEnabled(enabled)
    }
    
    /**
     * Log ride-related events for better crash context
     */
    fun logRideEvent(event: String, rideId: String? = null, additionalData: Map<String, Any>? = null) {
        crashlytics.log("Ride Event: $event")
        rideId?.let { crashlytics.setCustomKey("ride_id", it) }
        additionalData?.forEach { (key, value) ->
            when (value) {
                is String -> crashlytics.setCustomKey(key, value)
                is Boolean -> crashlytics.setCustomKey(key, value)
                is Int -> crashlytics.setCustomKey(key, value)
                is Float -> crashlytics.setCustomKey(key, value)
                is Double -> crashlytics.setCustomKey(key, value)
                else -> crashlytics.setCustomKey(key, value.toString())
            }
        }
    }
    
    /**
     * Log driver-related events
     */
    fun logDriverEvent(event: String, driverId: String? = null, additionalData: Map<String, Any>? = null) {
        crashlytics.log("Driver Event: $event")
        driverId?.let { crashlytics.setCustomKey("driver_id", it) }
        additionalData?.forEach { (key, value) ->
            when (value) {
                is String -> crashlytics.setCustomKey(key, value)
                is Boolean -> crashlytics.setCustomKey(key, value)
                is Int -> crashlytics.setCustomKey(key, value)
                is Float -> crashlytics.setCustomKey(key, value)
                is Double -> crashlytics.setCustomKey(key, value)
                else -> crashlytics.setCustomKey(key, value.toString())
            }
        }
    }
    
    /**
     * Log payment-related events
     */
    fun logPaymentEvent(event: String, paymentId: String? = null, amount: Double? = null, additionalData: Map<String, Any>? = null) {
        crashlytics.log("Payment Event: $event")
        paymentId?.let { crashlytics.setCustomKey("payment_id", it) }
        amount?.let { crashlytics.setCustomKey("payment_amount", it) }
        additionalData?.forEach { (key, value) ->
            when (value) {
                is String -> crashlytics.setCustomKey(key, value)
                is Boolean -> crashlytics.setCustomKey(key, value)
                is Int -> crashlytics.setCustomKey(key, value)
                is Float -> crashlytics.setCustomKey(key, value)
                is Double -> crashlytics.setCustomKey(key, value)
                else -> crashlytics.setCustomKey(key, value.toString())
            }
        }
    }
    
    /**
     * Log emergency events
     */
    fun logEmergencyEvent(event: String, emergencyId: String? = null, additionalData: Map<String, Any>? = null) {
        crashlytics.log("Emergency Event: $event")
        emergencyId?.let { crashlytics.setCustomKey("emergency_id", it) }
        additionalData?.forEach { (key, value) ->
            when (value) {
                is String -> crashlytics.setCustomKey(key, value)
                is Boolean -> crashlytics.setCustomKey(key, value)
                is Int -> crashlytics.setCustomKey(key, value)
                is Float -> crashlytics.setCustomKey(key, value)
                is Double -> crashlytics.setCustomKey(key, value)
                else -> crashlytics.setCustomKey(key, value.toString())
            }
        }
    }
    
    /**
     * Force a test crash for testing purposes
     * WARNING: This will actually crash the app!
     */
    fun forceTestCrash() {
        crashlytics.log("Forcing test crash")
        throw RuntimeException("Test Crash - This is intentional for testing Crashlytics")
    }
}
