package com.daxido.core.config

import com.daxido.BuildConfig

object AppConfig {
    // Google Maps Configuration
    // SECURITY: API keys are now loaded from BuildConfig (configured in build.gradle.kts)
    // Add these to your local.properties file:
    // GOOGLE_MAPS_API_KEY=your_actual_key_here
    // RAZORPAY_KEY_ID=your_actual_key_here
    val GOOGLE_MAPS_API_KEY = BuildConfig.GOOGLE_MAPS_API_KEY
    val GOOGLE_PLACES_API_KEY = BuildConfig.GOOGLE_MAPS_API_KEY
    val GOOGLE_DIRECTIONS_API_KEY = BuildConfig.GOOGLE_MAPS_API_KEY
    
    // Google Sign-In Configuration
    val GOOGLE_OAUTH_CLIENT_ID = BuildConfig.GOOGLE_OAUTH_CLIENT_ID

    // Firebase Configuration
    const val FIREBASE_PROJECT_ID = "daxido-native"
    const val FIREBASE_DATABASE_URL = "https://daxido-native-default-rtdb.firebaseio.com"
    const val FIREBASE_STORAGE_BUCKET = "daxido-native.firebasestorage.app"

    // Payment Gateway - Razorpay only
    // SECURITY: Payment key is now loaded from BuildConfig
    val RAZORPAY_KEY_ID = BuildConfig.RAZORPAY_KEY_ID

    // Cloud Functions URLs
    const val CLOUD_FUNCTIONS_BASE_URL = "https://us-central1-daxido-native.cloudfunctions.net"
    const val ALLOCATE_RIDE_FUNCTION = "$CLOUD_FUNCTIONS_BASE_URL/allocateDriver"
    const val CALCULATE_ETA_FUNCTION = "$CLOUD_FUNCTIONS_BASE_URL/calculatePreciseETA"
    const val NOTIFY_DRIVERS_FUNCTION = "$CLOUD_FUNCTIONS_BASE_URL/notifyDrivers"
    const val HANDLE_DRIVER_RESPONSE_FUNCTION = "$CLOUD_FUNCTIONS_BASE_URL/handleDriverResponse"
    const val UPDATE_RIDE_STATUS_FUNCTION = "$CLOUD_FUNCTIONS_BASE_URL/updateRideStatus"
    const val PROCESS_PAYMENT_FUNCTION = "$CLOUD_FUNCTIONS_BASE_URL/processPayment"
    const val EMERGENCY_ALERT_FUNCTION = "$CLOUD_FUNCTIONS_BASE_URL/emergencyAlert"

    // Emergency Services
    const val EMERGENCY_HOTLINE = "911" // Or local emergency number
    const val SUPPORT_HOTLINE = "+1-800-DAXIDO-1"

    // App Configuration
    const val MAX_SEARCH_RADIUS_KM = 10
    const val INITIAL_SEARCH_RADIUS_KM = 2
    const val LOCATION_UPDATE_INTERVAL_MS = 5000L
    const val HIGH_ACCURACY_UPDATE_INTERVAL_MS = 1000L
    const val DRIVER_RESPONSE_TIMEOUT_MS = 15000L

    // Feature Flags
    const val ENABLE_ML_DRIVER_MATCHING = true
    const val ENABLE_ROAD_SNAPPING = true
    const val ENABLE_SURGE_PRICING = true
    const val ENABLE_LIVE_TRACKING = true
    const val ENABLE_VOICE_NAVIGATION = true

    // Security
    const val MIN_OTP_LENGTH = 6  // SECURITY FIX: Increased from 4 to 6 digits for better security
    const val MAX_OTP_ATTEMPTS = 3
    const val SESSION_TIMEOUT_MINUTES = 30

    // Instructions for setup:
    // 1. Create a local.properties file in your project root (it's gitignored by default)
    // 2. Add the following lines to local.properties:
    //    GOOGLE_MAPS_API_KEY=your_actual_google_maps_key
    //    RAZORPAY_KEY_ID=your_actual_razorpay_key
    //    KEYSTORE_FILE=path_to_your_keystore
    //    KEYSTORE_PASSWORD=your_keystore_password
    //    KEY_ALIAS=your_key_alias
    //    KEY_PASSWORD=your_key_password
    // 3. Get Google Maps API Key from: https://console.cloud.google.com/apis
    // 4. Get Razorpay keys from: https://dashboard.razorpay.com/app/keys
    // 5. NEVER commit local.properties or actual keys to version control
}