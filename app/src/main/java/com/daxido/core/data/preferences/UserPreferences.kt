package com.daxido.core.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // SECURITY FIX: Use EncryptedSharedPreferences for storing sensitive user settings
    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "daxido_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback to regular SharedPreferences if encryption fails (e.g., on emulators)
            android.util.Log.e("UserPreferences", "Failed to create encrypted preferences, falling back to regular", e)
            context.getSharedPreferences("daxido_prefs", Context.MODE_PRIVATE)
        }
    }

    // Location & Privacy Settings
    fun isLocationSharingEnabled(): Boolean = prefs.getBoolean("location_sharing", true)
    fun setLocationSharing(enabled: Boolean) = prefs.edit().putBoolean("location_sharing", enabled).apply()

    fun isBiometricEnabled(): Boolean = prefs.getBoolean("biometric_auth", false)
    fun setBiometric(enabled: Boolean) = prefs.edit().putBoolean("biometric_auth", enabled).apply()

    fun isTwoFactorEnabled(): Boolean = prefs.getBoolean("two_factor", false)
    fun setTwoFactor(enabled: Boolean) = prefs.edit().putBoolean("two_factor", enabled).apply()

    fun isDataSharingEnabled(): Boolean = prefs.getBoolean("data_sharing", true)
    fun setDataSharing(enabled: Boolean) = prefs.edit().putBoolean("data_sharing", enabled).apply()

    // Notification Settings
    fun isRideNotificationsEnabled(): Boolean = prefs.getBoolean("ride_notifications", true)
    fun setRideNotifications(enabled: Boolean) = prefs.edit().putBoolean("ride_notifications", enabled).apply()

    fun isPromotionsEnabled(): Boolean = prefs.getBoolean("promotions", true)
    fun setPromotions(enabled: Boolean) = prefs.edit().putBoolean("promotions", enabled).apply()

    fun isPaymentNotificationsEnabled(): Boolean = prefs.getBoolean("payment_notifications", true)
    fun setPaymentNotifications(enabled: Boolean) = prefs.edit().putBoolean("payment_notifications", enabled).apply()

    // App Settings
    fun isDarkModeEnabled(): Boolean = prefs.getBoolean("dark_mode", false)
    fun setDarkMode(enabled: Boolean) = prefs.edit().putBoolean("dark_mode", enabled).apply()

    fun getSelectedLanguage(): String = prefs.getString("language", "English") ?: "English"
    fun setLanguage(language: String) = prefs.edit().putString("language", language).apply()

    fun isAutoPayEnabled(): Boolean = prefs.getBoolean("auto_pay", false)
    fun setAutoPay(enabled: Boolean) = prefs.edit().putBoolean("auto_pay", enabled).apply()

    fun isDataSaverEnabled(): Boolean = prefs.getBoolean("data_saver", false)
    fun setDataSaver(enabled: Boolean) = prefs.edit().putBoolean("data_saver", enabled).apply()

    // Clear all preferences
    fun clearAll() = prefs.edit().clear().apply()
}