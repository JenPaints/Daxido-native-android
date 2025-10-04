package com.daxido.admin.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daxido.admin.data.AdminRepository
import com.daxido.admin.models.DashboardAnalytics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminDashboardUiState())
    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            adminRepository.getDashboardAnalytics()
                .onSuccess { analytics ->
                    _uiState.update {
                        it.copy(
                            analytics = analytics,
                            isLoading = false,
                            recentActivity = generateRecentActivity(analytics)
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

    private fun generateRecentActivity(analytics: DashboardAnalytics): List<String> {
        return listOf(
            "${analytics.completedRidesToday} rides completed today",
            "${analytics.activeRides} rides currently active",
            "${analytics.onlineDrivers} drivers online now",
            "Total revenue today: ₹${analytics.revenueToday.toInt()}"
        )
    }
}

data class AdminDashboardUiState(
    val analytics: DashboardAnalytics? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val recentActivity: List<String> = emptyList()
)
