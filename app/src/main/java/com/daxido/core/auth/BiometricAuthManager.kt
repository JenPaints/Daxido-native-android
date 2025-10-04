package com.daxido.core.auth

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.daxido.core.data.preferences.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FEATURE COMPLETE: Biometric Authentication Manager
 * Handles fingerprint, face unlock, and other biometric authentication
 */
@Singleton
class BiometricAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferences: UserPreferences
) {

    /**
     * Check if biometric authentication is available on this device
     */
    fun isBiometricAvailable(): BiometricAvailability {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS ->
                BiometricAvailability.Available

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                BiometricAvailability.NotAvailable("No biometric hardware available")

            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                BiometricAvailability.NotAvailable("Biometric hardware is currently unavailable")

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                BiometricAvailability.NotEnrolled("No biometric credentials enrolled. Please set up biometrics in device settings.")

            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED ->
                BiometricAvailability.NotAvailable("Security update required")

            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED ->
                BiometricAvailability.NotAvailable("Biometric authentication not supported")

            BiometricManager.BIOMETRIC_STATUS_UNKNOWN ->
                BiometricAvailability.NotAvailable("Biometric status unknown")

            else ->
                BiometricAvailability.NotAvailable("Biometric authentication unavailable")
        }
    }

    /**
     * Show biometric authentication prompt
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Biometric Authentication",
        subtitle: String = "Verify your identity",
        negativeButtonText: String = "Cancel",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onFailed: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(context)

        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    when (errorCode) {
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_CANCELED -> {
                            onError("Authentication cancelled")
                        }
                        BiometricPrompt.ERROR_LOCKOUT,
                        BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                            onError("Too many attempts. Please try again later.")
                        }
                        BiometricPrompt.ERROR_NO_BIOMETRICS -> {
                            onError("No biometric credentials enrolled")
                        }
                        else -> {
                            onError(errString.toString())
                        }
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailed()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setConfirmationRequired(true)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    /**
     * Check if biometric authentication is enabled by user
     */
    fun isBiometricEnabled(): Boolean {
        return userPreferences.isBiometricEnabled()
    }

    /**
     * Enable or disable biometric authentication
     */
    fun setBiometricEnabled(enabled: Boolean) {
        userPreferences.setBiometric(enabled)
    }

    /**
     * Quick authentication for sensitive operations
     */
    fun authenticateForPayment(
        activity: FragmentActivity,
        amount: Double,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        authenticate(
            activity = activity,
            title = "Confirm Payment",
            subtitle = "Authenticate to confirm payment of ₹$amount",
            negativeButtonText = "Cancel",
            onSuccess = onSuccess,
            onError = onError,
            onFailed = { onError("Authentication failed") }
        )
    }
}

/**
 * Biometric availability status
 */
sealed class BiometricAvailability {
    object Available : BiometricAvailability()
    data class NotAvailable(val reason: String) : BiometricAvailability()
    data class NotEnrolled(val message: String) : BiometricAvailability()
}
