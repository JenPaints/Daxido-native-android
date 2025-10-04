package com.daxido.core.payment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import com.daxido.core.models.PaymentMethod
import com.daxido.core.models.PaymentType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UPI Payment integration (GPay, PhonePe, Paytm style)
 */
@Singleton
class UpiPaymentService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val UPI_REQUEST_CODE = 123

        // Popular UPI apps in India
        val UPI_APPS = listOf(
            UpiApp("Google Pay", "com.google.android.apps.nbu.paisa.user", "gpay"),
            UpiApp("PhonePe", "com.phonepe.app", "phonepe"),
            UpiApp("Paytm", "net.one97.paytm", "paytm"),
            UpiApp("Amazon Pay", "in.amazon.mShop.android.shopping", "amazonpay"),
            UpiApp("BHIM", "in.org.npci.upiapp", "bhim"),
            UpiApp("WhatsApp", "com.whatsapp", "whatsapp")
        )
    }

    data class UpiApp(
        val name: String,
        val packageName: String,
        val id: String
    )

    data class UpiPaymentRequest(
        val payeeVpa: String = "merchant@upi", // Merchant UPI ID
        val payeeName: String = "Daxido",
        val amount: Double,
        val transactionId: String,
        val transactionNote: String = "Daxido Ride Payment",
        val currency: String = "INR"
    )

    /**
     * Get available UPI apps on device
     */
    fun getAvailableUpiApps(): List<UpiApp> {
        val packageManager = context.packageManager
        return UPI_APPS.filter { app ->
            try {
                packageManager.getPackageInfo(app.packageName, 0)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Initiate UPI payment
     */
    fun initiateUpiPayment(
        activity: Activity,
        request: UpiPaymentRequest,
        preferredApp: String? = null
    ): Intent {
        val uri = buildUpiUri(request)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(uri)

            // If preferred app is specified, try to use it
            preferredApp?.let { appId ->
                val app = UPI_APPS.find { it.id == appId }
                app?.let {
                    setPackage(it.packageName)
                }
            }
        }

        return intent
    }

    /**
     * Build UPI payment URI
     */
    private fun buildUpiUri(request: UpiPaymentRequest): String {
        return "upi://pay?" +
                "pa=${Uri.encode(request.payeeVpa)}&" +
                "pn=${Uri.encode(request.payeeName)}&" +
                "tr=${Uri.encode(request.transactionId)}&" +
                "tn=${Uri.encode(request.transactionNote)}&" +
                "am=${request.amount}&" +
                "cu=${request.currency}"
    }

    /**
     * Parse UPI payment response
     */
    fun parseUpiResponse(data: Intent?): UpiPaymentResult {
        if (data == null) {
            return UpiPaymentResult.Failed("No response received")
        }

        val response = data.getStringExtra("response") ?: ""

        val statusKey = "Status="
        val txnIdKey = "txnId="
        val txnRefKey = "txnRef="

        val status = response.substringAfter(statusKey).substringBefore("&")
        val txnId = response.substringAfter(txnIdKey).substringBefore("&")
        val txnRef = response.substringAfter(txnRefKey).substringBefore("&")

        return when (status.uppercase()) {
            "SUCCESS" -> UpiPaymentResult.Success(txnId, txnRef)
            "FAILURE" -> UpiPaymentResult.Failed("Payment failed")
            "SUBMITTED" -> UpiPaymentResult.Pending(txnId)
            else -> UpiPaymentResult.Failed("Unknown status: $status")
        }
    }

    /**
     * Create UPI payment method list for UI
     */
    fun getUpiPaymentMethods(): List<PaymentMethod> {
        return getAvailableUpiApps().map { app ->
            PaymentMethod(
                id = app.id,
                type = PaymentType.UPI,
                displayName = app.name,
                isDefault = false
            )
        }
    }
}

/**
 * UPI Payment Result
 */
sealed class UpiPaymentResult {
    data class Success(val transactionId: String, val transactionRef: String) : UpiPaymentResult()
    data class Failed(val error: String) : UpiPaymentResult()
    data class Pending(val transactionId: String) : UpiPaymentResult()
    object Cancelled : UpiPaymentResult()
}
