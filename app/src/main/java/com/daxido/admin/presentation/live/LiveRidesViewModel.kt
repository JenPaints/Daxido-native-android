package com.daxido.admin.presentation.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daxido.admin.data.AdminRepository
import com.daxido.admin.models.LiveRideMonitoring
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LiveRidesViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveRidesUiState())
    val uiState: StateFlow<LiveRidesUiState> = _uiState.asStateFlow()

    fun loadLiveRides() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            adminRepository.getLiveRides()
                .collect { rides ->
                    _uiState.update {
                        it.copy(
                            liveRides = rides,
                            isLoading = false
                        )
                    }
                }
        }
    }
}

data class LiveRidesUiState(
    val liveRides: List<LiveRideMonitoring> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
