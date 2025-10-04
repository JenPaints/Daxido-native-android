package com.daxido.admin.presentation.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daxido.admin.data.AdminRepository
import com.daxido.admin.models.UserManagement
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserManagementViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserManagementUiState())
    val uiState: StateFlow<UserManagementUiState> = _uiState.asStateFlow()

    fun loadUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            adminRepository.getAllUsers(limit = 100)
                .onSuccess { users ->
                    _uiState.update {
                        it.copy(
                            users = users,
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

    fun setFilter(filter: UserFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun banUser(userId: String, reason: String) {
        viewModelScope.launch {
            adminRepository.banUser(userId, reason)
                .onSuccess {
                    loadUsers() // Reload to reflect changes
                }
        }
    }

    fun unbanUser(userId: String) {
        viewModelScope.launch {
            adminRepository.unbanUser(userId)
                .onSuccess {
                    loadUsers() // Reload to reflect changes
                }
        }
    }
}

data class UserManagementUiState(
    val users: List<UserManagement> = emptyList(),
    val filter: UserFilter = UserFilter.ALL,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
