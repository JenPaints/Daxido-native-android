package com.daxido.admin.presentation.financial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daxido.admin.data.AdminRepository
import com.daxido.admin.models.FinancialOverview
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FinancialViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinancialUiState())
    val uiState: StateFlow<FinancialUiState> = _uiState.asStateFlow()

    fun loadFinancialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            adminRepository.getFinancialOverview()
                .onSuccess { overview ->
                    _uiState.update {
                        it.copy(
                            overview = overview,
                            isLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message
                        )
                    }
                }
        }
    }
}

data class FinancialUiState(
    val overview: FinancialOverview? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
