package com.daxido.user.presentation.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daxido.core.models.PaymentMethod
import com.daxido.core.models.PaymentType
import com.daxido.data.repository.PaymentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val paymentRepository: PaymentRepository
) : ViewModel() {

    init {
        loadWalletBalance()
    }
    
    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()
    
    fun setPaymentAmount(amount: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                totalAmount = amount
            )
        }
    }
    
    fun selectPaymentMethod(method: PaymentType) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                selectedPaymentMethod = method
            )
        }
    }
    
    fun applyPromoCode(code: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isApplyingPromo = true
            )
            // TODO: Apply promo code logic
            _uiState.value = _uiState.value.copy(
                isApplyingPromo = false,
                appliedPromoCode = code,
                discount = 50.0
            )
        }
    }
    
    fun setTip(amount: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                tip = amount
            )
        }
    }
    
    private fun loadWalletBalance() {
        viewModelScope.launch {
            paymentRepository.getWalletBalance().collect { balance ->
                _uiState.value = _uiState.value.copy(walletBalance = balance)
            }
        }
    }

    fun processPayment(rideId: String, amount: Double, paymentMethod: PaymentType) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessingPayment = true,
                paymentError = null
            )

            val result = paymentRepository.processPayment(
                amount = amount,
                paymentMethod = PaymentMethod(type = paymentMethod),
                rideId = rideId
            )

            result.fold(
                onSuccess = { transactionId ->
                    _uiState.value = _uiState.value.copy(
                        isProcessingPayment = false,
                        isPaymentSuccessful = true,
                        paymentError = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isProcessingPayment = false,
                        isPaymentSuccessful = false,
                        paymentError = error.message ?: "Payment failed"
                    )
                }
            )
        }
    }

    fun addMoneyToWallet(amount: Double, paymentMethod: PaymentType) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessingPayment = true,
                paymentError = null
            )

            val result = paymentRepository.addMoneyToWallet(
                amount = amount,
                paymentMethod = PaymentMethod(type = paymentMethod)
            )

            result.fold(
                onSuccess = { newBalance ->
                    _uiState.value = _uiState.value.copy(
                        isProcessingPayment = false,
                        isPaymentSuccessful = true,
                        walletBalance = newBalance,
                        paymentError = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isProcessingPayment = false,
                        isPaymentSuccessful = false,
                        paymentError = error.message ?: "Failed to add money to wallet"
                    )
                }
            )
        }
    }
}

data class PaymentUiState(
    val isLoading: Boolean = false,
    val totalAmount: Double = 0.0,
    val selectedPaymentMethod: PaymentType = PaymentType.CASH,
    val walletBalance: Double = 1000.0,
    val promoCode: String = "",
    val isApplyingPromo: Boolean = false,
    val appliedPromoCode: String? = null,
    val discount: Double = 0.0,
    val tip: Double = 0.0,
    val rideFare: Double = 0.0,
    val isPaymentSuccessful: Boolean = false,
    val isProcessingPayment: Boolean = false,
    val paymentError: String? = null,
    val promoError: String? = null
)
