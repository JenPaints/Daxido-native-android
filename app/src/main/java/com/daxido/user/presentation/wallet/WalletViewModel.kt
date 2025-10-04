package com.daxido.user.presentation.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daxido.core.models.Transaction
import com.daxido.core.models.TransactionType
import com.daxido.core.models.TransactionStatus
import com.daxido.data.repository.PaymentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val paymentRepository: PaymentRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(WalletUiState())
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()
    
    init {
        loadWalletData()
    }
    
    fun loadWalletData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // Load wallet balance
                val balance = paymentRepository.getWalletBalanceValue()

                // Load transaction history
                val transactionsResult = paymentRepository.getTransactionHistory()
                val transactions = transactionsResult.getOrNull()?.map { data ->
                    Transaction(
                        id = data["transactionId"] as? String ?: "",
                        type = TransactionType.valueOf(data["type"] as? String ?: "OTHER"),
                        amount = data["amount"] as? Double ?: 0.0,
                        description = data["description"] as? String ?: "",
                        date = (data["timestamp"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: System.currentTimeMillis(),
                        status = TransactionStatus.valueOf(data["status"] as? String ?: "COMPLETED"),
                        referenceId = data["rideId"] as? String ?: data["transactionId"] as? String ?: ""
                    )
                } ?: emptyList()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    walletBalance = balance,
                    recentTransactions = transactions,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load wallet data: ${e.message}"
                )
            }
        }
    }

    fun addMoney(amount: Double, paymentMethodId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)

            try {
                // Get payment method
                val paymentMethods = paymentRepository.getPaymentMethods()
                val selectedMethod = paymentMethods.find { it.id == paymentMethodId }
                    ?: return@launch

                val result = paymentRepository.addMoneyToWallet(amount, selectedMethod)
                result.fold(
                    onSuccess = { newBalance ->
                        _uiState.value = _uiState.value.copy(
                            isProcessing = false,
                            walletBalance = newBalance,
                            successMessage = "₹${String.format("%.2f", amount)} added successfully"
                        )
                        // Reload transactions
                        loadWalletData()
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isProcessing = false,
                            errorMessage = error.message ?: "Failed to add money"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    errorMessage = e.message
                )
            }
        }
    }

    fun withdrawMoney(amount: Double, bankAccountId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)

            try {
                val result = paymentRepository.withdrawFromWallet(amount, bankAccountId)
                result.fold(
                    onSuccess = { newBalance ->
                        _uiState.value = _uiState.value.copy(
                            isProcessing = false,
                            walletBalance = newBalance,
                            successMessage = "Withdrawal of ₹${String.format("%.2f", amount)} initiated"
                        )
                        // Reload transactions
                        loadWalletData()
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isProcessing = false,
                            errorMessage = error.message ?: "Failed to withdraw money"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    errorMessage = e.message
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
}

data class WalletUiState(
    val isLoading: Boolean = true,
    val walletBalance: Double = 0.0,
    val recentTransactions: List<Transaction> = emptyList(),
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

