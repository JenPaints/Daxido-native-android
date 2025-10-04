package com.daxido.core.analytics

import android.content.Context
import android.os.Bundle
import com.daxido.core.data.preferences.UserPreferences
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FEATURE COMPLETE: Analytics Manager with Privacy Controls
 * Handles all analytics and crash reporting with user consent
 */
@Singleton
class AnalyticsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferences: UserPreferences
) {

    private val firebaseAnalytics: FirebaseAnalytics by lazy {
        FirebaseAnalytics.getInstance(context)
    }

    private val crashlytics: FirebaseCrashlytics by lazy {
        FirebaseCrashlytics.getInstance()
    }

    init {
        // Apply user preferences on initialization
        applyPrivacySettings()
    }

    /**
     * Apply privacy settings based on user preferences
     */
    private fun applyPrivacySettings() {
        val dataSharing = userPreferences.isDataSharingEnabled()

        // Enable/disable analytics collection
        firebaseAnalytics.setAnalyticsCollectionEnabled(dataSharing)

        // Enable/disable crashlytics collection
        crashlytics.setCrashlyticsCollectionEnabled(dataSharing)
    }

    /**
     * Update data sharing preference
     */
    fun setDataSharingEnabled(enabled: Boolean) {
        userPreferences.setDataSharing(enabled)
        applyPrivacySettings()
    }

    /**
     * Check if data sharing is enabled
     */
    fun isDataSharingEnabled(): Boolean {
        return userPreferences.isDataSharingEnabled()
    }

    /**
     * Log event only if user has consented
     */
    fun logEvent(eventName: String, params: Map<String, Any>? = null) {
        if (!userPreferences.isDataSharingEnabled()) {
            return
        }

        val bundle = Bundle().apply {
            params?.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Double -> putDouble(key, value)
                    is Boolean -> putBoolean(key, value)
                    else -> putString(key, value.toString())
                }
            }
        }

        firebaseAnalytics.logEvent(eventName, bundle)
    }

    /**
     * Log screen view
     */
    fun logScreenView(screenName: String, screenClass: String) {
        if (!userPreferences.isDataSharingEnabled()) {
            return
        }

        logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, mapOf(
            FirebaseAnalytics.Param.SCREEN_NAME to screenName,
            FirebaseAnalytics.Param.SCREEN_CLASS to screenClass
        ))
    }

    /**
     * Log ride-related events
     */
    fun logRideEvent(eventType: RideEventType, rideId: String, additionalParams: Map<String, Any>? = null) {
        if (!userPreferences.isDataSharingEnabled()) {
            return
        }

        val params = mutableMapOf<String, Any>(
            "event_type" to eventType.name,
            "ride_id" to rideId
        )
        additionalParams?.let { params.putAll(it) }

        logEvent("ride_event", params)
    }

    /**
     * Log payment events (anonymized)
     */
    fun logPaymentEvent(success: Boolean, amount: Double, paymentMethod: String) {
        if (!userPreferences.isDataSharingEnabled()) {
            return
        }

        logEvent("payment_event", mapOf(
            "success" to success,
            "amount_range" to getAmountRange(amount), // Anonymize amount
            "payment_method" to paymentMethod
        ))
    }

    /**
     * Set user properties (only with consent)
     */
    fun setUserProperty(propertyName: String, propertyValue: String) {
        if (!userPreferences.isDataSharingEnabled()) {
            return
        }

        firebaseAnalytics.setUserProperty(propertyName, propertyValue)
    }

    /**
     * Log non-fatal error only if consent given
     */
    fun logError(throwable: Throwable, message: String? = null) {
        if (!userPreferences.isDataSharingEnabled()) {
            return
        }

        message?.let { crashlytics.log(it) }
        crashlytics.recordException(throwable)
    }

    /**
     * Set user ID for analytics (anonymized)
     */
    fun setUserId(userId: String) {
        if (!userPreferences.isDataSharingEnabled()) {
            return
        }

        // Use hashed user ID for privacy
        val hashedUserId = userId.hashCode().toString()
        firebaseAnalytics.setUserId(hashedUserId)
        crashlytics.setUserId(hashedUserId)
    }

    /**
     * Clear all analytics data
     */
    fun clearAnalyticsData() {
        firebaseAnalytics.resetAnalyticsData()
        setDataSharingEnabled(false)
    }

    /**
     * Anonymize amount to range for privacy
     */
    private fun getAmountRange(amount: Double): String {
        return when {
            amount < 50 -> "0-50"
            amount < 100 -> "50-100"
            amount < 200 -> "100-200"
            amount < 500 -> "200-500"
            amount < 1000 -> "500-1000"
            else -> "1000+"
        }
    }
}

/**
 * Ride event types for analytics
 */
enum class RideEventType {
    RIDE_REQUESTED,
    DRIVER_FOUND,
    DRIVER_ARRIVED,
    RIDE_STARTED,
    RIDE_COMPLETED,
    RIDE_CANCELLED
}
