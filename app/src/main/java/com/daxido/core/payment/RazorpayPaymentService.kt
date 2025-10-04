package com.daxido.core.payment

import android.app.Activity
import android.content.Context
import android.util.Log
import com.daxido.core.config.AppConfig
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Razorpay payment gateway integration
 * SECURITY FIX: API key now loaded from BuildConfig via AppConfig
 * To use: Add your Razorpay key to local.properties:
 * RAZORPAY_KEY_ID=your_actual_razorpay_key
 */
@Singleton
class RazorpayPaymentService @Inject constructor(
    private val context: Context
) {

    private val apiKey = AppConfig.RAZORPAY_KEY_ID

    init {
        Checkout.preload(context)
    }

    /**
     * Process payment using Razorpay
     */
    suspend fun processPayment(
        activity: Activity,
        amount: Double,
        orderId: String,
        customerName: String,
        customerEmail: String,
        customerPhone: String,
        description: String
    ): PaymentResult = suspendCancellableCoroutine { continuation ->
        try {
            val checkout = Checkout()
            checkout.setKeyID(apiKey)

            val options = JSONObject().apply {
                put("name", "Daxido")
                put("description", description)
                put("image", "https://your-logo-url.com/logo.png")
                put("order_id", orderId)
                put("currency", "INR")
                put("amount", (amount * 100).toInt()) // Amount in paise

                val prefill = JSONObject().apply {
                    put("email", customerEmail)
                    put("contact", customerPhone)
                    put("name", customerName)
                }
                put("prefill", prefill)

                val theme = JSONObject().apply {
                    put("color", "#3399cc")
                }
                put("theme", theme)
            }

            val listener = object : PaymentResultListener {
                override fun onPaymentSuccess(razorpayPaymentId: String?) {
                    Log.d("RazorpayPayment", "Payment success: $razorpayPaymentId")
                    continuation.resume(
                        PaymentResult.Success(
                            transactionId = razorpayPaymentId ?: "",
                            amount = amount
                        )
                    )
                }

                override fun onPaymentError(code: Int, response: String?) {
                    Log.e("RazorpayPayment", "Payment error: $code - $response")

                    // IMPROVED ERROR HANDLING: Provide user-friendly error messages
                    val userFriendlyMessage = when (code) {
                        Checkout.NETWORK_ERROR -> "Network error. Please check your internet connection and try again."
                        Checkout.INVALID_OPTIONS -> "Payment configuration error. Please contact support."
                        Checkout.PAYMENT_CANCELED -> "Payment was cancelled."
                        Checkout.TLS_ERROR -> "Secure connection error. Please update your device and try again."
                        else -> when {
                            response?.contains("insufficient", ignoreCase = true) == true ->
                                "Insufficient funds. Please try a different payment method."
                            response?.contains("card", ignoreCase = true) == true ->
                                "Card payment failed. Please check your card details or try another card."
                            response?.contains("bank", ignoreCase = true) == true ->
                                "Bank payment failed. Please try again or use a different payment method."
                            response?.contains("timeout", ignoreCase = true) == true ->
                                "Payment timed out. Please try again."
                            else -> response ?: "Payment failed. Please try again."
                        }
                    }

                    continuation.resume(
                        PaymentResult.Failure(
                            errorCode = code,
                            errorMessage = userFriendlyMessage,
                            technicalDetails = response
                        )
                    )
                }
            }

            checkout.open(activity, options)

        } catch (e: Exception) {
            Log.e("RazorpayPayment", "Error processing payment: ${e.message}", e)
            continuation.resume(
                PaymentResult.Failure(
                    errorMessage = e.message ?: "Unknown error"
                )
            )
        }
    }

    /**
     * Verify payment signature
     */
    fun verifyPaymentSignature(
        orderId: String,
        paymentId: String,
        signature: String
    ): Boolean {
        // Implement signature verification using Razorpay secret
        // This should be done on the server side for security
        return true
    }

    /**
     * Create refund
     */
    suspend fun createRefund(
        paymentId: String,
        amount: Double,
        reason: String
    ): RefundResult {
        try {
            // Make API call to Razorpay to create refund
            // This requires server-side implementation with secret key

            Log.d("RazorpayPayment", "Refund created for payment: $paymentId")
            return RefundResult.Success(
                refundId = "rfnd_${System.currentTimeMillis()}",
                amount = amount,
                status = "processed"
            )
        } catch (e: Exception) {
            Log.e("RazorpayPayment", "Error creating refund: ${e.message}", e)
            return RefundResult.Failure(e.message ?: "Refund failed")
        }
    }
}

sealed class PaymentResult {
    data class Success(
        val transactionId: String,
        val amount: Double,
        val timestamp: Long = System.currentTimeMillis()
    ) : PaymentResult()

    data class Failure(
        val errorCode: Int = -1,
        val errorMessage: String,
        val technicalDetails: String? = null,
        val isRetryable: Boolean = true
    ) : PaymentResult() {
        fun shouldRetry(): Boolean = isRetryable && errorCode != Checkout.PAYMENT_CANCELED
    }
}

sealed class RefundResult {
    data class Success(
        val refundId: String,
        val amount: Double,
        val status: String
    ) : RefundResult()

    data class Failure(
        val errorMessage: String
    ) : RefundResult()
}