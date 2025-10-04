package com.daxido.user.presentation.corporate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daxido.core.corporate.CorporateAccountsService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CorporateAccountViewModel @Inject constructor(
    private val corporateAccountsService: CorporateAccountsService
) : ViewModel() {

    private val _uiState = MutableStateFlow(CorporateAccountUiState())
    val uiState: StateFlow<CorporateAccountUiState> = _uiState.asStateFlow()

    init {
        loadAccountDetails()
    }

    private fun loadAccountDetails() {
        viewModelScope.launch {
            val accountId = "account123" // Mock account ID
            val account = corporateAccountsService.getCorporateAccount(accountId)

            account?.let {
                _uiState.value = CorporateAccountUiState(
                    companyName = it.companyName,
                    creditLimit = it.creditLimit,
                    currentBalance = it.currentBalance,
                    monthlySpent = 5000.0, // Mock value
                    totalRides = 150, // Mock value
                    totalEmployees = 45 // Mock value
                )
            }
        }
    }

    fun generateBillingReport() {
        viewModelScope.launch {
            val accountId = "account123"
            val startDate = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000) // Last 30 days
            val endDate = System.currentTimeMillis()

            val report = corporateAccountsService.generateBillingReport(
                accountId = accountId,
                startDate = startDate,
                endDate = endDate
            )

            // Handle report generation
        }
    }
}