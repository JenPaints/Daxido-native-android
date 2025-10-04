package com.daxido.core.error

import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ErrorHandler @Inject constructor() {
    
    private val tag = "DaxidoError"
    
    fun handleError(error: Throwable, context: String = "") {
        when (error) {
            is NetworkException -> {
                Log.e(tag, "Network error in $context: ${error.message}")
                // Show user-friendly network error message
            }
            is AuthenticationException -> {
                Log.e(tag, "Authentication error in $context: ${error.message}")
                // Redirect to login screen
            }
            is PaymentException -> {
                Log.e(tag, "Payment error in $context: ${error.message}")
                // Show payment error dialog
            }
            is LocationException -> {
                Log.e(tag, "Location error in $context: ${error.message}")
                // Show location permission dialog
            }
            else -> {
                Log.e(tag, "Unexpected error in $context: ${error.message}", error)
                // Log to crash reporting service
            }
        }
    }
    
    fun CoroutineScope.launchWithErrorHandling(
        context: String = "",
        block: suspend CoroutineScope.() -> Unit
    ) {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            handleError(throwable, context)
        }
        launch(exceptionHandler, block = block)
    }
}

// Custom Exception Classes
sealed class DaxidoException(message: String, cause: Throwable? = null) : Exception(message, cause)

class NetworkException(message: String, cause: Throwable? = null) : DaxidoException(message, cause)
class AuthenticationException(message: String, cause: Throwable? = null) : DaxidoException(message, cause)
class PaymentException(message: String, cause: Throwable? = null) : DaxidoException(message, cause)
class LocationException(message: String, cause: Throwable? = null) : DaxidoException(message, cause)
class ValidationException(message: String, cause: Throwable? = null) : DaxidoException(message, cause)
class BusinessLogicException(message: String, cause: Throwable? = null) : DaxidoException(message, cause)
