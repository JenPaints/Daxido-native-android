package com.daxido.core.deeplink

import android.content.Intent
import android.net.Uri
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DEEP LINK VALIDATION AND HANDLING
 * Handles all deep links with proper validation
 *
 * Supported deep link formats:
 * - daxido://ride/{rideId}
 * - daxido://payment/{paymentId}
 * - daxido://referral/{referralCode}
 * - daxido://promo/{promoCode}
 * - https://daxido.com/ride/{rideId}
 * - https://daxido.com/payment/{paymentId}
 */
@Singleton
class DeepLinkHandler @Inject constructor() {

    /**
     * Parse and validate deep link from intent
     */
    fun handleDeepLink(intent: Intent): DeepLinkResult {
        val data: Uri = intent.data ?: return DeepLinkResult.Invalid("No data in intent")

        return try {
            when {
                // Handle app scheme (daxido://)
                data.scheme == "daxido" -> handleAppScheme(data)

                // Handle https scheme
                data.scheme == "https" -> handleHttpsScheme(data)

                else -> DeepLinkResult.Invalid("Unsupported scheme: ${data.scheme}")
            }
        } catch (e: Exception) {
            DeepLinkResult.Invalid("Error parsing deep link: ${e.message}")
        }
    }

    /**
     * Handle daxido:// scheme deep links
     */
    private fun handleAppScheme(uri: Uri): DeepLinkResult {
        val host = uri.host ?: return DeepLinkResult.Invalid("No host in URI")
        val pathSegments = uri.pathSegments

        return when (host) {
            "ride" -> {
                val rideId = pathSegments.getOrNull(0)
                if (validateRideId(rideId)) {
                    DeepLinkResult.Ride(rideId!!)
                } else {
                    DeepLinkResult.Invalid("Invalid ride ID")
                }
            }

            "payment" -> {
                val paymentId = pathSegments.getOrNull(0)
                if (validatePaymentId(paymentId)) {
                    DeepLinkResult.Payment(paymentId!!)
                } else {
                    DeepLinkResult.Invalid("Invalid payment ID")
                }
            }

            "referral" -> {
                val code = pathSegments.getOrNull(0)
                if (validateReferralCode(code)) {
                    DeepLinkResult.Referral(code!!)
                } else {
                    DeepLinkResult.Invalid("Invalid referral code")
                }
            }

            "promo" -> {
                val code = pathSegments.getOrNull(0)
                if (validatePromoCode(code)) {
                    DeepLinkResult.Promo(code!!)
                } else {
                    DeepLinkResult.Invalid("Invalid promo code")
                }
            }

            else -> DeepLinkResult.Invalid("Unknown host: $host")
        }
    }

    /**
     * Handle https:// scheme deep links
     */
    private fun handleHttpsScheme(uri: Uri): DeepLinkResult {
        val host = uri.host ?: return DeepLinkResult.Invalid("No host in URI")

        // Only accept daxido.com and www.daxido.com
        if (host != "daxido.com" && host != "www.daxido.com") {
            return DeepLinkResult.Invalid("Invalid host: $host")
        }

        val pathSegments = uri.pathSegments
        if (pathSegments.isEmpty()) {
            return DeepLinkResult.Invalid("No path in URI")
        }

        return when (pathSegments[0]) {
            "ride" -> {
                val rideId = pathSegments.getOrNull(1)
                if (validateRideId(rideId)) {
                    DeepLinkResult.Ride(rideId!!)
                } else {
                    DeepLinkResult.Invalid("Invalid ride ID")
                }
            }

            "payment" -> {
                val paymentId = pathSegments.getOrNull(1)
                if (validatePaymentId(paymentId)) {
                    DeepLinkResult.Payment(paymentId!!)
                } else {
                    DeepLinkResult.Invalid("Invalid payment ID")
                }
            }

            "referral" -> {
                val code = pathSegments.getOrNull(1)
                if (validateReferralCode(code)) {
                    DeepLinkResult.Referral(code!!)
                } else {
                    DeepLinkResult.Invalid("Invalid referral code")
                }
            }

            else -> DeepLinkResult.Invalid("Unknown path: ${pathSegments[0]}")
        }
    }

    /**
     * Validate ride ID format
     * Should be alphanumeric, 10-40 characters
     */
    private fun validateRideId(rideId: String?): Boolean {
        if (rideId.isNullOrBlank()) return false
        return rideId.matches(Regex("^[a-zA-Z0-9]{10,40}$"))
    }

    /**
     * Validate payment ID format
     */
    private fun validatePaymentId(paymentId: String?): Boolean {
        if (paymentId.isNullOrBlank()) return false
        return paymentId.matches(Regex("^[a-zA-Z0-9_-]{10,50}$"))
    }

    /**
     * Validate referral code
     * 6-12 alphanumeric characters
     */
    private fun validateReferralCode(code: String?): Boolean {
        if (code.isNullOrBlank()) return false
        return code.matches(Regex("^[A-Z0-9]{6,12}$"))
    }

    /**
     * Validate promo code
     * 4-20 alphanumeric characters
     */
    private fun validatePromoCode(code: String?): Boolean {
        if (code.isNullOrBlank()) return false
        return code.matches(Regex("^[A-Z0-9]{4,20}$"))
    }
}

/**
 * Deep link parsing result
 */
sealed class DeepLinkResult {
    data class Ride(val rideId: String) : DeepLinkResult()
    data class Payment(val paymentId: String) : DeepLinkResult()
    data class Referral(val code: String) : DeepLinkResult()
    data class Promo(val code: String) : DeepLinkResult()
    data class Invalid(val reason: String) : DeepLinkResult()

    fun isValid(): Boolean = this !is Invalid
}
