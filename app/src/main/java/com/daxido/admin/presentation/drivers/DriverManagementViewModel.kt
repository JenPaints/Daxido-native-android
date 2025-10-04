package com.daxido.admin.presentation.drivers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daxido.admin.data.AdminRepository
import com.daxido.admin.models.DriverManagement
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DriverManagementViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DriverManagementUiState())
    val uiState: StateFlow<DriverManagementUiState> = _uiState.asStateFlow()

    fun loadDrivers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            adminRepository.getAllDrivers(limit = 100)
                .onSuccess { drivers ->
                    _uiState.update {
                        it.copy(
                            drivers = drivers,
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

    fun setFilter(filter: DriverFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun verifyDriver(driverId: String) {
        viewModelScope.launch {
            adminRepository.verifyDriver(driverId)
                .onSuccess {
                    loadDrivers()
                }
        }
    }

    fun rejectDriver(driverId: String, reason: String) {
        viewModelScope.launch {
            adminRepository.rejectDriver(driverId, reason)
                .onSuccess {
                    loadDrivers()
                }
        }
    }
}

data class DriverManagementUiState(
    val drivers: List<DriverManagement> = emptyList(),
    val filter: DriverFilter = DriverFilter.ALL,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
